package starred.skies.odin.utils

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.render.drawLine
import net.minecraft.world.phys.Vec3

fun RenderEvent.Extract.drawTracer(to: Vec3, color: Color, thickness: Float = 3f, depth: Boolean = false) {
    val camera = mc.gameRenderer.mainCamera
    val fromBase = camera.position()
    val look = Vec3.directionFromRotation(camera.xRot/*? >= 1.21.11 {*/()/*? }*/, camera.yRot/*? >= 1.21.11 {*/()/*? }*/)
    // Start slightly in front of the crosshair ray so it does not get clipped by near plane.
    val from = fromBase.add(look.scale(0.35))
    drawLine(listOf(from, to), color, depth, thickness)
}