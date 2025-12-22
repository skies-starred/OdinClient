package starred.skies.odin

import com.github.stivais.commodore.Commodore
import com.odtheking.odin.clickgui.settings.impl.HUDSetting
import com.odtheking.odin.clickgui.settings.impl.KeybindSetting
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.features.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.ModuleManager
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import starred.skies.odin.features.impl.cheats.*

// dont question the weird category shit, it works and thats all i want
object OdinClient : ClientModInitializer {
    private val categoryCache = mutableMapOf<String, Category>()

    private val modulesToRegister = arrayOf(
        CloseChest, DungeonAbilities, FuckDiorite, SecretHitboxes, BreakerHelper, KeyHighlight, LividSolver, SpiritBear,
        Highlight, AutoClicker, Gloomlock, EscrowFix, AutoGFS
    )

    init {
        modulesToRegister.forEach { module ->
            val packageName = module.javaClass.packageName
            val lastPackage = packageName.substringAfterLast(".")

            if (!listOf("dungeon", "floor7", "render", "skyblock", "nether").contains(lastPackage)) {
                categoryCache.getOrPut(lastPackage) {
                    (Category.RENDER as CategoryAccessor).registerCategory( // no intellij, the cast will never not succeed
                        lastPackage.uppercase(),
                        lastPackage.replaceFirstChar { it.uppercase() }
                    )
                }
            }
        }
    }

    override fun onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            arrayOf<Commodore>().forEach { it.register(dispatcher) }
        }

        EventBus.subscribe(this)

        addModules()
    }

    private fun addModules() {
        modulesToRegister.forEach { module ->
            val packageName = module.javaClass.packageName
            val lastPackage = packageName.substringAfterLast(".")

            categoryCache[lastPackage]?.let { category ->
                Module::class.java.getDeclaredField("category").apply {
                    isAccessible = true
                    set(module, category)
                }
            }

            ModuleManager.modules.add(module)

            module.key?.let {
                module.register(KeybindSetting("Keybind", it, "Toggles the module").apply {
                    onPress = { module.onKeybind() }
                })
            }

            module.settings.forEach { setting ->
                when (setting) {
                    is KeybindSetting -> ModuleManager.keybindSettingsCache.add(setting)
                    is HUDSetting -> ModuleManager.hudSettingsCache.add(setting)
                }
            }
        }
    }
}