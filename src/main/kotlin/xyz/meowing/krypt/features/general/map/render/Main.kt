package xyz.meowing.krypt.features.general.map.render

import net.minecraft.client.gui.DrawContext
import net.minecraft.util.math.RotationAxis
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.map.Room
import xyz.meowing.krypt.api.dungeons.players.DungeonPlayer
import xyz.meowing.krypt.api.dungeons.utils.Checkmark
import xyz.meowing.krypt.api.dungeons.utils.DoorState
import xyz.meowing.krypt.api.dungeons.utils.DoorType
import xyz.meowing.krypt.api.dungeons.utils.RoomType
import xyz.meowing.krypt.features.general.map.DungeonMap
import xyz.meowing.krypt.features.general.map.utils.Utils
import xyz.meowing.krypt.utils.Render2D
import xyz.meowing.krypt.utils.Render2D.pushPop
import xyz.meowing.krypt.utils.Render2D.width
import java.awt.Color
import java.util.UUID

object Main {
    private const val ROOM_SIZE = 18
    private const val GAP_SIZE = 4
    private const val SPACING = ROOM_SIZE + GAP_SIZE

    fun renderMap(context: DrawContext) {
        val matrix = context.matrices
        val mapOffset = if (DungeonAPI.floor?.floorNumber == 1) 10.6f else 0f
        val mapScale = Utils.scale(DungeonAPI.floor?.floorNumber)

        context.pushPop {
            //#if MC >= 1.21.7
            //$ matrix.translate(5f, 5f)
            //$ matrix.translate(mapOffset, 0f)
            //$ matrix.scale(mapScale, mapScale)
            //#else
            matrix.translate(5f, 5f, 0f)
            matrix.translate(mapOffset, 0f, 0f)
            matrix.scale(mapScale, mapScale, 1f)
            //#endif

            renderRooms(context)
            renderCheckmarks(context)
            renderClearedRoomCheckmarks(context)
            renderPuzzleCheckmarks(context)
            renderRoomLabels(context, RoomType.PUZZLE, DungeonMap.puzzleCheckmarkMode)
            renderRoomLabels(context, RoomType.NORMAL, DungeonMap.normalCheckmarkMode)
            renderPlayers(context)
        }
    }

    fun renderRooms(context: DrawContext) {
        DungeonAPI.discoveredRooms.values.forEach { room ->
            Render2D.drawRect(
                context,
                room.x * SPACING,
                room.z * SPACING,
                ROOM_SIZE,
                ROOM_SIZE,
                Color(65 / 255f, 65 / 255f, 65 / 255f, 1f)
            )
        }

        DungeonAPI.uniqueRooms.forEach { room ->
            if (!room.explored) return@forEach
            val color = Utils.roomTypeColors[room.type] ?: return@forEach

            room.components.forEach { (x, z) ->
                Render2D.drawRect(context, x * SPACING, z * SPACING, ROOM_SIZE, ROOM_SIZE, color)
            }

            renderRoomConnectors(context, room)
        }

        DungeonAPI.uniqueDoors.forEach { door ->
            if (door.state != DoorState.DISCOVERED) return@forEach
            val type = if (door.opened) DoorType.NORMAL else door.type
            val color = Utils.doorTypeColors[type] ?: return@forEach
            val (cx, cy) = door.getComp().let { it.first / 2 * SPACING to it.second / 2 * SPACING }
            val vert = door.rotation == 0
            val (w, h) = if (vert) 6 to 4 else 4 to 6
            val (x, y) = if (vert) cx + 6 to cy + 18 else cx + 18 to cy + 6
            Render2D.drawRect(context, x, y, w, h, color)
        }
    }

