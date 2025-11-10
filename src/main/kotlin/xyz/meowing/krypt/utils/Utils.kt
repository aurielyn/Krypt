package xyz.meowing.krypt.utils

import xyz.meowing.knit.api.KnitClient.client
import java.awt.Color

object Utils {
    inline val partialTicks get() = client.renderTickCounter.getTickProgress(true)

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
}