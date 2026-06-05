package starred.skies.odin.helpers.croesus

import com.google.gson.JsonParser
import com.odtheking.odin.OdinMod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * Reads three public Hypixel-adjacent price/registry endpoints and caches the
 * results in memory for ~30 minutes. Pure read-only utility used by Auto Croesus.
 *
 * Endpoints
 *  - Bazaar (instant-sell prices for bazaar items)
 *  - SkyCofl per-item BIN (lowest BIN for non-bazaar items)
 *  - Hypixel items registry (display-name -> item id)
 */
object PriceClient {
    private const val URL_BAZAAR        = "https://api.hypixel.net/skyblock/bazaar"
    private const val URL_ITEMS         = "https://api.hypixel.net/v2/resources/skyblock/items"
    /** Per-item SkyCofl BIN endpoint — primary LBIN source. Returns a JSON array
     *  of active BIN auctions; lowest BIN per unit = min(startingBid / count). */
    private const val URL_SKYCOFL_BIN_F = "https://sky.coflnet.com/api/auctions/tag/%s/active/bin"

    const val DEFAULT_REFRESH_MS: Long = 30L * 60 * 1000
    private const val LBIN_TTL_MS: Long = 10L * 60 * 1000

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val bazaarSell = ConcurrentHashMap<String, Double>()
    private val bazaarBuy  = ConcurrentHashMap<String, Double>()
    private val lowestBin  = ConcurrentHashMap<String, Double>()
    private val lowestBinFetchedAt = ConcurrentHashMap<String, Long>()
    private val lbinInFlight = ConcurrentHashMap.newKeySet<String>()
    private val nameToId   = ConcurrentHashMap<String, String>()

    @Volatile private var lastRefreshedAt = 0L
    @Volatile var lastError: String? = null; private set
    private val refreshMutex = Mutex()

    val ageMs: Long get() = if (lastRefreshedAt == 0L) Long.MAX_VALUE else System.currentTimeMillis() - lastRefreshedAt

    fun getBazaarSell(itemId: String): Double? = bazaarSell[itemId]
    fun getBazaarBuy(itemId: String): Double? = bazaarBuy[itemId]
    fun getLowestBin(itemId: String): Double? = lowestBin[itemId]

    fun resolveItemId(displayName: String): String? =
        nameToId[displayName.trim().lowercase()]

    /** Map a Galatea / Hunting-Box shard display name to its bazaar id.
     *  "Power Dragon Shard" -> "SHARD_POWER_DRAGON". The items-registry endpoint
     *  doesn't include these (it stops at the pre-Galatea era). */
    fun resolveShardId(displayName: String): String? {
        val trimmed = displayName.trim()
        val withoutSuffix = when {
            trimmed.endsWith(" Shards", ignoreCase = true) -> trimmed.dropLast(7).trim()
            trimmed.endsWith(" Shard", ignoreCase = true)  -> trimmed.dropLast(6).trim()
            else -> return null
        }
        if (withoutSuffix.isEmpty()) return null
        val candidate = "SHARD_" + withoutSuffix.uppercase().replace(' ', '_').replace("'", "")
        return if (bazaarSell.containsKey(candidate) || bazaarBuy.containsKey(candidate)) candidate else null
    }

    /** Human-readable price, the way Hypixel shows coins. */
    fun formatPrice(price: Double): String {
        val abs = kotlin.math.abs(price)
        val l = java.util.Locale.US
        return when {
            abs >= 1_000_000_000_000.0 -> "%.2fT".format(l, price / 1_000_000_000_000.0)
            abs >= 1_000_000_000.0     -> "%.2fB".format(l, price / 1_000_000_000.0)
            abs >= 1_000_000.0         -> "%.2fM".format(l, price / 1_000_000.0)
            else                       -> "%,d".format(l, price.toLong())
        }
    }

    /** Bazaar instant-sell price for the enchant book matching the given NBT
     *  enchant key (e.g. `sharpness`, `ultimate_combo`) at the given level. */
    fun getEnchantBookPrice(enchantName: String, level: Int): Double? {
        val name = enchantName.uppercase()
        bazaarSell["ENCHANTMENT_${name}_$level"]?.let { return it }
        if (!name.startsWith("ULTIMATE_")) {
            bazaarSell["ENCHANTMENT_ULTIMATE_${name}_$level"]?.let { return it }
        }
        return null
    }

