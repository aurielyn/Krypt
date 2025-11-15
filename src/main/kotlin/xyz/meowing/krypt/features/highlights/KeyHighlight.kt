package xyz.meowing.krypt.features.highlights

import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.events.core.EntityEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.Utils.toFloatArray
import xyz.meowing.krypt.utils.rendering.Render3D
import java.awt.Color

@Module
object KeyHighlight : Feature(
    "keyHighlight",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private val highlightWither by ConfigDelegate<Boolean>("keyHighlight.wither")
    private val highlightBlood by ConfigDelegate<Boolean>("keyHighlight.blood")
    private val witherColor by ConfigDelegate<Color>("keyHighlight.witherColor")
    private val bloodColor by ConfigDelegate<Color>("keyHighlight.bloodColor")

    private var doorKey: Pair<Entity, Color>? = null

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Key highlight",
                "Highlight blood/wither door key in the world",
                "Highlights",
                ConfigElement(
                    "keyHighlight",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Wither key",
                ConfigElement(
                    "keyHighlight.wither",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Blood key",
                ConfigElement(
                    "keyHighlight.blood",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Wither key color",
                ConfigElement(
                    "keyHighlight.witherColor",
                    ElementType.ColorPicker(Color(0, 0, 0, 60))
                )
            )
            .addFeatureOption(
                "Blood key color",
                ConfigElement(
                    "keyHighlight.bloodColor",
                    ElementType.ColorPicker(Color(255, 0, 0, 60))
                )
            )
    }

    override fun initialize() {
        register<EntityEvent.Packet.Metadata> { event ->
            if (DungeonAPI.inBoss) return@register

            val entityName = event.entity.name.stripped

            when (entityName) {
                "Wither Key" -> if (highlightWither) doorKey = Pair(event.entity, witherColor)
                "Blood Key" -> if (highlightBlood) doorKey = Pair(event.entity, bloodColor)
            }
        }

        register<RenderEvent.World.Last> { event ->
            if (DungeonAPI.inBoss) return@register

            doorKey?.let { (entity, color) ->
                if (entity.isRemoved) {
                    doorKey = null
                    return@register
                }

                val matrices = event.context.matrixStack()
                val consumers = event.context.consumers()

                Render3D.drawLineToEntity(
                    entity,
                    consumers,
                    matrices,
                    color.toFloatArray(),
                    1f
                )

                val box = AABB(
                    entity.x - 0.4,
                    entity.y + 1.2,
                    entity.z - 0.4,
                    entity.x + 0.4,
                    entity.y + 2.0,
                    entity.z + 0.4
                )
                Render3D.drawSpecialBB(box, color, consumers, matrices)
            }
        }

        register<LocationEvent.WorldChange> {
            doorKey = null
        }
    }
}