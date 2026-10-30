package foo.starred.odinclient.features.impl.general

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.serialization.Codec
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.KeybindSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.getChatBreak
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.modMessage
import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.kontext.abstraction.gui.MinecraftGui
import foo.starred.kontext.abstraction.input.impl.GenericInputState
import foo.starred.odinclient.api.category.OdinClientCategory
import foo.starred.odinclient.api.storage.JsonStore
import foo.starred.odinclient.events.TickStartEvent
import foo.starred.odinclient.mixin.accessors.KeyMappingAccessor
import foo.starred.odinclient.utils.*
import net.minecraft.client.KeyMapping
import net.minecraft.world.phys.BlockHitResult

//~ if >= 26.2 'Setting' -> 'RenderableSetting'
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import foo.starred.kontext.api.storage.impl.generic.extension.mutableSet

object AutoClicker : Module(
    name = "Auto Clicker",
    description = "Auto clicker with options for left-click, right-click, or both.",
    category = OdinClientCategory.CHEATS
) {
    private val whiteListOnly by BooleanSetting("Whitelist only", desc = "Only click when holding a whitelisted item, whitelist using \"/odc ac add [left|right]\" while holding the item.")
    private val allowBreaking by BooleanSetting("Allow breaking blocks", desc = "Allows you to break blocks when auto clicking.")
    private val blockBreaker by BooleanSetting("Block dungeon breaker", true, desc = "Prevents auto clicker from working with Dungeon Breaker.")
    private val terminatorOnly by BooleanSetting("Terminator Only", true, desc = "Only click when the terminator and right click are held.")
    //~ if >= 26.2 '3.0, 15.0' -> '3.0..15.0'
    private val cps by NumberSetting("Clicks Per Second", 5.0f, 3.0, 15.0, .5, desc = "The amount of clicks per second to perform.").withDependency { terminatorOnly }

    private val enableLeftClick by BooleanSetting("Enable Left Click", true, desc = "Enable auto-clicking for left-click.").withDependency { !terminatorOnly }
    private val enableRightClick by BooleanSetting("Enable Right Click", true, desc = "Enable auto-clicking for right-click.").withDependency { !terminatorOnly }
    //~ if >= 26.2 '3.0, 15.0' -> '3.0..15.0'
    private val leftCps by NumberSetting("Left Clicks Per Second", 5.0f, 3.0, 15.0, .5, desc = "The amount of left clicks per second to perform.").withDependency { !terminatorOnly }
    //~ if >= 26.2 '3.0, 15.0' -> '3.0..15.0'
    private val rightCps by NumberSetting("Right Clicks Per Second", 5.0f, 3.0, 15.0, .5, desc = "The amount of right clicks per second to perform.").withDependency { !terminatorOnly }
    private val leftClickKeybind = KeybindSetting("Left Click", InputConstants.UNKNOWN.value, desc = "The keybind to hold for the auto clicker to click left click.").withDependency { !terminatorOnly }
    private val rightClickKeybind = KeybindSetting("Right Click", InputConstants.UNKNOWN.value, desc = "The keybind to hold for the auto clicker to click right click.").withDependency { !terminatorOnly }

    private val json = JsonStore("features/autoClicker")
    private val left = json.mutableSet("leftWhitelist", Codec.STRING)
    private val right = json.mutableSet("rightWhitelist", Codec.STRING)

    private var nlc = .0
    private var nrc = .0

    init {
        registerSetting(leftClickKeybind)
        registerSetting(rightClickKeybind)

        on<TickStartEvent> {
            if (MinecraftGui.screen != null) return@on
            if (client.player == null) return@on
            if (client.player!!.isUsingItem) return@on
            if (client.gameMode?.isDestroying ?: false) return@on
            val now = System.currentTimeMillis()

            if (terminatorOnly) {
                if (client.player?.mainHandItem?.itemId != "TERMINATOR" || !client.options.keyUse.isDown) return@on
                if (now < nrc) return@on

                nrc = now + ((1000 / cps) + ((Math.random() - .5) * 60.0))
                leftClick()
                return@on
            }

            val h1 = client.player?.mainHandItem?.itemId ?: ""
            if (blockBreaker && h1 == "DUNGEONBREAKER") return@on

            val h2 = held()
            val a = !whiteListOnly || h2 in left.value
            val b = !whiteListOnly || h2 in right.value
            if (!a && !b) return@on

            val level = client.level ?: return@on
            val hit = client.hitResult as? BlockHitResult

            val lc = a && enableLeftClick && GenericInputState.pressed(leftClickKeybind.value)
            val rc = b && enableRightClick && GenericInputState.pressed(rightClickKeybind.value)

            if (hit != null && !level.getBlockState(hit.blockPos).isAir && lc && allowBreaking) {
                KeyMapping.set((client.options.keyAttack as KeyMappingAccessor).boundKey, true)
                return@on
            }

            if (lc && now >= nlc) {
                nlc = now + ((1000 / leftCps) + ((Math.random() - .5) * 60.0))
                leftClick()
            }

            if (rc && now >= nrc) {
                nrc = now + ((1000 / rightCps) + ((Math.random() - .5) * 60.0))
                rightClick()
            }
        }

        command {
            "ac".then {
                "add" / "left" {
                    val item = held() ?: return@invoke modMessage("Hold an item to whitelist.")
                    if (item in left.value) return@invoke modMessage("\"$item\" is already in the left whitelist.")

                    left.update { add(item) }
                    modMessage("Added \"$item\" to the left whitelist.")
                }

                "add" / "right" {
                    val item = held() ?: return@invoke modMessage("Hold an item to whitelist.")
                    if (item in right.value) return@invoke modMessage("\"$item\" is already in the right whitelist.")

                    right.update { add(item) }
                    modMessage("Added \"$item\" to the right whitelist.")
                }

                "remove" / "left" {
                    val item = held() ?: return@invoke modMessage("Hold an item to remove from whitelist.")
                    if (item !in left.value) return@invoke modMessage("\"$item\" isn't in the left whitelist.")

                    left.update { remove(item) }
                    modMessage("Removed \"$item\" from the left whitelist.")
                }

                "remove" / "right" {
                    val item = held() ?: return@invoke modMessage("Hold an item to remove from whitelist.")
                    if (item !in right.value) return@invoke modMessage("\"$item\" isn't in the right whitelist.")

                    right.update { remove(item) }
                    modMessage("Removed \"$item\" from the right whitelist.")
                }

                "clear" / "left" {
                    left.update { clear() }
                    modMessage("Left whitelist cleared.")
                }

                "clear" / "right" {
                    right.update { clear() }
                    modMessage("Right whitelist cleared.")
                }

                "clear" / "all" {
                    left.update { clear() }
                    right.update { clear() }
                    modMessage("All whitelists cleared.")
                }

                "list" {
                    val left = left.value.joinToString(", ").ifEmpty { "empty" }
                    val right = right.value.joinToString(", ").ifEmpty { "empty" }

                    modMessage("Autoclicker whitelist:")
                    modMessage("Left: $left")
                    modMessage(getChatBreak().drop(20))
                    modMessage("Right: $right")
                }
            }
        }
    }

    fun held(): String? {
        val held = client.player?.mainHandItem
        return held?.nullableUUID ?: held?.nullableID ?: held?.hoverName?.string
    }
}
