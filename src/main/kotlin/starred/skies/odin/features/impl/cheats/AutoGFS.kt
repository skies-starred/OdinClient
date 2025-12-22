package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.impl.*
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.fillItemFromSack
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.sendCommand
import com.odtheking.odin.utils.skyblock.KuudraUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.tiles.RoomType

object AutoGFS : Module(
    name = "Auto GFS",
    description = "Automatically refills certain items from your sacks."
) {
    private val inKuudra by BooleanSetting("In Kuudra", true, desc = "Only gfs in Kuudra.")
    private val inDungeon by BooleanSetting("In Dungeon", true, desc = "Only gfs in Dungeons.")
    private val refillOnDungeonStart by BooleanSetting("Refill on Dungeon Start", true, desc = "Refill when a dungeon starts.")
    private val refillPearl by BooleanSetting("Refill Pearl", true, desc = "Refill ender pearls.")
    private val refillJerry by BooleanSetting("Refill Jerry", true, desc = "Refill inflatable jerrys.")
    private val refillTNT by BooleanSetting("Refill TNT", true, desc = "Refill superboom tnt.")
    private val autoGetDraft by BooleanSetting("Auto Get Draft", true, desc = "Automatically get draft from the sack.")

    private val puzzleFailRegex = Regex("^PUZZLE FAIL! (\\w{1,16}) .+$|^\\[STATUE\\] Oruo the Omniscient: (\\w{1,16}) chose the wrong answer! I shall never forget this moment of misrememberance\\.$")
    private val startRegex = Regex("\\[NPC] Mort: Here, I found this map when I first entered the dungeon\\.|\\[NPC] Mort: Right-click the Orb for spells, and Left-click \\(or Drop\\) to use your Ultimate!")

    init {
        on<ChatPacketEvent> {
            when {
                value.matches(puzzleFailRegex) -> {
                    if (!autoGetDraft || DungeonUtils.currentRoom?.data?.type != RoomType.PUZZLE) return@on
                    schedule(30) {
                        modMessage("§7Fetching Draft from sack...")
                        sendCommand("gfs architect's first draft 1")
                    }
                }

                value.matches(startRegex) -> {
                    if (refillOnDungeonStart) refill()
                }
            }
        }
    }

    private fun refill() {
        if (mc.screen != null || !(inKuudra && KuudraUtils.inKuudra) && !(inDungeon && DungeonUtils.inDungeons)) return
        val inventory = mc.player?.inventory ?: return

        inventory.find { it?.itemId == "ENDER_PEARL" }?.takeIf { refillPearl }?.also { fillItemFromSack(16, "ENDER_PEARL", "ender_pearl", false) }

        inventory.find { it?.itemId == "INFLATABLE_JERRY" }?.takeIf { refillJerry }?.also { fillItemFromSack(64, "INFLATABLE_JERRY", "inflatable_jerry", false) }

        inventory.find { it?.itemId == "SUPERBOOM_TNT" }.takeIf { refillTNT }?.also { fillItemFromSack(64, "SUPERBOOM_TNT", "superboom_tnt", false) }
    }
}