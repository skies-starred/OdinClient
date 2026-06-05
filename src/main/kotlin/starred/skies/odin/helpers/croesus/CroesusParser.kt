package starred.skies.odin.helpers.croesus

import com.odtheking.odin.utils.lore
import com.odtheking.odin.utils.loreString
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.inventory.AbstractContainerMenu
import java.util.Optional

/**
 * Read-only parser for the Croesus NPC's GUIs.
 *
 *  - [inCroesusMenu]   — top-level run-selection screen (title "Croesus").
 *  - [inRunMenu]       — a single-run sub-screen ("Catacombs - Floor X" or
 *                        "Master Catacombs - Floor X").
 *  - [findUnclaimedRunSlots] — which slots on the run-selection screen still
 *                        have unopened chests, for the overlay highlight.
 *  - [parseChests]     — read every tier's tooltip, decode contents + cost,
 *                        look prices up via [PriceClient], return one
 *                        [ChestInfo] per tier.
 *
 * Adapted from the ChatTriggers AutoCroesus module
 * (github.com/UnclaimedBloom6/RandomStuff/tree/main/AutoCroesus). Lore-shape
 * regexes (chest tier names, "Cost"/"FREE" sentinels, the "No chests opened
 * yet!" unclaimed marker) come straight from there; the regex format is
 * dictated by Hypixel's actual tooltip text.
 *
 * Item-ID resolution covers enchanted books, essences, and anything else with
 * a registered display name in the Hypixel items registry.
 */
object CroesusParser {

    /** A run still has unopened chests if any tooltip line contains this
     *  substring (plain text, formatting stripped). */
    const val LORE_UNCLAIMED_MARKER: String = "No chests opened yet"

    private val CHEST_TITLE_REGEX = Regex("^(Wood|Gold|Diamond|Emerald|Obsidian|Bedrock)(?: Chest)?$")
    private val COST_REGEX = Regex("^([\\d,]+) Coins$")
    private const val COST_FREE = "FREE"

