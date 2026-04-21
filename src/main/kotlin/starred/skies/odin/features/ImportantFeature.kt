package starred.skies.odin.features

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.modMessage
import net.minecraft.network.chat.Component

object ImportantFeature {
    private val set = setOf("516m", "evilbers")

    init {
        fn()
    }

    private fun fn() {
        schedule(24000) {
            run {
                if (!ModSettings.important) return@run
                if (mc.level == null) return@run
                if ((0..100).random() > 4) return@run
                val a = if (set.size == 1) set.first() else set.random()

                modMessage(Component.literal("§c[§6ዞ§c] $a §ejoined the game."), "")
            }

            fn()
        }
    }
}