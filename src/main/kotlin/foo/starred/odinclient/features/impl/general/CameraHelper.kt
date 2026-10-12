package foo.starred.odinclient.features.impl.general

//~ if >= 26.2 'Setting' -> 'RenderableSetting'
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.features.Module
import foo.starred.odinclient.api.category.OdinClientCategory

object CameraHelper : Module(
    name = "Camera helper",
    description = "Various cheat additions to the vanilla camera.",
    category = OdinClientCategory.CHEATS
) {
    val cameraClip by BooleanSetting("Camera Clip", false, desc = "Allows the camera to clip through blocks.")

    val enableDist by BooleanSetting("Custom distance", false, desc = "Allows you to set a custom distance for the camera.")
    //~ if >= 26.2 '3.0, 12.0' -> '3.0..12.0'
    val cameraDist by NumberSetting("Distance", 4f, 3.0, 12.0, 0.1, desc = "The distance of the camera from the player.").withDependency { enableDist }
}
