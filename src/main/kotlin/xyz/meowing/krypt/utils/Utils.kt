package xyz.meowing.krypt.utils

import net.minecraft.core.BlockPos
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.krypt.api.dungeons.enums.map.RoomRotations
import java.awt.Color

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

    // Uses seconds, idk if hours work but they should :pray:
    fun Float.toTimerFormat(decimals: Int = 2): String {
        val seconds = (this%60)
        val minutes = floor((this % 3600) / 60.0).toInt()
        val hours =  floor(this / 3600.0).toInt()

        val timerString = when {
            hours > 0 -> "${hours}h${minutes}m${String.format("%.${decimals}f", seconds)}s"
            minutes > 0 -> "${minutes}m${String.format("%.${decimals}f", seconds)}s"
            else -> "${String.format("%.${decimals}f", seconds)}s"
        }
        return timerString
    }
}
