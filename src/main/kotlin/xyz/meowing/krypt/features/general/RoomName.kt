package xyz.meowing.krypt.features.general

import net.minecraft.client.gui.DrawContext
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.Dungeon
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.hud.HudEditor
import xyz.meowing.krypt.hud.HudManager
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.Render2D

@Module
object RoomName: Feature("general.roomName", island = SkyBlockIsland.THE_CATACOMBS) {
    var chroma by ConfigDelegate<Boolean>("general.roomName.chroma")
    var name = "room name"

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Room Name Hud", "Displays the current rooms name", "Dungeons", ConfigElement(
                    "general.roomName",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Chroma Room Name", "Makes the room name hud chroma (requires sba / sh)", "Options", ConfigElement(
                    "general.roomName.chroma",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption("HudEditor", "Opens Hud Editor", "Options", ConfigElement(
                "general.roomName.hudEditor",
                ElementType.Button("Edit Position") {
                    TickScheduler.Client.post {
                        client.execute { client.setScreen(HudEditor()) }
                    }
                }
            ))
    }

    override fun initialize() {
        HudManager.register(name, "No Room Found", "general.roomName")
        register<GuiEvent.RenderHUD> { renderHud(it.context) }
    }

    private fun renderHud(context: DrawContext) {
        if (Dungeon.inBoss) return

        val text = "${if (chroma) "§z" else ""}${Dungeon.currentRoom?.name ?: "No Room Found"}"
        val x = HudManager.getX(name) + 5f
        val y = HudManager.getY(name) + 5f
        val scale = HudManager.getScale(name)

        Render2D.renderString(context,text, x, y, scale)
    }
}