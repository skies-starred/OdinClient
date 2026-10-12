package foo.starred.odinclient.features.impl.general

//~ if >= 26.2 'Setting' -> 'RenderableSetting'
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.customData
import com.odtheking.odin.utils.itemId
import foo.starred.odinclient.events.InputEvent
import foo.starred.odinclient.events.TickStartEvent
import foo.starred.odinclient.mixin.accessors.KeyMappingAccessor
import foo.starred.odinclient.api.category.OdinClientCategory
import foo.starred.odinclient.utils.rightClick
import foo.starred.snowbird.api.client
import net.minecraft.client.KeyMapping
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack

object EtherwarpHelper : Module(
    name = "Etherwarp helper",
    description = "Helper features for etherwarp",
    category = OdinClientCategory.CHEATS
) {
    private val leftClickEW by BooleanSetting("Left click etherwarp", true, desc = "Turns your left clicks into right clicks when holding an etherwarp item")
    private val lceShift by BooleanSetting("Shift automatically", true, desc = "Automatically shifts for you as well.").withDependency { leftClickEW }

    private val delays = intArrayOf(2, 3, 4)
    private var ticksLeft = 0

    init {
        on<InputEvent.Mouse.Press> {
            if (client.screen != null) return@on
            if (!leftClickEW) return@on
            if (buttonInfo.button != 0) return@on

            val player = mc.player ?: return@on
            if (!player.mainHandItem.etherwarp()) return@on

            val a = player.isCrouching
            if (!a && !lceShift) return@on
            if (!a && ticksLeft == 0) {
                KeyMapping.set((mc.options.keyShift as KeyMappingAccessor).boundKey, true)
                ticksLeft = delays.random()
                return@on cancel()
            }

            if (a) {
                cancel()
                action()
            }
        }

        on<TickStartEvent> {
            if (ticksLeft == 0) return@on

            ticksLeft--

            if (client.screen != null) return@on
            when (ticksLeft) {
                1 -> action()
                0 -> KeyMapping.set((mc.options.keyShift as KeyMappingAccessor).boundKey, false)
            }
        }
    }

    private fun action() {
        rightClick()
        with (mc.player ?: return) {
            if (swinging && swingTime >= 0) return

            swingingArm = InteractionHand.MAIN_HAND
            swingTime = -1
            swinging = true
        }
    }

    private fun ItemStack.etherwarp(): Boolean =
        customData.getBoolean("ethermerge").orElse(false) || itemId == "ETHERWARP_CONDUIT"
}
