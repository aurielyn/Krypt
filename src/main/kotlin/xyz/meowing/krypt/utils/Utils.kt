package xyz.meowing.krypt.utils

import net.minecraft.world.phys.Vec3
import xyz.meowing.knit.api.KnitClient.client
import java.awt.Color
import kotlin.math.sqrt

object Utils {
    inline val partialTicks get() = client.deltaTracker.getGameTimeDeltaPartialTick(true)

    private val formatRegex = "[§&][0-9a-fk-or]".toRegex()

    fun String?.removeFormatting(): String {
        if (this == null) return ""
        return this.replace(formatRegex, "")
    }

    fun Map<*, *>.toColorFromMap(): Color? {
        return try {
            val r = (get("r") as? Number)?.toInt() ?: 255
            val g = (get("g") as? Number)?.toInt() ?: 255
            val b = (get("b") as? Number)?.toInt() ?: 255
            val a = (get("a") as? Number)?.toInt() ?: 255
            Color(r, g, b, a)
        } catch (_: Exception) {
            null
        }
    }

    fun Color.toFloatArray(): FloatArray {
        return floatArrayOf(red / 255f, green / 255f, blue / 255f)
    }

    fun distanceFrom(pos1: Vec3, pos2: Vec3): Double {
        val xDist = (pos1.x - pos2.x) * (pos1.x - pos2.x)
        val yDist = (pos1.y - pos2.y) * (pos1.y - pos2.y)
        val zDist = (pos1.z - pos2.z) * (pos1.z - pos2.z)

        return sqrt((xDist + yDist + zDist).toDouble())
    }
}