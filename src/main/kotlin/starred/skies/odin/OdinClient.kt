package starred.skies.odin

import com.github.stivais.commodore.Commodore
import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odin.utils.getCenteredText
import com.odtheking.odin.utils.getChatBreak
import com.odtheking.odin.utils.modMessage
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import starred.skies.odin.commands.autoSellCommand
import starred.skies.odin.features.UpdateNotifier
import starred.skies.odin.features.impl.cheats.*
import starred.skies.odin.helpers.Scribble
import java.net.URI

object OdinClient : ClientModInitializer {
    private val commandsToRegister: Array<Commodore> = arrayOf(autoSellCommand)

    private val modulesToRegister: Array<Module> = arrayOf(
        CloseChest, DungeonAbilities, FuckDiorite, SecretHitboxes, BreakerHelper, KeyHighlight, LividSolver, SpiritBear,
        Highlight, AutoClicker, Gloomlock, EscrowFix, AutoGFS, QueueTerms, AutoTerms, Trajectories, AutoSell, SimonSays, InventoryWalk
    )

    private val mainFile: Scribble = Scribble("main")

    private var lastInstall: String by mainFile.string("lastInstall")
    private var send: Boolean = true

    const val MOD_VERSION: String = /*$ mod_version*/ "0.1.4-r0"
    val moduleConfig: ModuleConfig = ModuleConfig("odinClient")
    val joinListeners = mutableListOf<() -> Unit>()

    override fun onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            for (c in commandsToRegister) c.register(dispatcher)
        }

        ModuleManager.registerModules(moduleConfig, *modulesToRegister)
        EventBus.subscribe(UpdateNotifier)

        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            for (fn in joinListeners.toList()) fn.invoke()
            joinListeners.clear()

            if (!send) return@register
            if (lastInstall != MOD_VERSION) li()
        }
    }

    private fun li() {
        send = false
        lastInstall = MOD_VERSION
        val divider = getChatBreak()

        modMessage(divider, "")
        modMessage(getCenteredText("§bOdinClient [Addon]"), "")
        modMessage(divider, "")
        modMessage("Thank you for installing OdinClient §8(v$MOD_VERSION)§f.", "")
        modMessage("", "")
        modMessage("Quick start:", "")
        modMessage("  §b/odin §7- Open configuration menu", "")
        modMessage("", "")

        val message = Component.literal("Need help or want to suggest features? Click to join the Discord!")
            .withStyle(
                Style.EMPTY
                    .withClickEvent(
                        ClickEvent.OpenUrl(URI("https://discord.gg/DB5S3DjQVa"))
                    )
                    .withHoverEvent(
                        HoverEvent.ShowText(Component.literal("Click to join!").withStyle(Style.EMPTY.withColor(0xFFC4B5FD.toInt())))
                    )
                    .withUnderlined(true)
            )

        modMessage(message, "")
        modMessage(divider, "")
    }
}
