package foo.starred.odinclient.features

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import foo.starred.odinclient.api.category.OdinClientCategory

object ModSettings : Module(
    name = "--- OdinClient settings ---",
    description = "Settings for some of the internal stuff in OdinClient!",
    category = OdinClientCategory.CHEATS
) {
    val important by BooleanSetting("Important thing", true, desc = "Disabling the important thing may break features!")
}
