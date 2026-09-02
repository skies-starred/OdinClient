package foo.starred.odinclient.features.impl.floor7

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.ChatMessageEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import foo.starred.odinclient.utils.Category
import foo.starred.odinclient.utils.rightClick

object SimonSays : Module(
    name = "Simon Says Additions",
    description = "Additions for Simon Says!",
    category = Category.CHEATS
) {
    private val startButton = BlockPos(110, 121, 91)
    private val autoStart by BooleanSetting("Auto start", false, desc = "Automatically starts the device when it can be started.")
    private val startClicks by NumberSetting("Start Clicks", 3, 1, 10, desc = "Amount of clicks to start the device.").withDependency { autoStart }
    private val startClickDelay by NumberSetting("Start Click Delay", 3, 1, 25, unit = "ticks", desc = "Delay between each start click.").withDependency { autoStart }

    private var clicksLeft = 0
    private var delayTicks = 0
    private var active = false

    init {
        on<ChatMessageEvent> {
            if (value == "[BOSS] Goldor: Who dares trespass into my domain?") s()
        }

        on<TickEvent.Start> {
            if (!active) return@on
            if (mc.screen != null) return@on

            if (delayTicks > 0) {
                delayTicks--
                return@on
            }

            rightClick()
            clicksLeft--

            if (clicksLeft <= 0) active = false else delayTicks = startClickDelay
        }
    }

    private fun s() {
        val h = mc.hitResult?.takeIf { it.type == HitResult.Type.BLOCK } ?: return
        if ((h as? BlockHitResult)?.blockPos != startButton) return

        clicksLeft = startClicks
        delayTicks = 0
        active = true
    }
}
