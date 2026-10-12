package foo.starred.odinclient.features.impl.floor7

//~ if >= 26.2 'Setting' -> 'RenderableSetting'
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import foo.starred.odinclient.events.TickStartEvent
import foo.starred.odinclient.api.category.OdinClientCategory
import foo.starred.odinclient.utils.rightClick
import foo.starred.snowbird.api.client
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

object SimonSays : Module(
    name = "Simon Says Additions",
    description = "Additions for Simon Says!",
    category = OdinClientCategory.CHEATS
) {
    private val startButton = BlockPos(110, 121, 91)
    private val autoStart by BooleanSetting("Auto start", false, desc = "Automatically starts the device when it can be started.")
    //~ if >= 26.2 '1, 10' -> '1..10'
    private val startClicks by NumberSetting("Start Clicks", 3, 1, 10, desc = "Amount of clicks to start the device.").withDependency { autoStart }
    //~ if >= 26.2 '1, 25' -> '1..25'
    private val startClickDelay by NumberSetting("Start Click Delay", 3, 1, 25, unit = "ticks", desc = "Delay between each start click.").withDependency { autoStart }

    private var clicksLeft = 0
    private var delayTicks = 0
    private var active = false

    init {
        on<MessageEvent.Chat> {
            if (message == "[BOSS] Goldor: Who dares trespass into my domain?") s()
        }

        on<TickStartEvent> {
            if (!active) return@on
            if (client.screen != null) return@on

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
