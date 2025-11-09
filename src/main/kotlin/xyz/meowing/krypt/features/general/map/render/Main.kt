package xyz.meowing.krypt.features.general.map.render

import net.minecraft.client.gui.DrawContext
import net.minecraft.util.math.RotationAxis
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.DungeonClass
import xyz.meowing.krypt.api.dungeons.enums.map.Room
import xyz.meowing.krypt.api.dungeons.enums.DungeonPlayer
import xyz.meowing.krypt.api.dungeons.enums.map.Checkmark
import xyz.meowing.krypt.api.dungeons.enums.map.DoorState
import xyz.meowing.krypt.api.dungeons.enums.map.DoorType
import xyz.meowing.krypt.api.dungeons.enums.map.RoomType
import xyz.meowing.krypt.features.general.map.DungeonMap
import xyz.meowing.krypt.features.general.map.utils.Utils
import xyz.meowing.krypt.utils.Render2D
import xyz.meowing.krypt.utils.Render2D.pushPop
import xyz.meowing.krypt.utils.Render2D.width
import xyz.meowing.krypt.utils.StringUtils.removeFormatting
import java.awt.Color
import java.util.UUID

object Main {
    private const val ROOM_SIZE = 18
    private const val GAP_SIZE = 4
    private const val SPACING = ROOM_SIZE + GAP_SIZE

    fun renderMap(context: DrawContext) {
        val mapOffset = if (DungeonAPI.floor?.floorNumber == 1) 10.6f else 0f
        val mapScale = Utils.scale(DungeonAPI.floor?.floorNumber)

        context.pushPop {
            translateAndScale(context, 5f, 5f, mapScale, mapOffset, 0f)

            renderRooms(context)
            if (!DungeonMap.playerHeadsUnder) renderPlayers(context)
            renderCheckmarks(context)
            renderRoomLabels(context)
            if (DungeonMap.playerHeadsUnder) renderPlayers(context)
        }
    }

    private fun renderRooms(context: DrawContext) {
        DungeonAPI.discoveredRooms.values.forEach { room ->
            Render2D.drawRect(context, room.x * SPACING, room.z * SPACING, ROOM_SIZE, ROOM_SIZE, Color(65 / 255f, 65 / 255f, 65 / 255f, 1f))
        }

        DungeonAPI.uniqueRooms.forEach { room ->
            if (!room.explored) return@forEach
            val color = room.type.color

            room.components.forEach { (x, z) ->
                Render2D.drawRect(context, x * SPACING, z * SPACING, ROOM_SIZE, ROOM_SIZE, color)
            }

            renderRoomConnectors(context, room)
        }

        DungeonAPI.uniqueDoors.forEach { door ->
            if (door.state != DoorState.DISCOVERED) return@forEach
            val color = door.color
            val (cx, cy) = door.componentPos.let { it.first / 2 * SPACING to it.second / 2 * SPACING }
            val vert = door.rotation == 0
            val (w, h) = if (vert) 6 to 4 else 4 to 6
            val (x, y) = if (vert) cx + 6 to cy + 18 else cx + 18 to cy + 6
            Render2D.drawRect(context, x, y, w, h, color)
        }
    }

