package foo.starred.odinclient.utils

import foo.starred.kontext.abstraction.client.extension.client
import foo.starred.odinclient.mixin.accessors.KeyMappingAccessor
import net.minecraft.client.KeyMapping

fun rightClick() {
    val options = client.options ?: return
    val key = (options.keyUse as KeyMappingAccessor).boundKey
    KeyMapping.set(key, true)
    KeyMapping.click(key)
    KeyMapping.set(key, false)
}

fun leftClick() {
    val options = client.options ?: return
    val key = (options.keyAttack as KeyMappingAccessor).boundKey
    KeyMapping.set(key, true)
    KeyMapping.click(key)
    KeyMapping.set(key, false)
}
