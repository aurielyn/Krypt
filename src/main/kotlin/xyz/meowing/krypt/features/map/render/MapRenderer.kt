package xyz.meowing.krypt.features.map.render

import net.minecraft.client.gui.DrawContext
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.features.map.DungeonMap
import xyz.meowing.krypt.features.map.utils.Utils
import xyz.meowing.krypt.utils.Render2D
import xyz.meowing.krypt.utils.Render2D.pushPop
import xyz.meowing.krypt.utils.Render2D.width

object MapRenderer {
    private val defaultMapSize = Pair(138, 138)

    fun render(context: DrawContext, x: Float, y: Float, scale: Float) {
        val matrix = context.matrices
        context.pushPop {
            //#if MC >= 1.21.7
            //$$ matrix.translate(x, y)
            //$$ matrix.scale(scale, scale)
            //$$ matrix.translate(5f, 5f)
            //#else
            matrix.translate(x, y, 0f)
            matrix.scale(scale, scale, 1f)
            matrix.translate(5f, 5f, 0f)
            //#endif

            if (!DungeonAPI.floorCompleted) {
                renderMapBackground(context)
                Main.renderMap(context)
                if (DungeonMap.mapInfoUnder) renderInfoUnder(context, false)
                if (DungeonMap.mapBorder) renderMapBorder(context)
            } else if (DungeonMap.scoreMapEnabled) {
                renderMapBackground(context)
                if (DungeonMap.mapInfoUnder) renderInfoUnder(context, false)
                if (DungeonMap.mapBorder) renderMapBorder(context)
            }
        }
    }

    fun renderBoss(context: DrawContext, x: Float, y: Float, scale: Float) {
        val matrix = context.matrices
        context.pushPop {
            //#if MC >= 1.21.7
            //$$ matrix.translate(x, y)
            //$$ matrix.scale(scale, scale)
            //#else
            matrix.translate(x, y, 0f)
            matrix.scale(scale, scale, 1f)
            //#endif

            renderMapBackground(context)
            BossMapRenderer.renderBossMap(context)
            if (DungeonMap.mapInfoUnder) renderInfoUnder(context, false)
            if (DungeonMap.mapBorder) renderMapBorder(context)
        }
    }

    fun renderScore(context: DrawContext, x: Float, y: Float, scale: Float) {
        val matrix = context.matrices
        context.pushPop {
            //#if MC >= 1.21.7
            //$$ matrix.translate(x, y)
            //$$ matrix.scale(scale, scale)
            //#else
            matrix.translate(x, y, 0f)
            matrix.scale(scale, scale, 1f)
            //#endif

            renderMapBackground(context)
            ScoreMapRenderer.renderScoreMap(context)
            if (DungeonMap.mapInfoUnder) renderInfoUnder(context, false)
            if (DungeonMap.mapBorder) renderMapBorder(context)
        }
    }

    fun renderPreview(context: DrawContext, x: Float, y: Float) {
        val matrix = context.matrices
        context.pushPop {
            //#if MC >= 1.21.7
            //$$ matrix.translate(x + 5f, y + 5f)
            //#else
            matrix.translate(x + 5f, y + 5f, 0f)
            //#endif

            renderMapBackground(context)
            Render2D.drawImage(context, Utils.defaultMap, 5, 5, 128, 128)
            if (DungeonMap.mapInfoUnder) renderInfoUnder(context, true)
        }
    }

    fun renderInfoUnder(context: DrawContext, preview: Boolean) {
        val matrix = context.matrices
        var mapLine1 = DungeonAPI.mapLine1
        var mapLine2 = DungeonAPI.mapLine2
        if (preview) {
            mapLine1 = "§7Secrets: §b?    §7Crypts: §c0    §7Mimic: §c✘"
            mapLine2 = "§7Min Secrets: §b?    §7Deaths: §a0    §7Score: §c0"
        }
        val w1 = mapLine1.width().toFloat()
        val w2 = mapLine2.width().toFloat()

        context.pushPop {
            //#if MC >= 1.21.7
            //$$ matrix.translate(69f, 135f)
            //$$ matrix.scale(0.6f, 0.6f)
            //#else
            matrix.translate(69f, 135f, 0f)
            matrix.scale(0.6f, 0.6f, 1f)
            //#endif

            Render2D.renderString(context, mapLine1, -w1 / 2f, 0f, 1f)
            Render2D.renderString(context, mapLine2, -w2 / 2f, 10f, 1f)
        }
    }

    fun renderMapBackground(context: DrawContext) {
        val w = defaultMapSize.first
        var h = defaultMapSize.second
        h += if (DungeonMap.mapInfoUnder) 10 else 0
        Render2D.drawRect(context, 0, 0, w, h, DungeonMap.mapBackgroundColor)
    }

    fun renderMapBorder(context: DrawContext) {
        val (w, baseH) = defaultMapSize
        val borderWidth = DungeonMap.mapBorderWidth
        val h = baseH + if (DungeonMap.mapInfoUnder) 10 else 0
        val color = DungeonMap.mapBorderColor

        Render2D.drawRect(context, -borderWidth, -borderWidth, w + borderWidth * 2, borderWidth, color)
        Render2D.drawRect(context, -borderWidth, h, w + borderWidth * 2, borderWidth, color)
        Render2D.drawRect(context, -borderWidth, 0, borderWidth, h, color)
        Render2D.drawRect(context, w, 0, borderWidth, h, color)
    }
}