package starred.skies.odin.utils

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.modMessage
import net.minecraft.network.chat.Component
import starred.skies.odin.OdinClient.joinListeners

fun Component.notify() {
    if (mc.level != null) {
        modMessage(this)
    } else {
        joinListeners.add { schedule(20) { modMessage(this) } }
    }
}