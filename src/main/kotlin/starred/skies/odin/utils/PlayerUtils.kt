package starred.skies.odin.utils

import com.odtheking.odin.OdinMod.mc
import net.minecraft.client.KeyMapping
import starred.skies.odin.mixin.accessors.KeyMappingAccessor

fun rightClick() {
    val key = mc.options.keyUse
    val actualKey = (key as KeyMappingAccessor).boundKey
    KeyMapping.click(actualKey)
}

fun leftClick() {
    val key = mc.options.keyAttack
    val actualKey = (key as KeyMappingAccessor).boundKey
    KeyMapping.click(actualKey)
}