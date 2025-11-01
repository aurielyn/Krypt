package xyz.meowing.krypt.utils

import net.minecraft.client.gui.DrawContext
import net.minecraft.item.ItemStack
import net.minecraft.util.Colors
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.krypt.utils.StringUtils.removeFormatting

object Render2D {
    enum class TextStyle {
        DROP_SHADOW,
        DEFAULT
    }

    fun renderString(
        context: DrawContext,
        text: String,
        x: Float,
        y: Float,
        scale: Float,
        colors: Int = 0xFFFFFF,
        textStyle: TextStyle = TextStyle.DEFAULT
    ) {
        //#if MC >= 1.21.7
        //$$ context.matrices.pushMatrix()
        //$$ context.matrices.translate(x, y)
        //$$ context.matrices.scale(scale, scale)
        //#else
        context.matrices.push()
        context.matrices.translate(x, y, 0f)
        context.matrices.scale(scale, scale, 1f)
        //#endif

        when (textStyle) {
            TextStyle.DROP_SHADOW -> {
                context.drawText(client.textRenderer, text, 0, 0, colors, true)
            }

            TextStyle.DEFAULT -> {
                context.drawText(client.textRenderer, text, 0, 0, colors, false)
            }
        }

        //#if MC >= 1.21.7
        //$$ context.matrices.popMatrix()
        //#else
        context.matrices.pop()
        //#endif
    }

    fun renderStringWithShadow(context: DrawContext, text: String, x: Float, y: Float, scale: Float, colors: Int = Colors.WHITE) {
        renderString(context, text, x, y, scale, colors, TextStyle.DROP_SHADOW)
    }

    fun renderItem(context: DrawContext, item: ItemStack, x: Float, y: Float, scale: Float) {
        //#if MC >= 1.21.7
        //$$ context.matrices.pushMatrix()
        //$$ context.matrices.translate(x, y)
        //$$ context.matrices.scale(scale, scale)
        //#else
        context.matrices.push()
        context.matrices.translate(x, y, 0f)
        context.matrices.scale(scale, scale, 1f)
        //#endif

        context.drawItem(item, 0, 0)

        //#if MC >= 1.21.7
        //$$ context.matrices.popMatrix()
        //#else
        context.matrices.pop()
        //#endif
    }

    fun String.width(): Int {
        val lines = split('\n')
        return lines.maxOf { client.textRenderer.getWidth(it.removeFormatting()) }
    }

    fun String.height(): Int {
        val lineCount = count { it == '\n' } + 1
        return client.textRenderer.fontHeight * lineCount
    }
}