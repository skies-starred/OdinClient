package foo.starred.odinclient.features

import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.modMessage
import foo.starred.kontext.abstraction.client.extension.client
import net.minecraft.network.chat.Component

object ImportantFeature {
    private val set = setOf("516m", "evilbers")

    init {
        fn()
    }

    private fun fn() {
        schedule(72000) {
            run {
                if (!ModSettings.important) return@run
                if (client.level == null) return@run
                if ((0..100).random() > 4) return@run
                val a = if (set.size == 1) set.first() else set.random()

                modMessage(Component.literal("§c[§6ዞ§c] $a §ejoined the game."), "")
            }

            fn()
        }
    }
}
