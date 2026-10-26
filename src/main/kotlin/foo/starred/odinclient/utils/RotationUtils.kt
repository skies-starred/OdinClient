package foo.starred.odinclient.utils

import foo.starred.kontext.abstraction.client.extension.client
import kotlin.math.*

object RotationUtils {
    private var startRot = Rotation(0f, 0f)
    private var endRot = Rotation(0f, 0f)
    private var startTime = 0L
    private var endTime = 0L
    var isDone = true
        private set

    data class Rotation(val pitch: Float, val yaw: Float)

    fun reset() {
        isDone = true
        startTime = 0
        endTime = 0
    }

    fun smoothLook(targetYaw: Float, targetPitch: Float, timeMs: Long) {
        val player = client.player ?: return
        startRot = Rotation(player.xRot, player.yRot)

        val yawDiff = normalizeAngle(targetYaw - startRot.yaw)
        endRot = Rotation(targetPitch, startRot.yaw + yawDiff)

        startTime = System.currentTimeMillis()
        endTime = startTime + timeMs
        isDone = false
    }

    fun smartSmoothLook(targetYaw: Float, targetPitch: Float, msPer180: Int) {
        val player = client.player ?: return

        // If we are already rotating to this target, don't restart the timer
        if (!isDone && abs(normalizeAngle(targetYaw - endRot.yaw)) < 0.1f && abs(targetPitch - endRot.pitch) < 0.1f) {
            return
        }

        val yawDiff = abs(normalizeAngle(targetYaw - player.yRot))
        val pitchDiff = abs(targetPitch - player.xRot)
        val maxDiff = max(yawDiff, pitchDiff)

        val timeMs = (maxDiff / 180f * msPer180).toLong().coerceAtLeast(10L)
        smoothLook(targetYaw, targetPitch, timeMs)
    }

    fun update() {
        if (isDone) return
        val player = client.player ?: return
        val now = System.currentTimeMillis()

        if (now >= endTime) {
            player.setYRot(endRot.yaw)
            player.setXRot(endRot.pitch)
            isDone = true
            return
        }

        val progress = (now - startTime).toDouble() / (endTime - startTime)
        val eased = easeOutCubic(progress)

        player.setYRot(interpolate(startRot.yaw, endRot.yaw, eased))
        player.setXRot(interpolate(startRot.pitch, endRot.pitch, eased))
    }

    private fun interpolate(start: Float, end: Float, eased: Float): Float {
        return start + (end - start) * eased
    }

    private fun easeOutCubic(number: Double): Float {
        return max(0f, min(1f, (1.0 - (1.0 - number).pow(3.0)).toFloat()))
    }

    private fun normalizeAngle(angle: Float): Float {
        var normalized = angle % 360
        if (normalized > 180) normalized -= 360
        if (normalized < -180) normalized += 360
        return normalized
    }

    fun getRotation(x: Double, y: Double, z: Double): Rotation {
        val player = client.player ?: return Rotation(0f, 0f)
        val dx = x - player.x
        val dy = y - (player.y + player.eyeHeight)
        val dz = z - player.z

        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        val pitch = Math.toDegrees(atan2(-dy, sqrt(dx * dx + dz * dz))).toFloat()

        return Rotation(pitch.coerceIn(-90f, 90f), yaw)
    }
}
