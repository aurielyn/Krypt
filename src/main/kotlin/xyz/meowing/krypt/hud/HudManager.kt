package xyz.meowing.krypt.hud

import net.minecraft.client.gui.DrawContext
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.Render2D.height
import xyz.meowing.krypt.utils.Render2D.width

object HudManager {
    val elements = mutableMapOf<String, HudElement>()
    val customRenderers = mutableMapOf<String, (DrawContext) -> Unit>()
    val customSizes = mutableMapOf<String, Pair<Int, Int>>()

    fun register(id: String, text: String, configKey: String? = null) {
        elements[id] = HudElement(id, 20f, 20f, 0, 0, text = text, configKey = configKey)
    }

    fun registerCustom(
        id: String,
        width: Int,
        height: Int,
        renderer: (DrawContext) -> Unit,
        configKey: String? =  null
    ) {
        customRenderers[id] = renderer
        customSizes[id] = width to height
        elements[id] = HudElement(id, 20f, 20f, width, height, configKey = configKey)
    }

    fun getX(id: String): Float = elements[id]?.x ?: 0f
    fun getY(id: String): Float = elements[id]?.y ?: 0f
    fun getScale(id: String): Float = elements[id]?.scale ?: 1f
}
