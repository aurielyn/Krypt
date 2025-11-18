package xyz.meowing.krypt.features.solvers

import net.minecraft.core.BlockPos
import net.minecraft.util.CommonColors
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.events.core.EntityEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.events.core.WorldEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.Utils.toFloatArray
import xyz.meowing.krypt.utils.glowThisFrame
import xyz.meowing.krypt.utils.glowingColor
import xyz.meowing.krypt.utils.modMessage
import xyz.meowing.krypt.utils.rendering.Render3D
import java.awt.Color

/**
 * Modified from Odin's LividSolver.
 * Has a cool highlight along with a tracer now.
 *
 * Original File: [GitHub](https://github.com/odtheking/OdinFabric/blob/main/src/main/kotlin/com/odtheking/odin/features/impl/dungeon/LividSolver.kt)
 */
@Module
object LividSolver : Feature(
    "lividSolver",
    dungeonFloor = listOf(DungeonFloor.F5, DungeonFloor.M5)
) {
    private val ceilingWoolBlock = BlockPos(5, 108, 43)
    private var currentLivid: Livid? = null

    private val highlight by ConfigDelegate<Boolean>("lividSolver.highlight")
    private val tracer by ConfigDelegate<Boolean>("lividSolver.showTracer")

    private enum class Livid(
        val entityName: String,
        val colorCode: Char,
        val color: Color,
        val wool: Block
    ) {
        VENDETTA("Vendetta", 'f', Color(CommonColors.WHITE), Blocks.WHITE_WOOL),
        CROSSED("Crossed", 'd', Color(CommonColors.DARK_PURPLE), Blocks.MAGENTA_WOOL),
        ARCADE("Arcade", 'e', Color(CommonColors.YELLOW), Blocks.YELLOW_WOOL),
        SMILE("Smile", 'a', Color(CommonColors.GREEN), Blocks.LIME_WOOL),
        DOCTOR("Doctor", '7', Color(CommonColors.GRAY), Blocks.GRAY_WOOL),
        PURPLE("Purple", '5', Color(CommonColors.DARK_PURPLE), Blocks.PURPLE_WOOL),
        SCREAM("Scream", '9', Color(CommonColors.BLUE), Blocks.BLUE_WOOL),
        FROG("Frog", '2', Color(CommonColors.GREEN), Blocks.GREEN_WOOL),
        HOCKEY("Hockey", 'c', Color(CommonColors.RED), Blocks.RED_WOOL);

        var entity: Player? = null
    }

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Livid Solver",
                "Shows the correct Livid in F5/M5",
                "Solvers",
                ConfigElement(
                    "lividSolver",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Highlight livid",
                ConfigElement(
                    "lividSolver.highlight",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Show tracer",
                ConfigElement(
                    "lividSolver.showTracer",
                    ElementType.Switch(true)
                )
            )
    }

    override fun initialize() {
        register<WorldEvent.BlockUpdate> { event ->
            if (!DungeonAPI.inBoss || event.pos != ceilingWoolBlock) return@register

            currentLivid = Livid.entries.find {
                it.wool.defaultBlockState() == event.new.block.defaultBlockState()
            } ?: return@register

            scheduleMessage()
        }

        register<EntityEvent.Packet.Metadata> { event ->
            if (!DungeonAPI.inBoss) return@register

            val livid = currentLivid ?: return@register

            scheduleEntityUpdate(event.entity, livid)
        }

        register<RenderEvent.Entity.Pre> { event ->
            if (!highlight) return@register
            if (!DungeonAPI.inBoss) return@register
            if (currentLivid?.entity != event.entity) return@register
            if (client.player?.hasEffect(MobEffects.BLINDNESS) == true) return@register

            val entity = event.entity
            if (client.player?.hasLineOfSight(entity) == false) return@register

            entity.glowThisFrame = true
            entity.glowingColor = currentLivid!!.color.rgb
        }

        register<RenderEvent.World.Last> { event ->
            if (!tracer) return@register
            if (!DungeonAPI.inBoss) return@register

            currentLivid?.entity?.let { entity ->
                if (client.player?.hasLineOfSight(entity) == false) return@register

                Render3D.drawLineToEntity(
                    entity,
                    event.context.consumers(),
                    event.context.matrixStack(),
                    currentLivid!!.color.toFloatArray(),
                    1f
                )
            }
        }

        register<LocationEvent.WorldChange> {
            reset()
        }
    }

    private fun scheduleMessage() {
        val livid = currentLivid ?: return
        val blindnessDuration = client.player?.getEffect(MobEffects.BLINDNESS)?.duration ?: 0
        val delay = (blindnessDuration - 20).coerceAtLeast(1).toLong()

        TickScheduler.Client.schedule(delay) {
            KnitChat.modMessage("§7Found Livid: §${livid.colorCode}${livid.entityName}")
        }
    }

    private fun scheduleEntityUpdate(entity: Entity, livid: Livid) {
        val blindnessDuration = client.player?.getEffect(MobEffects.BLINDNESS)?.duration ?: 0
        val delay = (blindnessDuration - 20).coerceAtLeast(1).toLong()

        TickScheduler.Client.schedule(delay) {
            val player = entity as? Player ?: return@schedule
            if (player.name.stripped == "${livid.entityName} Livid") livid.entity = player
        }
    }

    private fun reset() {
        currentLivid = null
        Livid.entries.forEach { it.entity = null }
    }
}