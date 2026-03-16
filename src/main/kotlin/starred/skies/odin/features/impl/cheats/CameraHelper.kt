package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.*
import com.odtheking.odin.features.Module
import starred.skies.odin.utils.Skit

object CameraHelper : Module(
    name = "Camera helper",
    description = "Various cheat additions to the vanilla camera.",
    category = Skit.CHEATS
) {
    val cameraClip by BooleanSetting("Camera Clip", false, desc = "Allows the camera to clip through blocks.")

    val enableDist by BooleanSetting("Custom distance", false, desc = "Allows you to set a custom distance for the camera.")
    val cameraDist by NumberSetting("Distance", 4f, 3.0, 12.0, 0.1, desc = "The distance of the camera from the player.").withDependency { enableDist }
}