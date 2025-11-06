package xyz.meowing.krypt.utils

import net.minecraft.block.entity.SkullBlockEntity
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.PlayerSkinDrawer
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.util.DefaultSkinHelper
import net.minecraft.item.ItemStack
import net.minecraft.util.Colors
import net.minecraft.util.Identifier
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.krypt.utils.StringUtils.removeFormatting
import java.awt.Color
import java.util.Optional
import java.util.UUID

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

    fun drawPlayerHead(context: DrawContext, x: Int, y: Int, size: Int, uuid: UUID) {
        val textures = SkullBlockEntity.fetchProfileByUuid(uuid)
            .getNow(Optional.empty())
            .map(client.skinProvider::getSkinTextures)
            .orElseGet { DefaultSkinHelper.getSkinTextures(uuid) }

        PlayerSkinDrawer.draw(context, textures, x, y, size)
    }

    fun drawImage(ctx: DrawContext, image: Identifier, x: Int, y: Int, width: Int, height: Int) {
        ctx.drawGuiTexture(RenderLayer::getGuiTextured, image, x, y, width, height)
    }

    fun drawRect(ctx: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Color = Color.WHITE) {
        ctx.fill(RenderLayer.getGui(), x, y, x + width, y + height, color.rgb)
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