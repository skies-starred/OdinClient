package starred.skies.odin.utils

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.render.drawLine
import net.minecraft.world.phys.Vec3

//? if >= 1.21.10 {
import com.odtheking.odin.events.RenderEvent
//? } else {
/*import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
*///? }

fun /*? if >=1.21.10 {*/RenderEvent.Extract/*?} else {*//*WorldRenderContext*//*?}*/.drawTracer(to: Vec3, color: Color, thickness: Float = 3f, depth: Boolean = false) {
    val camera = mc.gameRenderer.mainCamera
    val cameraPos = camera.position
    val from = cameraPos.add(Vec3.directionFromRotation(camera.xRot, camera.yRot))

    drawLine(listOf(from, to), color, depth, thickness)
}