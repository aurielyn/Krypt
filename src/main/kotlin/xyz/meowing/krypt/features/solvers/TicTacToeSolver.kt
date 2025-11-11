package xyz.meowing.krypt.features.solvers

import net.minecraft.block.Blocks
import net.minecraft.block.ShapeContext
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.item.FilledMapItem
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.EmptyBlockView
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.utils.WorldScanUtils
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.WorldUtils
import xyz.meowing.krypt.utils.rendering.Render3D
import java.awt.Color
import kotlin.experimental.and
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Contains modified code from Noamm's tic-tac-toe solver.
 *
 * Original File: [GitHub](https://github.com/Noamm9/NoammAddons/blob/master/src/main/kotlin/noammaddons/features/impl/dungeons/solvers/puzzles/TicTacToeSolver.kt)
 */
@Module
object TicTacToeSolver : Feature(
    "ticTacToeSolver",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private var inTicTacToe = false
    private var bestMove: Box? = null
    private var roomCenter: Pair<Int, Int>? = null

    private val boxColor by ConfigDelegate<Color>("ticTacToeSolver.boxColor")

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Tic Tac Toe solver",
                "Shows the best move using minimax algorithm",
                "Solvers",
                ConfigElement(
                    "ticTacToeSolver",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Box color",
                ConfigElement(
                    "ticTacToeSolver.boxColor",
                    ElementType.ColorPicker(Color(0, 255, 0, 127))
                )
            )
    }

    override fun initialize() {
        register<DungeonEvent.Room.Change> { event ->
            if (event.new.name != "Tic Tac Toe") return@register

            inTicTacToe = true
            KnitPlayer.player?.let { p ->
                roomCenter = WorldScanUtils.getRoomCenter(p.x.toInt(), p.z.toInt())
            }

            TickScheduler.Server.schedule(2) {
                scanBoard()
            }
        }

        register<DungeonEvent.Room.Change> { event ->
            if (inTicTacToe && event.new.name != "Tic Tac Toe") reset()
        }

        register<LocationEvent.WorldChange> { reset() }

        register<PacketEvent.Received> { event ->
            if (!inTicTacToe) return@register
            val packet = event.packet as? EntitySpawnS2CPacket ?: return@register
            if (packet.entityType != EntityType.ITEM_FRAME) return@register

            TickScheduler.Server.schedule(5) {
                scanBoard()
            }
        }

        register<RenderEvent.World.Last> { event ->
            bestMove?.let { box ->
                Render3D.drawSpecialBB(
                    box,
                    boxColor,
                    event.context.consumers(),
                    event.context.matrixStack(),
                    phase = true
                )
            }
        }
    }

    private fun scanBoard() {
        val center = roomCenter ?: return
        val world = KnitClient.world ?: return

        val aabb = Box(
            center.first - 9.0, 65.0, center.second - 9.0,
            center.first + 9.0, 73.0, center.second + 9.0
        )

        val itemFrames = world.getEntitiesByClass(ItemFrameEntity::class.java, aabb) { true }
        val itemFramesWithMaps = itemFrames.filter { frame ->
            val item = frame.heldItemStack ?: return@filter false
            if (item.item !is FilledMapItem) return@filter false
            FilledMapItem.getMapState(item, world) != null
        }

        if (itemFramesWithMaps.size == 8) {
            reset()
            return
        }

        if (itemFramesWithMaps.size % 2 == 0) return

        val board = Array(3) { CharArray(3) }
        var leftmostRow: BlockPos? = null
        var sign = 1
        var facing = 'X'

        for (itemFrame in itemFramesWithMaps) {
            val map = itemFrame.heldItemStack ?: continue
            val mapData = (map.item as? FilledMapItem)?.let { FilledMapItem.getMapState(map, world) } ?: continue

            var row = 0
            sign = if (itemFrame.horizontalFacing.offsetX != 0) {
                if (itemFrame.horizontalFacing.offsetX > 0) 1 else -1
            } else {
                if (itemFrame.horizontalFacing.offsetZ > 0) 1 else -1
            }

            val itemFramePos = BlockPos(
                floor(itemFrame.x).toInt(),
                floor(itemFrame.y).toInt(),
                floor(itemFrame.z).toInt()
            )

            for (i in 2 downTo 0) {
                val realI = i * sign
                var blockPos = itemFramePos

                if (itemFrame.x % 0.5 == 0.0) {
                    blockPos = itemFramePos.add(realI, 0, 0)
                } else if (itemFrame.z % 0.5 == 0.0) {
                    blockPos = itemFramePos.add(0, 0, realI)
                    facing = 'Z'
                }

                val block = WorldUtils.getBlockStateAt(blockPos.x, blockPos.y, blockPos.z)?.block
                if (block == Blocks.STONE_BUTTON || block == Blocks.AIR) {
                    leftmostRow = blockPos
                    row = i
                    break
                }
            }

            val column = when (itemFrame.y) {
                72.5 -> 0
                71.5 -> 1
                70.5 -> 2
                else -> continue
            }

            val colorInt = (mapData.colors[8256] and 255.toByte()).toInt()
            when (colorInt) {
                114 -> board[column][row] = 'X'
                33 -> board[column][row] = 'O'
            }
        }

        val moveIndex = getBestMove(board) - 1
        if (moveIndex < 0) return

        leftmostRow?.let { pos ->
            val drawX = (if (facing == 'X') pos.x - sign * (moveIndex % 3) else pos.x).toDouble()
            val drawY = 72.0 - floor((moveIndex / 3).toDouble())
            val drawZ = (if (facing == 'Z') pos.z - sign * (moveIndex % 3) else pos.z).toDouble()

            bestMove = Box(drawX, drawY, drawZ, drawX + 1, drawY + 1, drawZ + 1)
        }
    }

    private fun getBestMove(board: Array<CharArray>): Int {
        val moves = mutableMapOf<Int, Int>()

        for (row in board.indices) {
            for (col in board[row].indices) {
                if (board[row][col] != '\u0000') continue

                board[row][col] = 'O'
                val score = minimax(board, false, 0)
                board[row][col] = '\u0000'
                moves[row * 3 + col + 1] = score
            }
        }

        return moves.maxByOrNull { it.value }?.key ?: 0
    }

    private fun minimax(board: Array<CharArray>, max: Boolean, depth: Int): Int {
        val score = getBoardRanking(board)
        if (score == 10 || score == -10) return score
        if (!hasMovesLeft(board)) return 0

        if (max) {
            var bestScore = -1000
            for (row in 0..2) {
                for (col in 0..2) {
                    if (board[row][col] == '\u0000') {
                        board[row][col] = 'O'
                        bestScore = max(bestScore, minimax(board, false, depth + 1))
                        board[row][col] = '\u0000'
                    }
                }
            }
            return bestScore - depth
        } else {
            var bestScore = 1000
            for (row in 0..2) {
                for (col in 0..2) {
                    if (board[row][col] == '\u0000') {
                        board[row][col] = 'X'
                        bestScore = min(bestScore, minimax(board, true, depth + 1))
                        board[row][col] = '\u0000'
                    }
                }
            }
            return bestScore + depth
        }
    }

    private fun getBoardRanking(board: Array<CharArray>): Int {
        for (row in 0..2) {
            if (board[row][0] == board[row][1] && board[row][0] == board[row][2]) {
                if (board[row][0] == 'X') return -10
                else if (board[row][0] == 'O') return 10
            }
        }

        for (col in 0..2) {
            if (board[0][col] == board[1][col] && board[0][col] == board[2][col]) {
                if (board[0][col] == 'X') return -10
                else if (board[0][col] == 'O') return 10
            }
        }

        if (board[0][0] == board[1][1] && board[0][0] == board[2][2]) {
            if (board[0][0] == 'X') return -10
            else if (board[0][0] == 'O') return 10
        }

        if (board[0][2] == board[1][1] && board[0][2] == board[2][0]) {
            if (board[0][2] == 'X') return -10
            else if (board[0][2] == 'O') return 10
        }

        return 0
    }

    private fun hasMovesLeft(board: Array<CharArray>): Boolean {
        for (row in board) {
            for (col in row) {
                if (col == '\u0000') return true
            }
        }
        return false
    }

    private fun reset() {
        inTicTacToe = false
        bestMove = null
        roomCenter = null
    }
}