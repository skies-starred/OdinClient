package foo.starred.odinclient

import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odin.utils.getCenteredText
import com.odtheking.odin.utils.getChatBreak
import com.odtheking.odin.utils.modMessage
import foo.starred.kontext.api.text.parser.impl.parse
import foo.starred.odinclient.api.storage.JsonStore
import foo.starred.odinclient.events.dispatcher.FabricEventDispatcher
import foo.starred.odinclient.features.ImportantFeature
import foo.starred.odinclient.features.ModSettings
import foo.starred.odinclient.features.impl.dungeons.*
import foo.starred.odinclient.features.impl.floor7.AutoTerms
import foo.starred.odinclient.features.impl.floor7.FuckDiorite
import foo.starred.odinclient.features.impl.floor7.QueueTerms
import foo.starred.odinclient.features.impl.floor7.SimonSays
import foo.starred.odinclient.features.impl.general.*
import foo.starred.odinclient.features.impl.render.NoGlow
import foo.starred.odinclient.utils.command
import foo.starred.updater.logic.source.impl.GitHubUpdateSource
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents

object OdinClient : ClientModInitializer {
    private val array: Array<Module> = arrayOf(
        CloseChest, AutoAbilities, FuckDiorite, SecretHitboxes, BreakerHelper, LividSolver, SpiritBear, TriggerBot,
        Highlight, AutoClicker, EscrowFix, AutoGFS, QueueTerms, AutoTerms, Trajectories, AutoSell, SimonSays,
        InventoryWalk, FarmKeys, AutoExperiments, EtherwarpHelper, GhostBlock, WorldScanner, AutoDojo, CheaterWardrobe,
        CameraHelper, ModSettings, AutoSuperboom, Ghosts, NoGlow, AutoHarp, DoorHighlight, CheaterMap
    )

    private val main: JsonStore = JsonStore("main")
    private var last: String by main.string("lastInstall")
    private var send: Boolean = true

    val version: String = /*$ mod_version*/ "0.3.6-r2"
    val config: ModuleConfig = ModuleConfig("odinClient")

    @JvmField
    var stream: Boolean = false

    override fun onInitializeClient() {
        GitHubUpdateSource("skies-starred/OdinClient", "odin-client").init(version)

        ModuleManager.registerModules(config, *array)

        EventBus.subscribe(ImportantFeature)
        EventBus.subscribe(FabricEventDispatcher)

        command {
            "stream" / "toggle" {
                stream = !stream
                modMessage("Stream mode is now: $stream.")
            }
        }

        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            if (!send) return@register
            if (last != version) li()
        }
    }

    private fun li() {
        send = false
        last = version
        val divider = getChatBreak()

        modMessage(divider, "")
        modMessage(getCenteredText("§bOdinClient [Addon]"), "")
        modMessage(divider, "")
        modMessage("Thank you for installing OdinClient §8(v$version)§f.", "")
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
