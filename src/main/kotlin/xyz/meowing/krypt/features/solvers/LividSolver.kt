package xyz.meowing.krypt.features.solvers

import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.rendering.Render3D
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.DyeColor
import net.minecraft.core.BlockPos
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitClient.world
import xyz.meowing.knit.api.KnitPlayer.player
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.Utils.toFloatArray
import xyz.meowing.krypt.utils.glowThisFrame
import xyz.meowing.krypt.utils.glowingColor
import java.awt.Color

@Module
object LividSolver : Feature(
    "lividSolver",
    dungeonFloor = listOf(DungeonFloor.F5, DungeonFloor.M5)
) {
    private var lividEntity: Entity? = null
    private val lividPos = BlockPos(5, 108, 42)
    private val lividTypes = mapOf(
        Blocks.WHITE_STAINED_GLASS to "Vendetta",
        Blocks.MAGENTA_STAINED_GLASS to "Crossed",
        Blocks.PINK_STAINED_GLASS to "Crossed",
        Blocks.RED_STAINED_GLASS to "Hockey",
        Blocks.GRAY_STAINED_GLASS to "Doctor",
        Blocks.GREEN_STAINED_GLASS to "Frog",
        Blocks.LIME_STAINED_GLASS to "Smile",
        Blocks.BLUE_STAINED_GLASS to "Scream",
        Blocks.PURPLE_STAINED_GLASS to "Purple",
        Blocks.YELLOW_STAINED_GLASS to "Arcade"
    )

    private val highlightLividColor by ConfigDelegate<Color>("highlightLivid.color")
    private val highlightLividLine by ConfigDelegate<Boolean>("highlightLivid.line")

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Livid solver",
                "Shows the correct Livid in F5/M5",
                "Solvers",
                ConfigElement(
                    "lividSolver",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Highlight correct livid color",
                ConfigElement(
                    "lividSolver.color",
                    ElementType.ColorPicker(Color(0, 255, 255, 127))
                )
            )
            .addFeatureOption(
                "Tracer",
                ConfigElement(
                    "lividSolver.line",
                    ElementType.Switch(false)
                )
            )
    }

    override fun initialize() {
        createCustomEvent<RenderEvent.Entity.Pre>("renderLivid") { event ->
            val entity = event.entity

            if (lividEntity == entity && player?.hasLineOfSight(entity) == true) {
                entity.glowThisFrame = true
                entity.glowingColor = highlightLividColor.rgb
            }
        }

        createCustomEvent<RenderEvent.World.Last>("renderLine") { event ->
            lividEntity?.let { entity ->
                if (player?.hasLineOfSight(entity) == true) {
                    Render3D.drawLineToEntity(
                        entity,
                        event.context.consumers(),
                        event.context.matrixStack(),
                        highlightLividColor.toFloatArray(),
                        highlightLividColor.alpha.toFloat()
                    )
                }
            }
        }

        createCustomEvent<TickEvent.Server>("tick") {
            val world = world ?: return@createCustomEvent
            val state: BlockState = world.getBlockState(lividPos) ?: return@createCustomEvent
            val lividType = lividTypes[state.block] ?: return@createCustomEvent

            world.players().find { it.name.stripped.contains(lividType) }?.let {
                lividEntity = it
                registerRender()
                unregisterEvent("tick")
            }
        }

        register<ChatEvent.Receive> { event ->
            if (event.message.stripped == "[BOSS] Livid: I respect you for making it to here, but I'll be your undoing.") {
                TickScheduler.Server.schedule(80) {
                    registerEvent("tick")
                }
            }
        }

        register<LocationEvent.WorldChange> {
            unregisterRender()
        }
    }

    private fun registerRender() {
        registerEvent("renderLivid")
        if (highlightLividLine) registerEvent("renderLine")
    }

    private fun unregisterRender() {
        unregisterEvent("renderLivid")
        unregisterEvent("renderWrong")
        unregisterEvent("renderLine")
        lividEntity = null
    }
}