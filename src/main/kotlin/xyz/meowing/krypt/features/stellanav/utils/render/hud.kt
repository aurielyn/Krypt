package xyz.meowing.krypt.features.stellanav.utils.render

import net.minecraft.client.gui.DrawContext
import xyz.meowing.krypt.api.dungeons.Dungeon
import xyz.meowing.krypt.features.stellanav.utils.utils
import xyz.meowing.krypt.utils.Render2D
import xyz.meowing.krypt.utils.Render2D.width
import java.awt.Color

object hud {
    private val defaultMapSize = Pair(138, 138)
    private val mapInfoUnder = true // Config this
    private val mapBorder = false // Config this

    fun render(context: DrawContext, x: Float, y: Float, scale: Float) {
        val matrix = context.matrices

        matrix.push()
        matrix.translate(x, y, 0f)
        matrix.scale(scale, scale, 1f)
        matrix.translate(5f,5f, 0f)

        if(!Dungeon.inBoss && !Dungeon.floorCompleted) {
            renderMapBackground(context)
            clear.renderMap(context)
            if (mapInfoUnder) renderInfoUnder(context, false)
            if (mapBorder) renderMapBorder(context)
        } else if (!Dungeon.floorCompleted /*&& mapConfig.bossMapEnabled*/) {
            renderMapBackground(context)
            //boss.renderMap(context)

            if (mapInfoUnder) renderInfoUnder(context, false)
            if (mapBorder) renderMapBorder(context)
        } else /*if (Dungeon.floorCompleted && mapConfig.scoreMapEnabled)*/ {
            renderMapBackground(context)
            //score.render(context)

            if (mapInfoUnder) renderInfoUnder(context, false)
            if (mapBorder) renderMapBorder(context)
        }

        matrix.pop()
    }

    fun renderPreview(context: DrawContext, x: Float, y: Float) {
        val matrix = context.matrices

        matrix.push()
        matrix.translate(x + 5f, y + 5f, 0f)

        renderMapBackground(context)

        Render2D.drawImage(context, utils.prevewMap, 5,5,128,128)

        if (mapInfoUnder) renderInfoUnder(context, true)

        matrix.pop()
    }

    fun renderInfoUnder(context: DrawContext, preview: Boolean) {
        val matrix = context.matrices

        var mapLine1 = Dungeon.mapLine1
        var mapLine2 = Dungeon.mapLine2

        if (preview) {
            mapLine1 = "§7Secrets: §b?    §7Crypts: §c0    §7Mimic: §c✘";
            mapLine2 = "§7Min Secrets: §b?    §7Deaths: §a0    §7Score: §c0";
        }

        val w1 = mapLine1.width().toFloat()
        val w2 = mapLine2.width().toFloat()

        matrix.push()
        matrix.translate(138f / 2f, 135f, 0f)
        matrix.scale(0.6f, 0.6f, 1f)

        Render2D.renderString(context, mapLine1, -w1 / 2f, 0f, 1f)
        Render2D.renderString(context, mapLine2, -w2 / 2f, 10f, 1f)

        matrix.pop()
    }


    fun renderMapBackground(context: DrawContext) {
        val w = defaultMapSize.first
        var h = defaultMapSize.second
        h += if (mapInfoUnder) 10 else 0

        Render2D.drawRect(context, 0, 0, w, h, /*Config this*/ Color(0,0,0,100))
    }

    fun renderMapBorder(context: DrawContext) {
        val (w, baseH) = defaultMapSize
        val borderWidth = 2 // Config this
        val h = baseH + if (mapInfoUnder) 10 else 0
        val color = Color(0,0,0, 255) // Config this
        // Top border
        Render2D.drawRect(context, -borderWidth, -borderWidth, w + borderWidth * 2, borderWidth, color)

        // Bottom border
        Render2D.drawRect(context, -borderWidth, h, w + borderWidth * 2, borderWidth, color)

        // Left border
        Render2D.drawRect(context, -borderWidth, 0, borderWidth, h, color)

        // Right border
        Render2D.drawRect(context, w, 0, borderWidth, h, color)
    }
}