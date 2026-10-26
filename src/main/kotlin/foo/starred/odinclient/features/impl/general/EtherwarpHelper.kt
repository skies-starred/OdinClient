package foo.starred.odinclient.features.impl.general

//~ if >= 26.2 'Setting' -> 'RenderableSetting'
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.customData
import com.odtheking.odin.utils.itemId
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.abstraction.gui.MinecraftGui
import foo.starred.odinclient.events.InputEvent
import foo.starred.odinclient.events.TickStartEvent
import foo.starred.odinclient.mixin.accessors.KeyMappingAccessor
import foo.starred.odinclient.api.category.OdinClientCategory
import foo.starred.odinclient.utils.rightClick
import net.minecraft.client.KeyMapping
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.SwingAnimation

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
            if (MinecraftGui.screen != null) return@on
            if (!leftClickEW) return@on
            if (buttonInfo.button != 0) return@on

            val player = client.player ?: return@on
            if (!player.mainHandItem.etherwarp()) return@on

            val a = player.isCrouching
            if (!a && !lceShift) return@on
            if (!a && ticksLeft == 0) {
                KeyMapping.set((client.options.keyShift as KeyMappingAccessor).boundKey, true)
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

            if (MinecraftGui.screen != null) return@on
            when (ticksLeft) {
                1 -> action()
                0 -> KeyMapping.set((client.options.keyShift as KeyMappingAccessor).boundKey, false)
            }
        }
    }

    private fun action() {
        rightClick()
        val player = client.player ?: return

        //? if >= 26.3 {
        /*if (player.isSwinging) return
        player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false)
        *///? } else {
        if (player.swinging && player.swingTime >= 0) return
        player.swingingArm = InteractionHand.MAIN_HAND
        player.swingTime = -1
        player.swinging = true
        //? }
    }

    private fun ItemStack.etherwarp(): Boolean =
        customData.getBoolean("ethermerge").orElse(false) || itemId == "ETHERWARP_CONDUIT"
}
