package starred.skies.odin.features.impl.render

import com.odtheking.odin.features.Module

object NoGlow : Module(
    name = "No Glow", //meow
    description = "Disables the vanilla Minecraft glow effect.",
) {
    @JvmStatic
    fun isEnabled() = enabled
}