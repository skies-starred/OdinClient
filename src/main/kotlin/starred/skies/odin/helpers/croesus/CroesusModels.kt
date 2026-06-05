package starred.skies.odin.helpers.croesus

/** A single line of loot inside a Croesus chest. `displayName` keeps the
 *  original formatted lore line (§ codes intact) so the overlay can render it
 *  the same colour Hypixel showed it. */
data class RewardItem(
    val skyblockId: String,
    val qty: Int,
    val unitValue: Double,
    val displayName: String,
)

/** A single chest tier (Wood / Gold / Diamond / Emerald / Obsidian / Bedrock)
 *  inside a Croesus run, parsed from its tooltip. */
data class ChestInfo(
    val slot: Int,
    val tierName: String,
    val tierColourCode: String,
    val cost: Double,
    val items: List<RewardItem>,
    val totalValue: Double,
) {
    val profit: Double get() = totalValue - cost
}

/** What the parser could not resolve — surfaced to the overlay so users can see
 *  which lore line broke the chest's profit calc instead of silently dropping
 *  the chest entirely. */
sealed class ChestParseResult {
    data class Success(val chest: ChestInfo) : ChestParseResult()
    data class Failure(val tierName: String, val reason: String) : ChestParseResult()
}
