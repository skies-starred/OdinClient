package foo.starred.odinclient.utils

import com.odtheking.odin.events.*
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.render.drawLine
import foo.starred.kontext.abstraction.client.extension.client
import net.minecraft.world.phys.Vec3

//~ if >= 26.2 'RenderEvent.Extract' -> 'RenderExtractEvent'
fun RenderEvent.Extract.drawTracer(to: Vec3, color: Color, thickness: Float = 3f, depth: Boolean = false) {
    //~ if >= 26.2 'mainCamera' -> 'mainCamera()'
    val camera = client.gameRenderer.mainCamera
    val cameraPos = camera.position()
    val from = cameraPos.add(Vec3.directionFromRotation(camera.xRot(), camera.yRot()))

    drawLine(listOf(from, to), color, depth, thickness)
}