    private fun renderCheckmarks(context: DrawContext) {
        DungeonAPI.discoveredRooms.values.forEach { room ->
            val x = room.x * SPACING + ROOM_SIZE / 2 - 5
            val y = room.z * SPACING + ROOM_SIZE / 2 - 6
            context.pushPop {
                translateAndScale(context, x.toFloat(), y.toFloat(), DungeonMap.checkmarkScale.toFloat())
                Render2D.drawImage(context, Checkmark.questionMark, 0, 0, 10, 12)
            }
        }

        DungeonAPI.uniqueRooms.forEach { room ->
            if (!room.explored) return@forEach

            val (centerX, centerZ) = room.center()
            val x = (centerX * SPACING).toInt() + ROOM_SIZE / 2 - 6
            val y = (centerZ * SPACING).toInt() + ROOM_SIZE / 2 - 6

            val showCleared = DungeonMap.showClearedRoomCheckmarks && room.checkmark != Checkmark.UNEXPLORED

            val checkmark = when {
                showCleared && room.type == RoomType.PUZZLE -> room.checkmark.image

                showCleared && room.checkmark in listOf(Checkmark.GREEN, Checkmark.WHITE) -> {
                    if (room.secretsFound == room.secrets) Checkmark.greenCheck else Checkmark.whiteCheck
                }

                room.type in listOf(RoomType.ENTRANCE, RoomType.PUZZLE) -> null

                room.type == RoomType.NORMAL && room.secrets != 0 -> null

                else -> room.checkmark.image
            } ?: return@forEach

            val scale = if (showCleared) DungeonMap.clearedRoomCheckmarkScale.toFloat() else DungeonMap.checkmarkScale.toFloat()

            context.pushPop {
                translateAndScale(context, x.toFloat(), y.toFloat(), scale)
                Render2D.drawImage(context, checkmark, 0, 0, 12, 12)
            }
        }
    }

    private fun renderRoomLabels(context: DrawContext) {
        DungeonAPI.uniqueRooms.forEach { room ->
            if (!room.explored) return@forEach

            val checkmarkMode = when (room.type) {
                RoomType.PUZZLE -> DungeonMap.puzzleCheckmarkMode
                RoomType.NORMAL -> DungeonMap.normalCheckmarkMode
                else -> return@forEach
            }

            if (checkmarkMode < 1) return@forEach

            val secrets = if (room.checkmark == Checkmark.GREEN) room.secrets else room.secretsFound
            val roomNameColor = DungeonMap.roomNameColor.code
            val secretsColor = DungeonMap.secretsColor.code
            val roomText = room.name ?: "???"
            val secretText = "$secrets/${room.secrets}"

            val lines = buildList {
                if (checkmarkMode in listOf(1, 3)) addAll(roomText.split(" ").map { roomNameColor + it })
                if (checkmarkMode in listOf(2, 3) && room.secrets != 0) add(secretsColor + secretText)
            }

            val (centerX, centerZ) = room.center()
            val scale = (0.75f * DungeonMap.roomLabelScale).toFloat()

            context.pushPop {
                translateAndScale(context, (centerX * SPACING).toFloat() + ROOM_SIZE / 2, (centerZ * SPACING).toFloat() + ROOM_SIZE / 2, scale)

                lines.forEachIndexed { i, line ->
                    val drawX = (-line.width() / 2).toFloat()
                    val drawY = (9 * i - (lines.size * 9) / 2).toFloat()
                    if (DungeonMap.coolText) drawShadowedText(context, line.removeFormatting(), drawX.toInt(), drawY.toInt(), 1f)
                    Render2D.renderString(context, line, drawX, drawY, 1f)
                }
            }
        }
    }

    private fun renderPlayers(context: DrawContext) {
        for (player in DungeonAPI.players) {
            if (player == null || (player.dead && player.name != KnitPlayer.name)) continue

            val iconX = player.iconX ?: continue
            val iconY = player.iconZ ?: continue
            val rotation = player.yaw ?: continue

            val x = iconX / 125.0 * 128.0
            val y = iconY / 125.0 * 128.0
            val ownName = !DungeonMap.showOwnPlayer && player.name == KnitPlayer.name

            if (DungeonAPI.holdingLeaps && DungeonMap.showPlayerNametags && !ownName) {
                context.pushPop {
                    translateAndScale(context, x.toFloat(), y.toFloat(), 1f)
                    Utils.renderNametag(context, player.name, 1f / 1.3f)
                }
            }

            renderPlayerIcon(context, player, x, y, rotation)
        }
    }