    fun renderCheckmarks(context: DrawContext) {
        val scale = DungeonMap.checkmarkScale.toFloat()

        DungeonAPI.discoveredRooms.values.forEach { room ->
            val x = room.x * SPACING + ROOM_SIZE / 2 - 5
            val y = room.z * SPACING + ROOM_SIZE / 2 - 6

            context.pushPop {
                //#if MC >= 1.21.7
                //$$ context.matrices.translate(x.toFloat(), y.toFloat())
                //$$ context.matrices.scale(scale, scale)
                //#else
                context.matrices.translate(x.toFloat(), y.toFloat(), 0f)
                context.matrices.scale(scale, scale, 1f)
                //#endif
                Render2D.drawImage(context, Utils.questionMark, 0, 0, 10, 12)
            }
        }

        DungeonAPI.uniqueRooms.forEach { room ->
            if (!room.explored) return@forEach
            val checkmark = Utils.getCheckmarks(room.checkmark) ?: return@forEach
            if (room.type in setOf(RoomType.NORMAL, RoomType.RARE) && room.secrets != 0) return@forEach
            if (room.type == RoomType.PUZZLE || room.type == RoomType.ENTRANCE) return@forEach

            val (centerX, centerZ) = room.center()
            val x = (centerX * SPACING).toInt() + ROOM_SIZE / 2 - 6
            val y = (centerZ * SPACING).toInt() + ROOM_SIZE / 2 - 6

            context.pushPop {
                //#if MC >= 1.21.7
                //$$ context.matrices.translate(x.toFloat(), y.toFloat())
                //$$ context.matrices.scale(scale, scale)
                //#else
                context.matrices.translate(x.toFloat(), y.toFloat(), 0f)
                context.matrices.scale(scale, scale, 1f)
                //#endif
                Render2D.drawImage(context, checkmark, 0, 0, 12, 12)
            }
        }
    }

    fun renderClearedRoomCheckmarks(context: DrawContext) {
        if (!DungeonMap.showClearedRoomCheckmarks) return

        val scale = DungeonMap.clearedRoomCheckmarkScale.toFloat()

        DungeonAPI.uniqueRooms.forEach { room ->
            if (!room.explored || room.checkmark == Checkmark.UNEXPLORED) return@forEach

            val isClear = room.checkmark == Checkmark.GREEN || room.checkmark == Checkmark.WHITE
            if (!isClear) return@forEach

            val checkmark = if (room.secretsFound == room.secrets) Utils.greenCheck else Utils.whiteCheck

            val (centerX, centerZ) = room.center()
            val x = (centerX * SPACING).toInt() + ROOM_SIZE / 2 - 6
            val y = (centerZ * SPACING).toInt() + ROOM_SIZE / 2 - 6

            context.pushPop {
                //#if MC >= 1.21.7
                //$ context.matrices.translate(x.toFloat(), y.toFloat())
                //$ context.matrices.scale(scale, scale)
                //#else
                context.matrices.translate(x.toFloat(), y.toFloat(), 0f)
                context.matrices.scale(scale, scale, 1f)
                //#endif
                Render2D.drawImage(context, checkmark, 0, 0, 12, 12)
            }
        }
    }

    fun renderPuzzleCheckmarks(context: DrawContext) {
        if (!DungeonMap.showClearedRoomCheckmarks) return

        val scale = DungeonMap.clearedRoomCheckmarkScale.toFloat()

        DungeonAPI.uniqueRooms.forEach { room ->
            if (!room.explored || room.type != RoomType.PUZZLE) return@forEach

            val checkmark = when (room.checkmark) {
                Checkmark.GREEN -> Utils.greenCheck
                Checkmark.WHITE -> Utils.whiteCheck
                Checkmark.FAILED -> Utils.failedRoom
                else -> return@forEach
            }

            val (centerX, centerZ) = room.center()
            val x = (centerX * SPACING).toInt() + ROOM_SIZE / 2 - 6
            val y = (centerZ * SPACING).toInt() + ROOM_SIZE / 2 - 6

            context.pushPop {
                //#if MC >= 1.21.7
                //$ context.matrices.translate(x.toFloat(), y.toFloat())
                //$ context.matrices.scale(scale, scale)
                //#else
                context.matrices.translate(x.toFloat(), y.toFloat(), 0f)
                context.matrices.scale(scale, scale, 1f)
                //#endif
                Render2D.drawImage(context, checkmark, 0, 0, 12, 12)
            }
        }
    }

