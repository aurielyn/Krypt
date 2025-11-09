package xyz.meowing.krypt.features.map

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.MapIdComponent
import net.minecraft.item.FilledMapItem
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket
import net.minecraft.util.Identifier
import net.minecraft.util.math.RotationAxis
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.core.handlers.DungeonScanner
import xyz.meowing.krypt.api.dungeons.core.handlers.HotbarMapColorParser
import xyz.meowing.krypt.api.dungeons.core.handlers.MapUpdater
import xyz.meowing.krypt.api.dungeons.core.map.Door
import xyz.meowing.krypt.api.dungeons.core.map.Room
import xyz.meowing.krypt.api.dungeons.core.map.RoomState
import xyz.meowing.krypt.api.dungeons.core.map.Unknown
import xyz.meowing.krypt.api.dungeons.core.utils.MapUtils
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.utils.Render2D
import xyz.meowing.krypt.utils.Render2D.pushPop
import java.awt.Color
import java.util.UUID

@Module
object DungeonMapRenderer {
    private const val MAP_SIZE = 128
    private val CheckMarkGreen = Identifier.of("krypt", "krypt/clear/green_check")
    private val CheckMarkWhite = Identifier.of("krypt", "krypt/clear/white_check")
    private val CheckMarkCross = Identifier.of("krypt", "krypt/clear/failed_room")
    private val CheckMarkQuestion = Identifier.of("krypt", "krypt/clear/question_mark")
    private val MarkerSelf = Identifier.of("krypt", "krypt/marker_self")
    private val MarkerOther = Identifier.of("krypt", "krypt/marker_other")

    init {
        EventBus.registerIn<GuiEvent.RenderHUD>(SkyBlockIsland.THE_CATACOMBS) { event ->
            render(event.context, 10f, 10f, 1f)
        }

        EventBus.registerIn<TickEvent.Client>(SkyBlockIsland.THE_CATACOMBS) {
            DungeonScanner.scan()
        }

        EventBus.registerIn<PacketEvent.Received>(SkyBlockIsland.THE_CATACOMBS) { event ->
            val packet = event.packet as? MapUpdateS2CPacket ?: return@registerIn
            val player = client.player ?: return@registerIn
            val world = client.world ?: return@registerIn

            val mapItem = player.inventory.getStack(8)
            if (mapItem.item !is FilledMapItem && !mapItem.name.string.contains("Magical Map", true)) return@registerIn

            val mapId = mapItem.get(DataComponentTypes.MAP_ID) ?: MapIdComponent(packet.mapId().id)
            val mapState = FilledMapItem.getMapState(mapId, world) ?: return@registerIn

            client.execute {
                packet.apply(mapState)
                DungeonAPI.mapData = mapState

                if (!MapUtils.calibrated) {
                    MapUtils.calibrated = MapUtils.calibrateMap()
                } else {
                    MapUpdater.updateRooms(mapState)
                    MapUpdater.updatePlayers(mapState)
                }
            }
        }
    }

    fun render(context: DrawContext, x: Float, y: Float, scale: Float) {
        val matrix = context.matrices

        matrix.push()
        matrix.translate(x, y, 0f)
        matrix.scale(scale, scale, 1f)

        drawMapBackground(context)
        renderRooms(context)
        renderCheckmarks(context)
        renderPlayerHeads(context)

        matrix.pop()
    }

    private fun drawMapBackground(context: DrawContext) {
        context.fill(
            RenderLayer.getGui(),
            0, 0,
            MAP_SIZE, MAP_SIZE,
            DungeonMap.mapBackgroundColor.rgb
        )

        if (!DungeonMap.mapBorder) return

        val borderWidth = DungeonMap.mapBorderWidth
        val color = DungeonMap.mapBorderColor.rgb

        context.fill(RenderLayer.getGui(), -borderWidth, -borderWidth, MAP_SIZE + borderWidth, 0, color)
        context.fill(RenderLayer.getGui(), -borderWidth, MAP_SIZE, MAP_SIZE + borderWidth, MAP_SIZE + borderWidth, color)
        context.fill(RenderLayer.getGui(), -borderWidth, 0, 0, MAP_SIZE, color)
        context.fill(RenderLayer.getGui(), MAP_SIZE, 0, MAP_SIZE + borderWidth, MAP_SIZE, color)
    }

    private fun renderRooms(context: DrawContext) {
        val matrix = context.matrices

        matrix.push()
        matrix.translate(MapUtils.startCorner.first.toFloat(), MapUtils.startCorner.second.toFloat(), 0f)

        val connectorSize = HotbarMapColorParser.quarterRoom

        for (y in 0..10) {
            for (x in 0..10) {
                val tile = DungeonAPI.dungeonList[y * 11 + x]
                if (tile is Unknown || tile.state == RoomState.UNDISCOVERED) continue

                val color = tile.color
                val xEven = x and 1 == 0
                val yEven = y and 1 == 0

                val xOffset = (x shr 1) * (MapUtils.mapRoomSize + connectorSize)
                val yOffset = (y shr 1) * (MapUtils.mapRoomSize + connectorSize)

                when {
                    xEven && yEven -> {
                        drawRect(context, xOffset, yOffset, MapUtils.mapRoomSize, MapUtils.mapRoomSize, color)
                    }

                    !xEven && !yEven -> {
                        drawRect(context, xOffset, yOffset, MapUtils.mapRoomSize + connectorSize, MapUtils.mapRoomSize + connectorSize, color)
                    }

                    else -> {
                        drawRoomConnector(context, xOffset, yOffset, connectorSize, tile is Door, !xEven, color)
                    }
                }
            }
        }

        matrix.pop()
    }

