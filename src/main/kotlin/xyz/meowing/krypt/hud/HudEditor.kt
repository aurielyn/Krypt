package xyz.meowing.krypt.hud

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import net.minecraft.client.gui.DrawContext
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.screen.KnitScreen
import xyz.meowing.krypt.api.data.StoredFile
import xyz.meowing.krypt.hud.HudManager.customRenderers
import xyz.meowing.krypt.utils.Render2D.height
import xyz.meowing.krypt.utils.Render2D.width
import java.awt.Color

class HudEditor : KnitScreen("HUD Editor") {
    private var dragging: HudElement? = null
    private var offsetX = 0f
    private var offsetY = 0f
    private var mc = KnitClient.client

    override fun onInitGui() {
        super.onInitGui()
        HudManager.loadAllLayouts()
    }

    override fun onCloseGui() {
        super.onCloseGui()
        HudManager.saveAllLayouts()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        super.render(context, mouseX, mouseY, deltaTicks)
        context.fill(0, 0, width, width, 0x90000000.toInt())

        HudManager.elements.values.forEach { element ->
            if (!element.isEnabled()) return@forEach

            context.matrices.push()
            context.matrices.translate(element.x, element.y, 0f)
            context.matrices.scale(element.scale, element.scale, 1f)

            val isHovered = element.isHovered(mouseX.toFloat(), mouseY.toFloat())

            val borderColor = when {
                isHovered -> Color(255, 255, 255).rgb
                else -> Color(100, 100, 120).rgb
            }

            val alpha = if (isHovered) 140 else 90
            val custom = customRenderers[element.id]

            if (custom != null) {
                drawHollowRect(context, 0, 0, element.width, element.height, borderColor)
                context.fill(0,0, element.width, element.height, Color(30, 35, 45, alpha).rgb)
                custom(context)
            } else {
                if (element.width == 0 && element.height == 0){
                    element.width = element.text.width() + 4
                    element.height = element.text.height() + 4
                }

                drawHollowRect(context, -2, -3, element.width, element.height, borderColor)
                context.fill(-2,-3, element.width, element.height, Color(30, 35, 45, alpha).rgb)

                context.drawTextWithShadow(KnitClient.client.textRenderer, element.text, 0, 0, 0xFFFFFF)
            }

            context.matrices.pop()
        }

        context.drawTextWithShadow(mc.textRenderer, "Drag elements. Press ESC to exit.", 10, 10, 0xFFFFFF)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val hovered = HudManager.elements.values.firstOrNull { it.isHovered(mouseX.toFloat(), mouseY.toFloat()) }
        if (hovered != null) {
            dragging = hovered
            offsetX = mouseX.toFloat() - hovered.x
            offsetY = mouseY.toFloat() - hovered.y
            return true
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        dragging?.let {
            it.x = mouseX.toFloat() - offsetX
            it.y = mouseY.toFloat() - offsetY
            return true
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        dragging = null
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val hovered = HudManager.elements.values.firstOrNull { it.isHovered(mouseX.toFloat(), mouseY.toFloat()) }
        if (hovered != null) {
            val scaleDelta = if (verticalAmount > 0) 0.1f else -0.1f
            hovered.scale = (hovered.scale + scaleDelta).coerceIn(0.2f, 5.0f)
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun shouldPause(): Boolean = false

    private fun drawHollowRect(context: DrawContext, x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        context.fill(x1, y1, x2, y1 + 1, color)
        context.fill(x1, y2 - 1, x2, y2, color)
        context.fill(x1, y1, x1 + 1, y2, color)
        context.fill(x2 - 1, y1, x2, y2, color)
    }
}