    private fun renderRoomConnectors(context: DrawContext, room: Room) {
        val directions = listOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))

        for ((x, z) in room.components) {
            for ((dx, dz) in directions) {
                val nx = x + dx
                val nz = z + dz
                if (!room.hasComponent(nx, nz)) continue

                val cx = (x + nx) / 2 * SPACING
                val cy = (z + nz) / 2 * SPACING
                val isVertical = dx == 0
                val w = if (isVertical) ROOM_SIZE else GAP_SIZE
                val h = if (isVertical) GAP_SIZE else ROOM_SIZE
                val drawX = if (isVertical) cx else cx + ROOM_SIZE
                val drawY = if (isVertical) cy + ROOM_SIZE else cy

                Render2D.drawRect(context, drawX, drawY, w, h, room.type.color)
            }
        }

        if (room.components.size == 4 && room.shape == "2x2") {
            val x = room.components[0].first * SPACING + ROOM_SIZE
            val y = room.components[0].second * SPACING + ROOM_SIZE
            Render2D.drawRect(context, x, y, GAP_SIZE, GAP_SIZE, room.type.color)
        }
    }

    private fun Room.center(): Pair<Double, Double> {
        val minX = components.minOf { it.first }
        val minZ = components.minOf { it.second }
        val maxX = components.maxOf { it.first }
        val maxZ = components.maxOf { it.second }

        val width = maxX - minX
        val height = maxZ - minZ

        var centerZ = minZ + height / 2.0
        if (shape == "L") {
            val topEdgeCount = components.count { it.second == minZ }
            centerZ += if (topEdgeCount == 2) -height / 2.0 else height / 2.0
        }

        return Pair(minX + width / 2.0, centerZ)
    }

    private fun drawShadowedText(context: DrawContext, text: String, x: Int, y: Int, scale: Float) {
        val offsets = listOf(Pair(scale, 0f), Pair(-scale, 0f), Pair(0f, scale), Pair(0f, -scale))

        for ((dx, dy) in offsets) {
            context.pushPop {
                translateAndScale(context, dx, dy, 1f)
                Render2D.renderString(context, "§0$text", x.toFloat(), y.toFloat(), 1f)
            }
        }
    }

    private fun renderPlayerIcon(context: DrawContext, player: DungeonPlayer, x: Double, y: Double, rotation: Float) {
        context.pushPop {
            val matrix = context.matrices

            //#if MC >= 1.21.7
            //$$ matrix.translate(x.toFloat(), y.toFloat())
            //$$ matrix.rotate((rotation * (kotlin.math.PI / 180)).toFloat())
            //$$ matrix.scale(1f, 1f)
            //#else
            matrix.translate(x.toFloat(), y.toFloat(), 0f)
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation))
            matrix.scale(1f, 1f, 1f)
            //#endif

            if (DungeonMap.showPlayerHead) {
                val borderColor = if (DungeonMap.iconClassColors) player.dungeonClass?.color else DungeonMap.playerIconBorderColor

                Render2D.drawRect(context, -6, -6, 12, 12, borderColor ?: DungeonClass.defaultColor)

                val borderSize = DungeonMap.playerIconBorderSize.toFloat()
                //#if MC >= 1.21.7
                //$$ matrix.scale(1f - borderSize, 1f - borderSize)
                //#else
                matrix.scale(1f - borderSize, 1f - borderSize, 1f)
                //#endif
                Render2D.drawPlayerHead(context, -6, -6, 12, player.uuid ?: UUID(0, 0))
            } else {
                val head = if (player.name == KnitPlayer.name) Utils.markerSelf else Utils.markerOther
                Render2D.drawImage(context, head, -4, -5, 7, 10)
            }
        }
    }

    private fun translateAndScale(context: DrawContext, x: Float, y: Float, scale: Float, offsetX: Float = 0f, offsetY: Float = 0f) {
        val matrix = context.matrices

        //#if MC >= 1.21.7
        //$$ matrix.translate(x, y)
        //$$ if (offsetX != 0f || offsetY != 0f) matrix.translate(offsetX, offsetY)
        //$$ if (scale != 1f) matrix.scale(scale, scale)
        //#else
        matrix.translate(x, y, 0f)
        if (offsetX != 0f || offsetY != 0f) matrix.translate(offsetX, offsetY, 0f)
        if (scale != 1f) matrix.scale(scale, scale, 1f)
        //#endif
    }
}