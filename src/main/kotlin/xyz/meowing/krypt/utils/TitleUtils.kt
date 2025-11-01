package xyz.meowing.krypt.utils

import net.minecraft.client.gui.DrawContext
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.GuiEvent
import java.util.*

object TitleUtils {
    private data class TitleData(
        val title: String?,
        val subtitle: String?,
        val fadeIn: Int,
        val stay: Int,
        val fadeOut: Int,
        val scale: Float = 4.0f
    )

    private val titleQueue = LinkedList<TitleData>()
    private var currentTitle: TitleData? = null
    private var startTime = 0L

    init {
        EventBus.register<GuiEvent.RenderHUD> { render(it.context) }
    }

    fun showTitle(title: String?, subtitle: String?, duration: Int, scale: Float = 4.0f) {
        titleQueue.offer(TitleData(title, subtitle, 200, duration, 200, scale))
        if (currentTitle == null) nextTitle()
    }

    fun showTitle(title: String?, subtitle: String?, fadeIn: Int, stay: Int, fadeOut: Int, scale: Float = 4.0f) {
        titleQueue.offer(TitleData(title, subtitle, fadeIn, stay, fadeOut, scale))
        if (currentTitle == null) nextTitle()
    }

    private fun nextTitle() {
        currentTitle = titleQueue.poll()
        startTime = System.currentTimeMillis()
    }

    private fun render(context: DrawContext) {
        val title = currentTitle ?: return
        val elapsed = System.currentTimeMillis() - startTime
        val totalDuration = title.fadeIn + title.stay + title.fadeOut

        if (elapsed >= totalDuration) {
            nextTitle()
            return
        }

        val alpha = when {
            elapsed < title.fadeIn -> elapsed.toFloat() / title.fadeIn
            elapsed < title.fadeIn + title.stay -> 1.0f
            else -> 1.0f - ((elapsed - title.fadeIn - title.stay).toFloat() / title.fadeOut)
        }

        val scale = title.scale * (0.8f + 0.2f * alpha)
        val centerX = client.window.scaledWidth / 2f
        val centerY = client.window.scaledHeight / 2f

        val hasTitle = title.title != null
        val hasSubtitle = title.subtitle != null

        when {
            hasTitle && hasSubtitle -> {
                val titleWidth = client.textRenderer.getWidth(title.title) * scale
                val titleX = centerX - titleWidth / 2
                val titleY = centerY - (client.textRenderer.fontHeight * scale) / 2 - 2 * scale
                Render2D.renderStringWithShadow(context, title.title, titleX, titleY, scale)

                val subScale = scale * 0.7f
                val subtitleWidth = client.textRenderer.getWidth(title.subtitle) * subScale
                val subtitleX = centerX - subtitleWidth / 2
                val subtitleY = centerY + (client.textRenderer.fontHeight * subScale) / 2 + 2 * scale
                Render2D.renderStringWithShadow(context, title.subtitle, subtitleX, subtitleY, subScale)
            }
            hasTitle -> {
                val titleWidth = client.textRenderer.getWidth(title.title) * scale
                val titleX = centerX - titleWidth / 2
                val titleY = centerY - (client.textRenderer.fontHeight * scale) / 2
                Render2D.renderStringWithShadow(context, title.title, titleX, titleY, scale)
            }
            hasSubtitle -> {
                val subScale = scale * 0.7f
                val subtitleWidth = client.textRenderer.getWidth(title.subtitle) * subScale
                val subtitleX = centerX - subtitleWidth / 2
                val subtitleY = centerY - (client.textRenderer.fontHeight * subScale) / 2
                Render2D.renderStringWithShadow(context, title.subtitle, subtitleX, subtitleY, subScale)
            }
        }
    }
}