package xyz.meowing.krypt.utils

import net.minecraft.world.phys.Vec3
import xyz.meowing.knit.api.KnitClient.client
import java.awt.Color
import kotlin.math.sqrt

object Utils {
    inline val partialTicks get() = client.deltaTracker.getGameTimeDeltaPartialTick(true)
    private val formatRegex = "[§&][0-9a-fk-or]".toRegex()

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

    fun Float.toTimerFormat(decimals: Int = 2): String {
        val hours = (this / 3600).toInt()
        val minutes = ((this % 3600) / 60).toInt()
        val seconds = this % 60f

        val secondsFormatted = String.format("%.${decimals}f", seconds)

        return when {
            hours > 0 -> "${hours}h ${minutes}m ${secondsFormatted}s"
            minutes > 0 -> "${minutes}m ${secondsFormatted}s"
            else -> "${secondsFormatted}s"
        }
    }

    fun String?.removeFormatting(): String {
        if (this == null) return ""
        return this.replace(formatRegex, "")
    }
}