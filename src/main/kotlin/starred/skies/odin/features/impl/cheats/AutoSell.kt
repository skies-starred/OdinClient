package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.impl.ActionSetting
import com.odtheking.odin.clickgui.settings.impl.ListSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.*
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ClickType
import starred.skies.odin.OdinClient
import starred.skies.odin.utils.Skit
import starred.skies.odin.utils.guiClick

object AutoSell : Module(
    name = "Auto Sell",
    description = "Automatically sell items in trades and cookie menus. (/autosell)",
    category = Skit.CHEATS
) {
    val sellList by ListSetting("Sell list", mutableSetOf<String>())
    private val delay by NumberSetting("Delay", 6, 2, 10, 1, desc = "The delay between each sell action.", unit = " ticks")
    private val randomization by NumberSetting("Randomization", 1, 0, 5, 1, desc = "Random delay variance", unit = " ticks")
    private val clickType1 by SelectorSetting("Click Type", "Shift", arrayListOf("Shift", "Middle", "Left"), desc = "The type of click to use when selling items.")
    private val addDefaults by ActionSetting("Add defaults", desc = "Add default dungeon items to the auto sell list.") {
        sellList.addAll(defaultItems)
        modMessage("§aAdded default items to auto sell list")
        OdinClient.moduleConfig.save()
    }

    private var last = 0L
    private var next = 0L
    private var inGui = false

    init {
        //~ if >=1.21.11 'GuiEvent' -> 'ScreenEvent'
        on<GuiEvent.Open> {
            inGui = screen.title?.string in listOf("Trades", "Booster Cookie", "Farm Merchant", "Ophelia")
        }

        //~ if >=1.21.11 'GuiEvent' -> 'ScreenEvent'
        on<GuiEvent.Close> {
            inGui = false
        }

        on<TickEvent.Start> {
            if (sellList.isEmpty()) return@on
            if (!inGui) return@on

            val menu = (mc.screen as? AbstractContainerScreen<*>)?.menu ?: return@on
            val now = System.currentTimeMillis()
            if (now - last < next) return@on

            for (s in menu.slots) {
                if (s.container !is Inventory) continue

                val stack = s.item.takeIf { !it.isEmpty } ?: continue
                val name = stack.hoverName?.string?.noControlCodes ?: continue

                if (!sellList.any { name.contains(it, true) }) continue
                if (blacklist.any { name.contains(it, true) }) continue

                guiClick(menu.containerId, s.index, clickType = clickType1.get())
                last = now
                delay()

                break
            }
        }
    }

    private fun delay() {
        next = ((delay + (0..randomization).random()) * 50).toLong()
    }

    private fun Int.get() = when (this) {
        0 -> ClickType.QUICK_MOVE
        1 -> ClickType.CLONE
        2 -> ClickType.PICKUP
        else -> ClickType.QUICK_MOVE
    }

    private val defaultItems = arrayOf(
        "enchanted ice", "superboom tnt", "rotten", "skeleton master", "skeleton grunt", "cutlass",
        "skeleton lord", "skeleton soldier", "zombie soldier", "zombie knight", "zombie commander", "zombie lord",
        "skeletor", "super heavy", "heavy", "sniper helmet", "dreadlord", "earth shard", "zombie commander whip",
        "machine gun", "sniper bow", "soulstealer bow", "silent death", "training weight",
        "beating heart", "premium flesh", "mimic fragment", "enchanted rotten flesh", "sign",
        "enchanted bone", "defuse kit", "optical lens", "tripwire hook", "button", "carpet", "lever", "diamond atom",
        "healing viii splash potion", "healing 8 splash potion", "candycomb"
    )

    private val blacklist = listOf("skeleton master chestplate")

}