    private fun renderCheckmarks(context: DrawContext) {
        val matrix = context.matrices

        matrix.push()
        matrix.translate(MapUtils.startCorner.first.toFloat(), MapUtils.startCorner.second.toFloat(), 0f)

        for (y in 0..10 step 2) {
            for (x in 0..10 step 2) {
                val room = DungeonAPI.dungeonList[y * 11 + x] as? Room ?: continue
                if (room.isSeparator || room.state == RoomState.UNDISCOVERED) continue

                val checkmarkSize = 10.0
                val size = MapUtils.mapRoomSize + HotbarMapColorParser.quarterRoom
                val xOffset = (x / 2f) * size + (MapUtils.mapRoomSize - checkmarkSize) / 2
                val yOffset = (y / 2f) * size + (MapUtils.mapRoomSize - checkmarkSize) / 2

                drawCheckmark(context, room, xOffset.toFloat(), yOffset.toFloat(), checkmarkSize)
            }
        }

        matrix.pop()
    }

    private fun renderPlayerHeads(context: DrawContext) {
        val matrix = context.matrices
        context.pushPop {
            matrix.translate(MapUtils.startCorner.first.toFloat(), MapUtils.startCorner.second.toFloat(), 0f)

            DungeonAPI.teammates.filterNot { it.dead }.forEach { player ->
                val entity = player.entity

                if (entity != null) {
                    val pos = entity.lastRenderPos
                    val (x, z) = MapUtils.coordsToMap(pos)
                    val yaw = entity.yaw + 180f
                    drawPlayerHead(
                        context,
                        player.entity?.uuid,
                        x,
                        z,
                        yaw,
                        player.name == client.player?.name?.stripped
                    )
                } else {
                    val mapX = player.mapIcon.mapX - 10 // weird offset?
                    val mapZ = player.mapIcon.mapZ - 10 // weird offset?
                    val yaw = player.mapIcon.yaw
                    if (mapX == 0f && mapZ == 0f) return@forEach

                    drawPlayerHead(
                        context,
                        player.entity?.uuid,
                        mapX,
                        mapZ,
                        yaw,
                        player.name == client.player?.name?.stripped
                    )
                }
            }
        }
    }

    private fun drawPlayerHead(context: DrawContext, uuid: UUID?, x: Float, z: Float, yaw: Float, isSelf: Boolean) {
        val matrix = context.matrices

        context.pushPop {
            matrix.translate(x, z, 0f)
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw))

            if (DungeonMap.showPlayerHead) {
                val borderColor = DungeonMap.playerIconBorderColor
                val borderSize = DungeonMap.playerIconBorderSize.toFloat()

                Render2D.drawRect(context, -6, -6, 12, 12, borderColor)

                matrix.scale(1f - borderSize, 1f - borderSize, 1f)
                Render2D.drawPlayerHead(context, -6, -6, 12, uuid ?: UUID(0, 0))
            } else {
                val marker = if (isSelf) MarkerSelf else MarkerOther
                Render2D.drawImage(context, marker, -4, -5, 7, 10)
            }
        }
    }

    private fun getCheckmark(state: RoomState) = when (state) {
        RoomState.CLEARED -> CheckMarkWhite
        RoomState.GREEN -> CheckMarkGreen
        RoomState.FAILED -> CheckMarkCross
        RoomState.UNOPENED -> CheckMarkQuestion
        else -> null
    }

    private fun drawCheckmark(context: DrawContext, room: Room, xOffset: Float, yOffset: Float, checkmarkSize: Double) {
        getCheckmark(room.state)?.let { texture ->
            context.drawGuiTexture(
                RenderLayer::getGuiTextured,
                texture,
                xOffset.toInt(),
                yOffset.toInt(),
                checkmarkSize.toInt(),
                checkmarkSize.toInt()
            )
        }
    }

    private fun drawRoomConnector(
        context: DrawContext,
        x: Int,
        y: Int,
        doorWidth: Int,
        doorway: Boolean,
        vertical: Boolean,
        color: Color
    ) {
        val doorwayOffset = if (MapUtils.mapRoomSize == 16) 5 else 6

        if (doorway) {
            val width = 6
            val x1 = if (vertical) x + MapUtils.mapRoomSize else x
            val y1 = if (vertical) y else y + MapUtils.mapRoomSize
            val xFinal = if (vertical) x1 else x1 + doorwayOffset
            val yFinal = if (vertical) y1 + doorwayOffset else y1

            drawRect(
                context,
                xFinal, yFinal,
                if (vertical) doorWidth else width,
                if (vertical) width else doorWidth,
                color
            )
        } else {
            val x1 = if (vertical) x + MapUtils.mapRoomSize else x
            val y1 = if (vertical) y else y + MapUtils.mapRoomSize

            drawRect(
                context,
                x1, y1,
                if (vertical) doorWidth else MapUtils.mapRoomSize,
                if (vertical) MapUtils.mapRoomSize else doorWidth,
                color
            )
        }
    }

    private fun drawRect(context: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Color) {
        context.fill(
            RenderLayer.getGui(),
            x, y,
            x + width, y + height,
            color.rgb
        )
    }
}