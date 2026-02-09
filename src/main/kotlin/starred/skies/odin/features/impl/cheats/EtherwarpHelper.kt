package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.customData
import com.odtheking.odin.utils.itemId
import net.minecraft.client.KeyMapping
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import starred.skies.odin.events.InputEvent
import starred.skies.odin.mixin.accessors.KeyMappingAccessor
import starred.skies.odin.utils.Skit
import starred.skies.odin.utils.rightClick

object EtherwarpHelper : Module(
    name = "Etherwarp helper",
    description = "Helper features for etherwarp",
    category = Skit.CHEATS
) {
    private val leftClickEW by BooleanSetting("Left click etherwarp", true, desc = "Turns your left clicks into right clicks when holding an etherwarp item")
    private val lceShift by BooleanSetting("Shift automatically", true, desc = "Automatically shifts for you as well.").withDependency { leftClickEW }

    private val delays = intArrayOf(2, 3, 4)
    private var ticksLeft = 0

    init {
        on<InputEvent.Mouse.Press> {
            if (mc.screen != null) return@on
            if (!leftClickEW) return@on
            if (buttonInfo.button != 0) return@on

            val player = mc.player ?: return@on
            if (player.mainHandItem?.etherwarp() != true) return@on

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

        on<TickEvent.Start> {
            if (ticksLeft == 0) return@on

            ticksLeft--

            if (mc.screen != null) return@on
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