package xyz.meowing.krypt.features.general

import net.minecraft.client.gui.DrawContext
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.krypt.Krypt
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.Dungeon
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.hud.HudEditor
import xyz.meowing.krypt.hud.HudManager
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.Render2D
import xyz.meowing.krypt.utils.Render2D.width

@Module
object RoomSecrets: Feature("general.roomSecrets", island = SkyBlockIsland.THE_CATACOMBS) {
    private const val name = "Secrets Display"

    override fun addConfig() {
        ConfigManager
        .addFeature("Room Secrets HUD", "Shows the secrets in the current dungeon room", "Dungeons", ConfigElement(
            "general.roomSecrets",
            ElementType.Switch(false)
        ))
        .addFeatureOption("HudEditor", "Opens Hud Editor", "Options", ConfigElement(
            "general.roomSecrets.hudEditor",
            ElementType.Button("Edit Position") {
                TickScheduler.Client.post {
                    client.execute { client.setScreen(HudEditor()) }
                }
            }
        ))
    }

    override fun initialize() {
        HudManager.registerCustom(name, 50,30, this::hudEditorRender, "general.roomSecrets")

        register<GuiEvent.RenderHUD> { renderHUD(it.context) }
    }

    fun hudEditorRender(context: DrawContext){
        val matrix = context.matrices
        //#if MC >= 1.21.7
        //$$ matrix.pushMatrix()
        //#else
        matrix.push()
        //#endif

        val text1 = "§fSecrets"
        val text2 = "§a7§7/§a7"
        val w1 = text1.width().toFloat()
        val w2 = text2.width().toFloat()

        //#if MC >= 1.21.7
        //$$ matrix.translate(25f, 5f)
        //#else
        matrix.translate(25f, 5f, 0f)
        //#endif

        Render2D.renderString(context, text1, -w1 / 2f, 0f, 1f)
        Render2D.renderString(context, text2, -w2 / 2f, 10f, 1f)

        //#if MC >= 1.21.7
        //$$ matrix.popMatrix()
        //#else
        matrix.pop()
        //#endif
    }

    private fun renderHUD(context: DrawContext) {
        val matrix = context.matrices
        val x = HudManager.getX(name)
        val y = HudManager.getY(name)
        val scale = HudManager.getScale(name)

        //#if MC >= 1.21.7
        //$$ matrix.pushMatrix()
        //$$ matrix.scale(scale, scale)
        //$$ matrix.translate(x, y)
        //#else
        matrix.push()
        matrix.scale(scale, scale, 1f)
        matrix.translate(x, y, 0f)
        //#endif

        val text1 = "§fSecrets"
        val text2 = getText()
        val w1 = text1.width().toFloat()
        val w2 = text2.width().toFloat()

        //#if MC >= 1.21.7
        //$$ matrix.translate(25f, 5f)
        //#else
        matrix.translate(25f, 5f, 0f)
        //#endif

        Render2D.renderString(context, text1, -w1 / 2f, 0f, 1f)
        Render2D.renderString(context, text2, -w2 / 2f, 10f, 1f)

        //#if MC >= 1.21.7
        //$$ matrix.popMatrix()
        //#else
        matrix.pop()
        //#endif
    }

    private fun getText(): String {
        val room = Dungeon.currentRoom
        //Krypt.LOGGER.info("Current room: ${room?.name}")
        val found = room?.secretsFound ?: 0
        val total = room?.secrets ?: 0
        var text: String

        if ((found == 0 || found == -1) && total == 0) {
            text = "§7None"
            return text
        }

        val percent = found.toFloat() / total.toFloat()

        text = when {
            percent < 0.5f -> "§c$found§7/§c$total"
            percent <   1f -> "§e$found§7/§e$total"
            else           -> "§a$found§7/§a$total"
        }

        return text
    }
}