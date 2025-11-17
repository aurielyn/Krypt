@file:Suppress("ConstPropertyName")

package xyz.meowing.krypt.api.dungeons.utils

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.krypt.api.dungeons.enums.map.Room
import kotlin.math.floor


/**
 * Contains modified code from Noamm's ScanUtils.
 */
object ScanUtils {
    val cornerStart = Pair(-200, -200)
    val cornerEnd   = Pair(-10, -10)

    const val dungeonRoomSize = 31
    const val dungeonDoorSize = 1
    const val roomDoorCombinedSize = dungeonRoomSize + dungeonDoorSize
    const val halfRoomSize = dungeonRoomSize / 2
    const val halfCombinedSize = roomDoorCombinedSize / 2

    val defaultMapSize = Pair(125, 125)

    val directions = listOf(
        listOf(halfCombinedSize, 0, 1, 0),
        listOf(-halfCombinedSize, 0, -1, 0),
        listOf(0, halfCombinedSize, 0, 1),
        listOf(0, -halfCombinedSize, 0, -1)
    )

    val mapDirections = listOf(
        1 to 0,  // East
        -1 to 0, // West
        0 to 1,  // South
        0 to -1  // North
    )

    fun getScanCoord(): List<Triple<Int, Int, Pair<Int, Int>>> {
        val cords = mutableListOf<Triple<Int, Int, Pair<Int, Int>>>()

        for (z in 0..<11) {
            for (x in 0..<11) {
                if (x % 2 == 1 && z % 2 == 1) continue

                val rx = cornerStart.first + halfRoomSize + x * halfCombinedSize
                val rz = cornerStart.second + halfRoomSize + z * halfCombinedSize
                cords += Triple(x, z, Pair(rx, rz))
            }
        }

        return cords
    }

    fun getRotation(center: BlockPos, relativeCoords: Map<Block, BlockPos>): Int? {
        relativeCoords.forEach { (block, coords) ->
            for (i in 0..3) {
                val pos = getRealCoord(coords, center, i * 90)
                if (client.level?.getBlockState(pos)?.block == block) {
                    return i * 90
                }
            }
        }
        return null
    }

    fun BlockPos.rotateBlock(degree: Int): BlockPos {
        return when ((degree % 360 + 360) % 360) {
            0 -> BlockPos(x, y, z)
            90 -> BlockPos(z, y, -x)
            180 -> BlockPos(-x, y, -z)
            270 -> BlockPos(-z, y, x)
            else -> BlockPos(x, y, z)
        }
    }

    fun getRealCoord(pos: BlockPos, roomCenter: BlockPos, rotation: Int): BlockPos {
        return pos.rotateBlock(rotation).offset(roomCenter.x, roomCenter.y, roomCenter.z)
    }

    fun getRoomComponent(pos: BlockPos): Pair<Int, Int> {
        val gx = floor((pos.x + 200.5) / 32).toInt()
        val gz = floor((pos.z + 200.5) / 32).toInt()
        return Pair(gx, gz)
    }

    fun getRoomCorner(pair: Pair<Int, Int>): Pair<Int, Int> {
        return Pair(
            -200 + pair.first * 32,
            -200 + pair.second * 32
        )
    }

    fun getRoomCenter(pair: Pair<Int, Int>): Pair<Int, Int> {
        return Pair(pair.first + 15, pair.second + 15)
    }

    fun getRoomCenter(room: Room): BlockPos {
        return getRoomCenter(getRoomCorner(room.getRoomComponent())).run { BlockPos(first, 0, second) }
    }
}