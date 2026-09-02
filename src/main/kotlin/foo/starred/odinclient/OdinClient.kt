package foo.starred.odinclient

import com.github.stivais.commodore.Commodore
import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odin.utils.getCenteredText
import com.odtheking.odin.utils.getChatBreak
import com.odtheking.odin.utils.modMessage
import foo.starred.odinclient.commands.autoClickerCommand
import foo.starred.odinclient.commands.autoSellCommand
import foo.starred.odinclient.commands.highlightCommand
import foo.starred.odinclient.commands.streamCommand
import foo.starred.odinclient.features.*
import foo.starred.odinclient.features.impl.dungeons.*
import foo.starred.odinclient.features.impl.floor7.*
import foo.starred.odinclient.features.impl.general.*
import foo.starred.odinclient.features.impl.render.*
import foo.starred.odinclient.api.storage.JsonStore
import foo.starred.snowbird.api.text.parser.impl.parse
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents

object OdinClient : ClientModInitializer {
    private val commandsToRegister: Array<Commodore> = arrayOf(
        autoSellCommand, streamCommand, highlightCommand, autoClickerCommand
    )

    private val modulesToRegister: Array<Module> = arrayOf(
        CloseChest, AutoAbilities, FuckDiorite, SecretHitboxes, BreakerHelper, LividSolver, SpiritBear, TriggerBot,
        Highlight, AutoClicker, EscrowFix, AutoGFS, QueueTerms, AutoTerms, Trajectories, AutoSell, SimonSays,
        InventoryWalk, FarmKeys, AutoExperiments, EtherwarpHelper, GhostBlock, WorldScanner, AutoDojo, CheaterWardrobe,
        CameraHelper, ModSettings, AutoSuperboom, Ghosts, NoGlow, AutoHarp, DoorHighlight, CheaterMap
    )

    private val mainFile: JsonStore = JsonStore("main")

    private var lastInstall: String by mainFile.string("lastInstall")
    private var send: Boolean = true

    const val MOD_VERSION: String = /*$ mod_version*/ "0.3.2-r1"
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
        modMessage("<hover:<green>Click to open!><click:url:https://patreon.com/starredskies>Want to help support the development for mods like OdinClient? Click here :3".parse(), "")
        modMessage(divider, "")
    }
}
