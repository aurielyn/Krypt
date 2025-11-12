package xyz.meowing.krypt.features.highlights

import net.minecraft.util.math.Box
import tech.thatgravyboat.skyblockapi.utils.regex.matchWhen
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.DungeonKey
import xyz.meowing.krypt.api.dungeons.enums.map.DoorState
import xyz.meowing.krypt.api.dungeons.enums.map.DoorType
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.rendering.Render3D
import java.awt.Color

@Module
object DoorHighlight : Feature(
    "doorHighlight",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private val keyObtainedRegex = Regex("(?:\\[.+] ?)?\\w+ has obtained (?<type>\\w+) Key!")
    private val keyPickedUpRegex = Regex("A (?<type>\\w+) Key was picked up!")
    private val witherDoorOpenRegex = Regex("\\w+ opened a WITHER door!")
    private val bloodDoorOpenRegex = Regex("The BLOOD DOOR has been opened!")

    private var witherKeyObtained = false
    private var bloodKeyObtained = false
    private var bloodOpen = false

    private val witherWithKey by ConfigDelegate<Color>("doorHighlight.witherWithKey")
    private val witherNoKey by ConfigDelegate<Color>("doorHighlight.witherNoKey")
    private val bloodWithKey by ConfigDelegate<Color>("doorHighlight.bloodWithKey")
    private val bloodNoKey by ConfigDelegate<Color>("doorHighlight.bloodNoKey")

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Door highlight",
                "Highlight wither/blood doors through walls",
                "Highlights",
                ConfigElement(
                    "doorHighlight",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Wither door with key",
                ConfigElement(
                    "doorHighlight.witherWithKey",
                    ElementType.ColorPicker(Color(0, 255, 0, 100))
                )
            )
            .addFeatureOption(
                "Wither door without key",
                ConfigElement(
                    "doorHighlight.witherNoKey",
                    ElementType.ColorPicker(Color(50, 50, 50, 100))
                )
            )
            .addFeatureOption(
                "Blood door with key",
                ConfigElement(
                    "doorHighlight.bloodWithKey",
                    ElementType.ColorPicker(Color(0, 255, 0, 100))
                )
            )
            .addFeatureOption(
                "Blood door without key",
                ConfigElement(
                    "doorHighlight.bloodNoKey",
                    ElementType.ColorPicker(Color(255, 0, 0, 100))
                )
            )
    }

    override fun initialize() {
        register<LocationEvent.WorldChange> { reset() }

        register<ChatEvent.Receive> { event ->
            val message = event.message.stripped

            matchWhen(message) {
                case(keyObtainedRegex, "type") { (type) ->
                    handleGetKey(type)
                }

                case(keyPickedUpRegex, "type") { (type) ->
                    handleGetKey(type)
                }

                case(witherDoorOpenRegex) {
                    witherKeyObtained = false
                }

                case(bloodDoorOpenRegex) {
                    bloodKeyObtained = false
                    bloodOpen = true
                }
            }
        }

        register<RenderEvent.World.Last> { event ->
            if (bloodOpen) return@register

            DungeonAPI.doors.forEach { door ->
                if (door == null) return@forEach
                if (door.state != DoorState.DISCOVERED) return@forEach
                if (door.opened) return@forEach
                if (door.type !in setOf(DoorType.WITHER, DoorType.BLOOD)) return@forEach

                val color = when (door.type) {
                    DoorType.WITHER -> if (witherKeyObtained) witherWithKey else witherNoKey
                    DoorType.BLOOD -> if (bloodKeyObtained) bloodWithKey else bloodNoKey
                    else -> return@forEach
                }

                val (x, y, z) = door.getPos()
                val box = Box(
                    x.toDouble() - 1.0, y.toDouble(), z.toDouble() - 1,
                    x.toDouble() + 2.0, y.toDouble() + 4.0, z.toDouble() + 2
                )

                Render3D.drawSpecialBB(
                    box,
                    color,
                    event.context.consumers(),
                    event.context.matrixStack(),
                    phase = true,
                    customFillAlpha = 0.6f
                )
            }
        }
    }

    private fun handleGetKey(type: String) {
        val key = DungeonKey.getById(type) ?: return
        when (key) {
            DungeonKey.WITHER -> witherKeyObtained = true
            DungeonKey.BLOOD -> bloodKeyObtained = true
        }
    }

    private fun reset() {
        witherKeyObtained = false
        bloodKeyObtained = false
        bloodOpen = false
    }
}