    /** Fire-and-forget: ensure this item's LBIN is fresh. Cheap when cached,
     *  deduped when in flight — safe to call every frame from the overlay. */
    fun ensureLowestBin(itemId: String) {
        if (itemId.isBlank()) return
        val age = lowestBinFetchedAt[itemId] ?: 0L
        if (System.currentTimeMillis() - age < LBIN_TTL_MS) return
        if (!lbinInFlight.add(itemId)) return
        scope.launch {
            try {
                fetchSkyCoflLowestBin(itemId)?.let {
                    lowestBin[itemId] = it
                    lowestBinFetchedAt[itemId] = System.currentTimeMillis()
                }
            } catch (t: Throwable) {
                OdinMod.logger.warn("[AutoCroesus] SkyCofl LBIN fetch failed for $itemId: ${t.message}")
            } finally {
                lbinInFlight.remove(itemId)
            }
        }
    }

    /** Kick off an async refresh if the bulk cache is older than [maxAgeMs]. */
    fun refreshIfStale(maxAgeMs: Long = DEFAULT_REFRESH_MS) {
        if (ageMs < maxAgeMs) return
        scope.launch {
            refreshMutex.withLock {
                if (ageMs < maxAgeMs) return@withLock
                val errors = mutableListOf<String>()
                runCatching { fetchBazaar() }      .onFailure { errors += "bazaar: ${it.message ?: it.javaClass.simpleName}" }
                runCatching { fetchItemRegistry() }.onFailure { errors += "items: ${it.message ?: it.javaClass.simpleName}" }
                val anySucceeded = bazaarSell.isNotEmpty() || nameToId.isNotEmpty()
                if (anySucceeded) lastRefreshedAt = System.currentTimeMillis()
                lastError = if (errors.isEmpty()) null else errors.joinToString("; ")
                if (errors.isNotEmpty()) OdinMod.logger.warn("[AutoCroesus] PriceClient partial: $lastError")
            }
        }
    }

    private fun openJsonGet(url: String): HttpURLConnection {
        val conn = URI(url).toURL().openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10_000
        conn.readTimeout = 20_000
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "OdinClient-AutoCroesus/1.0")
        return conn
    }

    private fun fetchBazaar() {
        val conn = openJsonGet(URL_BAZAAR)
        conn.inputStream.use { stream ->
            val root = JsonParser.parseReader(InputStreamReader(stream)).asJsonObject
            check(root.get("success")?.asBoolean == true) { "success=false" }
            val products = root.getAsJsonObject("products") ?: return
            bazaarSell.clear()
            bazaarBuy.clear()
            for ((id, json) in products.entrySet()) {
                val qs = json.asJsonObject.getAsJsonObject("quick_status") ?: continue
                qs.get("sellPrice")?.asDouble?.let { bazaarSell[id] = it }
                qs.get("buyPrice")?.asDouble?.let { bazaarBuy[id] = it }
            }
        }
    }

    private fun fetchSkyCoflLowestBin(itemId: String): Double? {
        val url = URL_SKYCOFL_BIN_F.format(itemId)
        val conn = openJsonGet(url)
        conn.inputStream.use { stream ->
            val root = JsonParser.parseReader(InputStreamReader(stream))
            if (!root.isJsonArray) return null
            var minPerItem = Double.MAX_VALUE
            for (el in root.asJsonArray) {
                val obj = el.asJsonObject
                val count = obj.get("count")?.asInt ?: 1
                val bid = obj.get("startingBid")?.asDouble ?: continue
                if (count <= 0 || bid <= 0) continue
                val perItem = bid / count
                if (perItem < minPerItem) minPerItem = perItem
            }
            return if (minPerItem != Double.MAX_VALUE) minPerItem else null
        }
    }

    private fun fetchItemRegistry() {
        val conn = openJsonGet(URL_ITEMS)
        conn.inputStream.use { stream ->
            val root = JsonParser.parseReader(InputStreamReader(stream)).asJsonObject
            check(root.get("success")?.asBoolean == true) { "success=false" }
            val items = root.getAsJsonArray("items") ?: return
            nameToId.clear()
            for (el in items) {
                val obj = el.asJsonObject
                val id = obj.get("id")?.asString ?: continue
                val name = obj.get("name")?.asString ?: continue
                nameToId[name.trim().lowercase()] = id
            }
        }
    }
}
