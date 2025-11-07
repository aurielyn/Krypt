package xyz.meowing.krypt.features.general.map

import net.minecraft.client.gui.DrawContext
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.elements.MCColorCode
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.features.general.map.render.MapRenderer
import xyz.meowing.krypt.hud.HudManager
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import java.awt.Color

@Module
object DungeonMap : Feature(
    "dungeonMap",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private const val NAME = "Dungeon Map"

    var showPlayerHead by ConfigDelegate<Boolean>("dungeonMap.showPlayerHead")
    var iconClassColors by ConfigDelegate<Boolean>("dungeonMap.iconClassColors")
    var playerIconBorderColor by ConfigDelegate<Color>("dungeonMap.playerIconBorderColor")
    var playerIconBorderSize by ConfigDelegate<Double>("dungeonMap.playerIconBorderSize")
    var showOwnPlayer by ConfigDelegate<Boolean>("dungeonMap.showOwnPlayer")
    var showPlayerNametags by ConfigDelegate<Boolean>("dungeonMap.showPlayerNametags")

    var healerColor by ConfigDelegate<Color>("dungeonMap.healerColor")
    var mageColor by ConfigDelegate<Color>("dungeonMap.mageColor")
    var berserkColor by ConfigDelegate<Color>("dungeonMap.berserkColor")
    var archerColor by ConfigDelegate<Color>("dungeonMap.archerColor")
    var tankColor by ConfigDelegate<Color>("dungeonMap.tankColor")

    var puzzleCheckmarkMode by ConfigDelegate<Int>("dungeonMap.puzzleCheckmarkMode")
    var normalCheckmarkMode by ConfigDelegate<Int>("dungeonMap.normalCheckmarkMode")
    var checkmarkScale by ConfigDelegate<Double>("dungeonMap.checkmarkScale")
    var roomNameColor by ConfigDelegate<MCColorCode>("dungeonMap.roomNameColor")
    var secretsColor by ConfigDelegate<MCColorCode>("dungeonMap.secretsColor")
    var roomLabelScale by ConfigDelegate<Double>("dungeonMap.roomLabelScale")

    var showClearedRoomCheckmarks by ConfigDelegate<Boolean>("dungeonMap.showClearedRoomCheckmarks")
    var clearedRoomCheckmarkScale by ConfigDelegate<Double>("dungeonMap.clearedRoomCheckmarkScale")

    var normalRoomColor by ConfigDelegate<Color>("dungeonMap.normalRoomColor")
    var puzzleRoomColor by ConfigDelegate<Color>("dungeonMap.puzzleRoomColor")
    var trapRoomColor by ConfigDelegate<Color>("dungeonMap.trapRoomColor")
    var yellowRoomColor by ConfigDelegate<Color>("dungeonMap.yellowRoomColor")
    var bloodRoomColor by ConfigDelegate<Color>("dungeonMap.bloodRoomColor")
    var fairyRoomColor by ConfigDelegate<Color>("dungeonMap.fairyRoomColor")
    var entranceRoomColor by ConfigDelegate<Color>("dungeonMap.entranceRoomColor")

    var normalDoorColor by ConfigDelegate<Color>("dungeonMap.normalDoorColor")
    var witherDoorColor by ConfigDelegate<Color>("dungeonMap.witherDoorColor")
    var bloodDoorColor by ConfigDelegate<Color>("dungeonMap.bloodDoorColor")
    var entranceDoorColor by ConfigDelegate<Color>("dungeonMap.entranceDoorColor")

    var mapInfoUnder by ConfigDelegate<Boolean>("dungeonMap.mapInfoUnder")
    var mapBorder by ConfigDelegate<Boolean>("dungeonMap.mapBorder")
    var mapBorderWidth by ConfigDelegate<Int>("dungeonMap.mapBorderWidth")
    var mapBorderColor by ConfigDelegate<Color>("dungeonMap.mapBorderColor")
    var mapBackgroundColor by ConfigDelegate<Color>("dungeonMap.mapBackgroundColor")

    var bossMapEnabled by ConfigDelegate<Boolean>("dungeonMap.bossMapEnabled")
    var scoreMapEnabled by ConfigDelegate<Boolean>("dungeonMap.scoreMapEnabled")

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Dungeon Map",
                "Enables the dungeon map",
                "Map",
                ConfigElement(
                    "dungeonMap",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Show Player Heads",
                ConfigElement(
                    "dungeonMap.showPlayerHead",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Class Colored Icons",
                ConfigElement(
                    "dungeonMap.iconClassColors",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Player Icon Border Color",
                ConfigElement(
                    "dungeonMap.playerIconBorderColor",
                    ElementType.ColorPicker(Color(0, 0, 0, 255))
                )
            )
            .addFeatureOption(
                "Player Icon Border Size",
                ConfigElement(
                    "dungeonMap.playerIconBorderSize",
                    ElementType.Slider(0.0, 0.5, 0.2, true)
                )
            )
            .addFeatureOption(
                "Show Own Player",
                ConfigElement(
                    "dungeonMap.showOwnPlayer",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Show Player Nametags",
                ConfigElement(
                    "dungeonMap.showPlayerNametags",
                    ElementType.Switch(true)
                )
            )

        ConfigManager
            .addFeature(
                "Class Colors",
                "Configure class icon colors",
                "Map",
                ConfigElement(
                    "dungeonMap.classColors",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Healer Color",
                ConfigElement(
                    "dungeonMap.healerColor",
                    ElementType.ColorPicker(Color(240, 70, 240, 255))
                )
            )
            .addFeatureOption(
                "Mage Color",
                ConfigElement(
                    "dungeonMap.mageColor",
                    ElementType.ColorPicker(Color(70, 210, 210, 255))
                )
            )
            .addFeatureOption(
                "Berserk Color",
                ConfigElement(
                    "dungeonMap.berserkColor",
                    ElementType.ColorPicker(Color(70, 210, 210, 255))
                )
            )
            .addFeatureOption(
                "Archer Color",
                ConfigElement(
                    "dungeonMap.archerColor",
                    ElementType.ColorPicker(Color(254, 223, 0, 255))
                )
            )
            .addFeatureOption(
                "Tank Color",
                ConfigElement(
                    "dungeonMap.tankColor",
                    ElementType.ColorPicker(Color(30, 170, 50, 255))
                )
            )

        ConfigManager
            .addFeature(
                "Room Labels",
                "Configure room label display",
                "Map",
                ConfigElement(
                    "dungeonMap.roomLabels",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Puzzle Room Labels",
                ConfigElement(
                    "dungeonMap.puzzleCheckmarkMode",
                    ElementType.Dropdown(
                        listOf("Nothing", "Name Only", "Secrets Only", "Name & Secrets"),
                        3
                    )
                )
            )
            .addFeatureOption(
                "Normal Room Labels",
                ConfigElement(
                    "dungeonMap.normalCheckmarkMode",
                    ElementType.Dropdown(
                        listOf("Nothing", "Name Only", "Secrets Only", "Name & Secrets"),
                        2
                    )
                )
            )
            .addFeatureOption(
                "Checkmark Scale",
                ConfigElement(
                    "dungeonMap.checkmarkScale",
                    ElementType.Slider(0.5, 2.0, 1.0, true)
                )
            )
            .addFeatureOption(
                "Room Name Color",
                ConfigElement(
                    "dungeonMap.roomNameColor",
                    ElementType.MCColorPicker(MCColorCode.WHITE)
                )
            )
            .addFeatureOption(
                "Secrets Text Color",
                ConfigElement(
                    "dungeonMap.secretsColor",
                    ElementType.MCColorPicker(MCColorCode.AQUA)
                )
            )
            .addFeatureOption(
                "Room Label Scale",
                ConfigElement(
                    "dungeonMap.roomLabelScale",
                    ElementType.Slider(0.5, 2.0, 1.0, true)
                )
            )
            .addFeatureOption(
                "Show Cleared Room Checkmarks",
                ConfigElement(
                    "dungeonMap.showClearedRoomCheckmarks",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Cleared Room Checkmark Scale",
                ConfigElement(
                    "dungeonMap.clearedRoomCheckmarkScale",
                    ElementType.Slider(0.5, 2.0, 1.0, true)
                )
            )

        ConfigManager
            .addFeature(
                "Room Colors",
                "Configure room type colors",
                "Map",
                ConfigElement(
                    "dungeonMap.roomColors",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Normal Room Color",
                ConfigElement(
                    "dungeonMap.normalRoomColor",
                    ElementType.ColorPicker(Color(107, 58, 17, 255))
                )
            )
            .addFeatureOption(
                "Puzzle Room Color",
                ConfigElement(
                    "dungeonMap.puzzleRoomColor",
                    ElementType.ColorPicker(Color(117, 0, 133, 255))
                )
            )
            .addFeatureOption(
                "Trap Room Color",
                ConfigElement(
                    "dungeonMap.trapRoomColor",
                    ElementType.ColorPicker(Color(216, 127, 51, 255))
                )
            )
            .addFeatureOption(
                "Yellow Room Color",
                ConfigElement(
                    "dungeonMap.yellowRoomColor",
                    ElementType.ColorPicker(Color(254, 223, 0, 255))
                )
            )
            .addFeatureOption(
                "Blood Room Color",
                ConfigElement(
                    "dungeonMap.bloodRoomColor",
                    ElementType.ColorPicker(Color(255, 0, 0, 255))
                )
            )
            .addFeatureOption(
                "Fairy Room Color",
                ConfigElement(
                    "dungeonMap.fairyRoomColor",
                    ElementType.ColorPicker(Color(224, 0, 255, 255))
                )
            )
            .addFeatureOption(
                "Entrance Room Color",
                ConfigElement(
                    "dungeonMap.entranceRoomColor",
                    ElementType.ColorPicker(Color(20, 133, 0, 255))
                )
            )

        ConfigManager
            .addFeature(
                "Door Colors",
                "Configure door type colors",
                "Map",
                ConfigElement(
                    "dungeonMap.doorColors",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Normal Door Color",
                ConfigElement(
                    "dungeonMap.normalDoorColor",
                    ElementType.ColorPicker(Color(80, 40, 10, 255))
                )
            )
            .addFeatureOption(
                "Wither Door Color",
                ConfigElement(
                    "dungeonMap.witherDoorColor",
                    ElementType.ColorPicker(Color(0, 0, 0, 255))
                )
            )
            .addFeatureOption(
                "Blood Door Color",
                ConfigElement(
                    "dungeonMap.bloodDoorColor",
                    ElementType.ColorPicker(Color(255, 0, 0, 255))
                )
            )
            .addFeatureOption(
                "Entrance Door Color",
                ConfigElement(
                    "dungeonMap.entranceDoorColor",
                    ElementType.ColorPicker(Color(0, 204, 0, 255))
                )
            )

        ConfigManager
            .addFeature(
                "Map Display",
                "Configure map appearance",
                "Map",
                ConfigElement(
                    "dungeonMap.mapDisplay",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Map Info Under",
                ConfigElement(
                    "dungeonMap.mapInfoUnder",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Map Border",
                ConfigElement(
                    "dungeonMap.mapBorder",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Map Border Width",
                ConfigElement(
                    "dungeonMap.mapBorderWidth",
                    ElementType.Slider(1.0, 5.0, 2.0, false)
                )
            )
            .addFeatureOption(
                "Map Border Color",
                ConfigElement(
                    "dungeonMap.mapBorderColor",
                    ElementType.ColorPicker(Color(0, 0, 0, 255))
                )
            )
            .addFeatureOption(
                "Map Background Color",
                ConfigElement(
                    "dungeonMap.mapBackgroundColor",
                    ElementType.ColorPicker(Color(0, 0, 0, 100))
                )
            )
            .addFeatureOption(
                "Boss Map Enabled",
                ConfigElement(
                    "dungeonMap.bossMapEnabled",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Score Map Enabled",
                ConfigElement(
                    "dungeonMap.scoreMapEnabled",
                    ElementType.Switch(true)
                )
            )
    }

    override fun initialize() {
        HudManager.registerCustom(NAME, 148, 148, { MapRenderer.renderPreview(it, 0f, 0f) }, "dungeonMap")
        register<GuiEvent.RenderHUD> { event -> renderMap(event.context) }
    }

    fun renderMap(context: DrawContext) {
        val x = HudManager.getX(NAME)
        val y = HudManager.getY(NAME)
        val scale = HudManager.getScale(NAME)

        if (DungeonAPI.inBoss && bossMapEnabled) {
            MapRenderer.renderBoss(context, x, y, scale)
        } else {
            MapRenderer.render(context, x, y, scale)
        }
    }
}