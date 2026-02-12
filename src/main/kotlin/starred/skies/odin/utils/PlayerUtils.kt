package starred.skies.odin.utils

import com.odtheking.odin.OdinMod.mc
import net.minecraft.client.KeyMapping
import starred.skies.odin.mixin.accessors.KeyMappingAccessor

fun rightClick() {
    val options = mc.options ?: return
    val key = (options.keyUse as KeyMappingAccessor).boundKey
    KeyMapping.click(key)
}

fun leftClick() {
    val options = mc.options ?: return
    val key = (options.keyAttack as KeyMappingAccessor).boundKey
    KeyMapping.click(key)
}