    fun renderRoomLabels(context: DrawContext, type: RoomType, checkmarkMode: Int) {
        DungeonAPI.uniqueRooms.forEach { room ->
            if (!room.explored || room.type != type || checkmarkMode < 1) return@forEach

            val secrets = if (room.checkmark == Checkmark.GREEN) room.secrets else room.secretsFound
            val textColor = Utils.getTextColor(room.checkmark)
            val roomText = room.name ?: "???"
            val secretText = "${DungeonMap.secretsColor.code}$secrets/${room.secrets}"

            val lines = buildList {
                if (checkmarkMode in listOf(1, 3)) addAll(roomText.split(" ").map { "${DungeonMap.roomNameColor.code}$it" })
                if (checkmarkMode in listOf(2, 3) && room.secrets != 0) add(secretText)
            }

            val (centerX, centerZ) = room.center()
            val x = (centerX * SPACING).toInt() + ROOM_SIZE / 2
            val y = (centerZ * SPACING).toInt() + ROOM_SIZE / 2
            val scale = (0.75f * DungeonMap.roomLabelScale).toFloat()

            context.pushPop {
                //#if MC >= 1.21.7
                //$ context.matrices.translate(x.toFloat(), y.toFloat())
                //$ context.matrices.scale(scale, scale)
                //#else
                context.matrices.translate(x.toFloat(), y.toFloat(), 0f)
                context.matrices.scale(scale, scale, 1f)
                //#endif

                lines.forEachIndexed { i, line ->
                    val drawX = (-line.width() / 2).toFloat()
                    val drawY = (9 * i - (lines.size * 9) / 2).toFloat()
                    Render2D.renderStringWithShadow(context, textColor + line, drawX, drawY, 1f)
                }
            }
        }
    }

    fun renderPlayers(context: DrawContext) {
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
                    //#if MC >= 1.21.7
                    //$$ context.matrices.translate(x.toFloat(), y.toFloat())
                    //#else
                    context.matrices.translate(x.toFloat(), y.toFloat(), 0f)
                    //#endif
                    Utils.renderNametag(context, player.name, 1f / 1.3f)
                }
            }

            renderPlayerIcon(context, player, x, y, rotation)
        }
    }

    fun renderRoomConnectors(context: DrawContext, room: Room) {
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

                Render2D.drawRect(context, drawX, drawY, w, h, Utils.roomTypeColors[room.type] ?: Color.GRAY)
            }
        }

        if (room.components.size == 4 && room.shape == "2x2") {
            val x = room.components[0].first * SPACING + ROOM_SIZE
            val y = room.components[0].second * SPACING + ROOM_SIZE
            Render2D.drawRect(context, x, y, GAP_SIZE, GAP_SIZE, Utils.roomTypeColors[room.type] ?: Color.GRAY)
        }
    }

    fun Room.center(): Pair<Double, Double> {
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

    fun drawShadowedText(context: DrawContext, text: String, x: Int, y: Int, scale: Float) {
        val offsets = listOf(Pair(scale, 0f), Pair(-scale, 0f), Pair(0f, scale), Pair(0f, -scale))
        for ((dx, dy) in offsets) {
            context.pushPop {
                //#if MC >= 1.21.7
                //$$ context.matrices.translate(dx, dy)
                //#else
                context.matrices.translate(dx, dy, 0f)
                //#endif
                Render2D.renderString(context, "§0$text", x.toFloat(), y.toFloat(), 1f)
            }
        }
    }

    fun renderPlayerIcon(context: DrawContext, player: DungeonPlayer, x: Double, y: Double, rotation: Float) {
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
                val borderColor = if (DungeonMap.iconClassColors) Utils.getClassColor(player.dungeonClass?.displayName) else DungeonMap.playerIconBorderColor

                Render2D.drawRect(context, -6, -6, 12, 12, borderColor)

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
}