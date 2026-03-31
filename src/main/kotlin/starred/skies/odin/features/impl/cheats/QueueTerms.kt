package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.GuiEvent
import com.odtheking.odin.events.*
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.impl/*? >= 1.21.11 {*//*.boss*//*? } else {*/.floor7/*? }*/.TerminalSolver
import com.odtheking.odin.utils.devMessage
import com.odtheking.odin.utils.skyblock.dungeon.terminals.TerminalTypes
import com.odtheking.odin.utils.skyblock.dungeon.terminals.TerminalUtils
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import starred.skies.odin.events.TerminalUpdateEvent
import starred.skies.odin.utils.Skit
import starred.skies.odin.utils.notify
import java.net.URI
import java.util.*

object QueueTerms : Module(
    name = "Queue Terms",
    description = "Queues clicks in terminals to ensure every click is registered (only works in custom term gui).",
    category = Skit.CHEATS
) {
    private val dispatchDelay by NumberSetting("Dispatch Delay", 140L, 140L, 300L, unit = "ms", desc = "The delay between each click.")
    private data class Click(val slot: Int, val button: Int)
    private val queue: Queue<Click> = LinkedList()
    private var lastClickTime = 0L

    init {
        on<GuiEvent.CustomTermGuiClick> {
            with(TerminalUtils.currentTerm ?: return@on) {
                if (
                    type == TerminalTypes.MELODY ||
                    //~ if >= 1.21.11 'TerminalSolver.renderType != 1' -> '!TerminalSolver.customGuiEnabled'
                    TerminalSolver.renderType != 1 ||
                    !isClicked ||
                    !canClick(slot, button)
                ) return@on

                queue.offer(Click(slot, button))
                simulateClick(slot, button)
                devMessage("§aQueued click on slot $slot")
                cancel()
            }
        }

        //~ if >=1.21.11 'GuiEvent.DrawBackground' -> 'ScreenEvent.Render'
        on<GuiEvent.DrawBackground> {
            with(TerminalUtils.currentTerm ?: return@on) {
                if (
                    type == TerminalTypes.MELODY ||
                    //~ if >= 1.21.11 'TerminalSolver.renderType != 1' -> '!TerminalSolver.customGuiEnabled'
                    TerminalSolver.renderType != 1 ||
                    System.currentTimeMillis() - lastClickTime < dispatchDelay ||
                    queue.isEmpty() ||
                    isClicked
                ) return@on

                val click = queue.poll() ?: return@on

                click(click.slot, click.button, false)
                devMessage("§dDispatched click on slot ${click.slot}")
                lastClickTime = System.currentTimeMillis()
            }
        }

        on<TerminalUpdateEvent> {
            with (TerminalUtils.currentTerm ?: return@on) {
                //~ if >= 1.21.11 'TerminalSolver.renderType != 1' -> '!TerminalSolver.customGuiEnabled'
                if (TerminalSolver.renderType != 1 || queue.isEmpty()) return@on
                queue.forEach { simulateClick(it.slot, it.button) }
            }
        }

        on<TerminalEvent.Close> {
            queue.clear()
        }
    }

    override fun onEnable() {
        super.onEnable()
        Component.literal("Queue Terms is slightly buggy in OdinClient, please refer to https://github.com/skies-starred/Nebulune for a better one.")
            .withStyle(Style.EMPTY.withClickEvent(ClickEvent.OpenUrl(URI("https://github.com/skies-starred/Nebulune"))).withHoverEvent(HoverEvent.ShowText(Component.literal("Click to open link."))))
            .notify()
    }
}