    private val BOOK_REGEX_FORMATTED = Regex(
        "^(?:§.)*Enchanted Book \\((§d§l)?([\\w ]+) (\\w+)(?:§.)*\\)\$"
    )
    private val ESSENCE_REGEX = Regex("^(\\w+) Essence x(\\d+)$")
    private val NUMERAL_VALUES = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)

    private val RUN_TITLE_REGEX = Regex("^(?:Master )?Catacombs - .+$")
    private val BUY_CONFIRM_TITLE_REGEX = Regex("^(Wood|Gold|Diamond|Emerald|Obsidian|Bedrock)$")

    /** "Open Reward Chest" in the buy-confirm screen. */
    const val BUY_CONFIRM_SLOT: Int = 31
    /** "Go Back" in the buy-confirm screen. */
    const val BUY_BACK_SLOT: Int = 49
    /** "Go Back" in the run sub-screen. */
    const val RUN_BACK_SLOT: Int = 30

    // -- GUI detection ----------------------------------------------------------

    fun inCroesusMenu(screen: Screen?): Boolean =
        screen is AbstractContainerScreen<*> && screen.title.string.trim() == "Croesus"

    fun inRunMenu(screen: Screen?): Boolean =
        screen is AbstractContainerScreen<*> && RUN_TITLE_REGEX.matches(screen.title.string.trim())

    fun inBuyConfirmMenu(screen: Screen?): Boolean =
        screen is AbstractContainerScreen<*> &&
            BUY_CONFIRM_TITLE_REGEX.matches(screen.title.string.trim())

    /** Parse the chest currently displayed in the buy-confirm screen.
     *  Returns null when [title] isn't a recognised tier. */
    fun parseBuyConfirmChest(menu: AbstractContainerMenu, title: String): ChestParseResult? {
        val tierName = title.trim()
        val colourCode = TIER_COLOUR_CODE[tierName] ?: return null
        val lorePlain = lorePlain(menu, BUY_CONFIRM_SLOT)
            ?: return ChestParseResult.Failure(tierName, "buy-confirm slot $BUY_CONFIRM_SLOT empty")
        val loreFormatted = loreFormatted(menu, BUY_CONFIRM_SLOT)
            ?: return ChestParseResult.Failure(tierName, "buy-confirm slot $BUY_CONFIRM_SLOT empty")
        return parseChestLore(BUY_CONFIRM_SLOT, tierName, colourCode, lorePlain, loreFormatted)
    }

    // -- Run-selection screen ---------------------------------------------------

    fun findUnclaimedRunSlots(menu: AbstractContainerMenu): List<Int> {
        val out = mutableListOf<Int>()
        val end = (menu.slots.size - 36).coerceAtMost(54)
        for (i in 0 until end) {
            val lore = lorePlain(menu, i) ?: continue
            if (lore.any { LORE_UNCLAIMED_MARKER in it }) out += i
        }
        return out
    }

    private val TIER_COLOUR_CODE = mapOf(
        "Wood" to "§7", "Gold" to "§6", "Diamond" to "§b",
        "Emerald" to "§a", "Obsidian" to "§5", "Bedrock" to "§c",
    )

    // -- Run sub-screen ---------------------------------------------------------

    fun parseChests(menu: AbstractContainerMenu): List<ChestParseResult> {
        val results = mutableListOf<ChestParseResult>()
        val end = (menu.slots.size - 36).coerceAtMost(27)
        for (i in 0 until end) {
            val slot = menu.slots.getOrNull(i) ?: continue
            val stack = slot.item ?: continue
            if (stack.isEmpty) continue

            val plainName = stack.hoverName.string.trim()
            val titleMatch = CHEST_TITLE_REGEX.matchEntire(plainName) ?: continue
            val tierName = titleMatch.groupValues[1]
            val colourCode = TIER_COLOUR_CODE[tierName] ?: "§7"

            val lorePlain = lorePlain(menu, i) ?: run {
                results += ChestParseResult.Failure(tierName, "no lore"); continue
            }
            val loreFormatted = loreFormatted(menu, i) ?: run {
                results += ChestParseResult.Failure(tierName, "no lore"); continue
            }
            results += parseChestLore(i, tierName, colourCode, lorePlain, loreFormatted)
        }
        return results
    }

    private fun parseChestLore(
        slot: Int,
        tierName: String,
        colourCode: String,
        lorePlain: List<String>,
        loreFormatted: List<String>,
    ): ChestParseResult {
        if (lorePlain.any {
                val s = it.lowercase()
                "already bought" in s || "already opened" in s
            }) {
            return ChestParseResult.Failure(tierName, "already bought")
        }

        val costIdx = lorePlain.indexOfFirst { it.trim() == "Cost" }
        if (costIdx < 0 || costIdx + 1 >= lorePlain.size) {
            return ChestParseResult.Failure(tierName, "no Cost marker in lore")
        }
        val costLine = lorePlain[costIdx + 1].trim()
        val cost = parseCost(costLine)
            ?: return ChestParseResult.Failure(tierName, "unparseable cost: \"$costLine\"")

        val blankIdx = costIdx - 1
        if (blankIdx < 1 || lorePlain[blankIdx].isNotBlank()) {
            return ChestParseResult.Failure(tierName, "no blank separator before Cost (idx=$blankIdx)")
        }
        val lastItem = blankIdx - 1
        val firstItem = 1
        if (firstItem > lastItem) {
            return ChestParseResult.Success(ChestInfo(slot, tierName, colourCode, cost, emptyList(), 0.0))
        }

        val items = mutableListOf<RewardItem>()
        var totalValue = 0.0
        for (i in firstItem..lastItem) {
            val plain = lorePlain.getOrNull(i)?.trim() ?: continue
            if (plain.isEmpty()) continue
            val formatted = loreFormatted.getOrNull(i) ?: plain
            val (id, qty) = tryParseLine(plain, formatted)
            val price = priceFor(id)
            items += RewardItem(id, qty, price, formatted)
            totalValue += price * qty
        }
        val sorted = items.sortedByDescending { it.unitValue * it.qty }
        return ChestParseResult.Success(
            ChestInfo(slot, tierName, colourCode, cost, sorted, totalValue)
        )
    }

    private fun priceFor(id: String): Double {
        if (id.startsWith("ENCHANTMENT_")) {
            val rest = id.removePrefix("ENCHANTMENT_").removePrefix("ULTIMATE_")
            val lastUnderscore = rest.lastIndexOf('_')
            if (lastUnderscore > 0) {
                val name = rest.substring(0, lastUnderscore)
                val lvl = rest.substring(lastUnderscore + 1).toIntOrNull() ?: 1
                PriceClient.getEnchantBookPrice(name, lvl)?.let { return it }
            }
        }
        PriceClient.getBazaarSell(id)?.let { return it }
        PriceClient.getLowestBin(id)?.let { return it }
        PriceClient.ensureLowestBin(id)
        return 0.0
    }

    private fun parseCost(line: String): Double? {
        if (line.equals(COST_FREE, ignoreCase = true)) return 0.0
        val m = COST_REGEX.matchEntire(line) ?: return null
        return m.groupValues[1].replace(",", "").toDoubleOrNull()
    }

    // -- Single-line parsing (book / essence / item) ----------------------------

    private fun tryParseLine(plain: String, formatted: String): Pair<String, Int> {
        tryParseBook(formatted)?.let { return it }
        tryParseEssence(plain)?.let { return it }

        val qtyMatch = Regex("^(.+?) x(\\d+)$").matchEntire(plain)
        val (namePart, qty) = if (qtyMatch != null) {
            qtyMatch.groupValues[1] to qtyMatch.groupValues[2].toInt()
        } else plain to 1

        val id = PriceClient.resolveItemId(namePart)
            ?: PriceClient.resolveShardId(namePart)
            ?: namePart.uppercase().replace(' ', '_').replace("'", "")
        return id to qty
    }

    private fun tryParseBook(formatted: String): Pair<String, Int>? {
        val m = BOOK_REGEX_FORMATTED.matchEntire(formatted) ?: return null
        val ultPrefix = m.groupValues[1]
        val rawName = m.groupValues[2]
        val tierStr = m.groupValues[3]

        val tier = tierStr.toIntOrNull() ?: decodeRoman(tierStr) ?: return null
        val ultimate = ultPrefix.isNotEmpty()
        val nameUpper = rawName.uppercase().replace(' ', '_')
        val id = ("ENCHANTMENT_" + (if (ultimate && !nameUpper.startsWith("ULTIMATE_")) "ULTIMATE_" else "") + nameUpper + "_$tier")
            .replace("ULTIMATE_ULTIMATE_", "ULTIMATE_")
        return id to 1
    }

    private fun tryParseEssence(plain: String): Pair<String, Int>? {
        val m = ESSENCE_REGEX.matchEntire(plain.trim()) ?: return null
        val type = m.groupValues[1].uppercase()
        val qty = m.groupValues[2].toIntOrNull() ?: 1
        return "ESSENCE_$type" to qty
    }

    private fun decodeRoman(numeral: String): Int? {
        if (numeral.isEmpty() || numeral.any { it !in NUMERAL_VALUES }) return null
        var sum = 0
        var i = 0
        while (i < numeral.length) {
            val curr = NUMERAL_VALUES[numeral[i]]!!
            val next = if (i + 1 < numeral.length) NUMERAL_VALUES[numeral[i + 1]] ?: 0 else 0
            if (curr < next) { sum += next - curr; i += 2 } else { sum += curr; i++ }
        }
        return sum
    }

    // -- Helpers ----------------------------------------------------------------

    /** Lore lines with § codes preserved (needed for ultimate-book detection). */
    fun loreFormatted(menu: AbstractContainerMenu, slotIndex: Int): List<String>? {
        val stack = menu.slots.getOrNull(slotIndex)?.item ?: return null
        if (stack.isEmpty) return null
        val lines = stack.lore.takeIf { it.isNotEmpty() } ?: return null
        return lines.map { it.formattedString }
    }

    /** Lore lines with all formatting stripped. */
    fun lorePlain(menu: AbstractContainerMenu, slotIndex: Int): List<String>? {
        val stack = menu.slots.getOrNull(slotIndex)?.item ?: return null
        if (stack.isEmpty) return null
        return stack.loreString.takeIf { it.isNotEmpty() }
    }

    /** Walks a Component tree and rebuilds the legacy-encoded string with §
     *  colour / format codes. Needed because Hypixel ships lore as proper
     *  Components but parseBook compares against `§d§l` prefixes. */
    private val Component.formattedString: String get() = buildString {
        val rgbMap = ChatFormatting.entries
            .mapNotNull { it.color?.let { color -> color to it } }
            .toMap()
        visit<Unit>({ style, content ->
            style.color?.value?.let { rgbMap[it]?.let(::append) }
            if (style.isBold) append(ChatFormatting.BOLD)
            if (style.isItalic) append(ChatFormatting.ITALIC)
            if (style.isUnderlined) append(ChatFormatting.UNDERLINE)
            if (style.isStrikethrough) append(ChatFormatting.STRIKETHROUGH)
            if (style.isObfuscated) append(ChatFormatting.OBFUSCATED)
            append(content)
            Optional.empty()
        }, Style.EMPTY)
    }
}
