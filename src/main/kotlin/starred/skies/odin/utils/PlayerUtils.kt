package starred.skies.odin.utils

import com.odtheking.odin.OdinMod.mc
import net.minecraft.client.KeyMapping

fun rightClick() {
    val key = mc.options.keyUse
    KeyMapping.set(key.defaultKey, true)
    KeyMapping.click(key.defaultKey)
    KeyMapping.set(key.defaultKey, false)
}

fun leftClick() {
    val key = mc.options.keyAttack
    KeyMapping.set(key.defaultKey, true)
    KeyMapping.click(key.defaultKey)
    KeyMapping.set(key.defaultKey, false)
}