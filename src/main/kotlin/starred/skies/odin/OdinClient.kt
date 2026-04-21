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
import starred.skies.odin.commands.autoClickerCommand
import starred.skies.odin.commands.autoSellCommand
import starred.skies.odin.commands.highlightCommand
import starred.skies.odin.commands.streamCommand
import starred.skies.odin.features.ImportantFeature
import starred.skies.odin.features.ModSettings
import starred.skies.odin.features.UpdateNotifier
import starred.skies.odin.features.impl.cheats.*
import starred.skies.odin.features.impl.render.*
import starred.skies.odin.helpers.Scribble
import xyz.aerii.library.handlers.parser.parse

object OdinClient : ClientModInitializer {
    private val commandsToRegister: Array<Commodore> = arrayOf(
        autoSellCommand, streamCommand, highlightCommand, autoClickerCommand
    )

    private val modulesToRegister: Array<Module> = arrayOf(
        CloseChest, DungeonAbilities, FuckDiorite, SecretHitboxes, BreakerHelper, KeyHighlight, LividSolver, SpiritBear, TriggerBot,
        Highlight, AutoClicker, Gloomlock, EscrowFix, AutoGFS, QueueTerms, AutoTerms, Trajectories, AutoSell, SimonSays, InventoryWalk,
        FarmKeys, AutoExperiments, EtherwarpHelper, GhostBlock, DoorESP, CancelInteract, WorldScanner, AutoDojo, CheaterWardrobe,
        CameraHelper, ModSettings, AutoSuperboom, Ghosts, NoGlow
    )

    private val mainFile: Scribble = Scribble("main")

    private var lastInstall: String by mainFile.string("lastInstall")
    private var send: Boolean = true

    const val MOD_VERSION: String = /*$ mod_version*/ "0.1.9-r2"
    val moduleConfig: ModuleConfig = ModuleConfig("odinClient")
    val joinListeners = mutableListOf<() -> Unit>()

    override fun onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            for (c in commandsToRegister) c.register(dispatcher)
        }

        ModuleManager.registerModules(moduleConfig, *modulesToRegister)
        EventBus.subscribe(UpdateNotifier)
        EventBus.subscribe(ImportantFeature)

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
        modMessage("<hover:<${0xFFC4B5FD.toInt()}>Click to join!><click:url:https://discord.gg/DB5S3DjQVa>Need help or want to suggest features? Click to join the Discord!".parse())
        modMessage(divider, "")
        modMessage("<hover:<green>Click to open!><click:url:https://aerii.xyz/donate>Want to help support the development for mods like OdinClient? Click here :3".parse(), "")
        modMessage(divider, "")
    }
}
