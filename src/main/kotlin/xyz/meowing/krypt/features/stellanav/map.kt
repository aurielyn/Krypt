package xyz.meowing.krypt.features.stellanav

import net.minecraft.client.gui.DrawContext
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.features.stellanav.utils.render.hud
import xyz.meowing.krypt.hud.HudManager
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager

@Module
object map: Feature("stellanav.enabled", island = SkyBlockIsland.THE_CATACOMBS) {
    private const val name = "StellaNav"

    override fun addConfig() {
        ConfigManager.addFeature(
            "Dungeon Map",
            "Enables the dungeon map",
            "StellaNav",
            ConfigElement("stellanav.enabled", ElementType.Switch(false))
        )
    }

    override fun initialize() {
        HudManager.registerCustom(name, 148, 148, {hud.renderPreview(it, 0f, 0f)}, "stellanav.enabled")

        register<GuiEvent.RenderHUD> { event ->
            renderMap(event.context)
        }
    }

    fun renderMap(context: DrawContext) {
        val x = HudManager.getX(name)
        val y = HudManager.getY(name)
        val scale = HudManager.getScale(name)

        hud.render(context, x, y, scale)
    }
}