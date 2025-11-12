package xyz.meowing.krypt.api.dungeons.utils

import net.minecraft.core.BlockPos
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.krypt.utils.WorldUtils

object WorldScanUtils {
    val blacklist = setOf(5, 54, 146)

    fun isChunkLoaded(x: Int, z: Int): Boolean {
        val world = KnitClient.world ?: return false
        val chunkX = x shr 4
        val chunkZ = z shr 4
        return world.chunkSource.hasChunk(chunkX, chunkZ)
    }

    fun getCore(x: Int, z: Int): Int {
        val sb = StringBuilder(150)
        val height = getHighestY(x, z)?.coerceIn(11..140) ?: 140 .coerceIn(11..140)

        sb.append(CharArray(140 - height) { '0' })
        var bedrock = 0

        for (y in height downTo 12) {
            val id = WorldUtils.checkIfAir(x, y, z)

            if (id == 0 && bedrock >= 2 && y < 69) {
                sb.append(CharArray(y - 11) { '0' })
                break
            }

            if (id == 7) {
                bedrock++
            } else {
                bedrock = 0
                if (id in blacklist) continue
            }
            sb.append(id)
        }
        return sb.toString().hashCode()
    }

    fun getHighestY(x: Int, z: Int): Int? {
        for (y in 255 downTo 0) {
            val id = WorldUtils.getBlockNumericId(x, y, z)
            if (id != 0 && id != 41) return y
        }
        return null
    }

    fun componentToRealCoord(x: Int, z: Int, includeDoors: Boolean = false): Pair<Int, Int> {
        val (x0, z0) = ScanUtils.cornerStart
        val offset = if (includeDoors) ScanUtils.halfCombinedSize else ScanUtils.roomDoorCombinedSize
        return Pair(x0 + ScanUtils.halfRoomSize + offset * x, z0 + ScanUtils.halfRoomSize + offset * z)
    }

    fun realCoordToComponent(x: Int, z: Int, includeDoors: Boolean = false): Pair<Int, Int> {
        val (x0, z0) = ScanUtils.cornerStart
        val size = if (includeDoors) ScanUtils.halfCombinedSize else ScanUtils.roomDoorCombinedSize
        val shift = 4 + ((size - 16) shr 4)
        return Pair(((x - x0 + 0.5).toInt() shr shift), ((z - z0 + 0.5).toInt() shr shift))
    }

    fun rotateCoord(pos: Triple<Int, Int, Int>, degree: Int): Triple<Int, Int, Int> {
        val d = (degree + 360) % 360
        return when (d) {
            0   -> pos
            90  -> Triple(pos.third, pos.second, -pos.first)
            180 -> Triple(-pos.first, pos.second, -pos.third)
            270 -> Triple(-pos.third, pos.second, pos.first)
            else -> pos
        }
    }

    fun getRoomShape(comps: List<Pair<Int, Int>>): String {
        val count = comps.size
        val xs = comps.map { it.first }.toSet()
        val zs = comps.map { it.second }.toSet()

        return when (count) {
            1 -> "1x1"
            2 -> "1x2"
            3 -> if (xs.size == 3 || zs.size == 3) "1x3" else "L"
            4 -> if (xs.size == 1 || zs.size == 1) "1x4" else "2x2"
            else -> "Unknown"
        }
    }

    private const val ROOM_SIZE_SHIFT = 5
    private const val DUNGEON_START = -185

    fun getRoomCenter(posX: Int, posZ: Int): Pair<Int, Int> {
        val roomX = (posX - DUNGEON_START + (1 shl (ROOM_SIZE_SHIFT - 1))) shr ROOM_SIZE_SHIFT
        val roomZ = (posZ - DUNGEON_START + (1 shl (ROOM_SIZE_SHIFT - 1))) shr ROOM_SIZE_SHIFT
        return Pair(
            ((roomX shl ROOM_SIZE_SHIFT) + DUNGEON_START),
            ((roomZ shl ROOM_SIZE_SHIFT) + DUNGEON_START)
        )
    }

    fun getRealCoord(pos: BlockPos, center: BlockPos, rotation: Int): BlockPos {
        val relX = pos.x
        val relZ = pos.z

        val rotated = when ((rotation % 360 + 360) % 360) {
            0 -> Pair(relX, relZ)
            90 -> Pair(relZ, -relX)
            180 -> Pair(-relX, -relZ)
            270 -> Pair(-relZ, relX)
            else -> Pair(relX, relZ)
        }

        return BlockPos(
            center.x + rotated.first,
            pos.y,
            center.z + rotated.second
        )
    }

    fun getRoomCenterComponent(posX: Int, posZ: Int): Pair<Int, Int> {
        val (centerX, centerZ) = getRoomCenter(posX, posZ)
        return realCoordToComponent(centerX, centerZ, includeDoors = false)
    }

    fun isInRoomBounds(x: Int, z: Int, roomX: Int, roomZ: Int): Boolean {
        val halfRoom = ScanUtils.halfRoomSize
        return x in (roomX - halfRoom)..(roomX + halfRoom) && z in (roomZ - halfRoom)..(roomZ + halfRoom)
    }
}