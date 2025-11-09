package xyz.meowing.krypt.api.dungeons.core.utils

import net.minecraft.item.map.MapDecoration
import net.minecraft.util.math.Vec3d
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.core.handlers.DungeonScanner
import xyz.meowing.krypt.api.dungeons.core.handlers.HotbarMapColorParser
import xyz.meowing.krypt.utils.StringUtils.equalsOneOf

object MapUtils {
    val MapDecoration.mapX get() = ((x + 128) shr 1).toFloat()
    val MapDecoration.mapZ get() = ((z + 128) shr 1).toFloat()
    val MapDecoration.yaw get() = rotation * 22.5f

    fun coordsToMap(vec: Vec3d): Pair<Float, Float> {
        val x = ((vec.x - DungeonScanner.startX) * coordMultiplier + startCorner.first).toFloat()
        val z = ((vec.z - DungeonScanner.startZ) * coordMultiplier + startCorner.second).toFloat()
        return Pair(x, z)
    }

    var startCorner = Pair(5, 5)
    var mapRoomSize = 16
    var coordMultiplier = 0.625
    var calibrated = false

    fun calibrateMap(): Boolean {
        val (start, size) = findEntranceCorner()
        if (size.equalsOneOf(16, 18)) {
            mapRoomSize = size
            startCorner = when (DungeonAPI.dungeonFloor?.floorNumber) {
                null -> Pair(22, 22)
                1 -> Pair(22, 11)
                2, 3 -> Pair(11, 11)
                else -> {
                    val startX = start and 127
                    val startZ = start shr 7
                    Pair(startX % (mapRoomSize + 4), startZ % (mapRoomSize + 4))
                }
            }
            coordMultiplier = (mapRoomSize + 4.0) / DungeonScanner.roomSize
            HotbarMapColorParser.calibrate()
            return true
        }
        return false
    }

    private fun findEntranceCorner(): Pair<Int, Int> {
        var start = 0
        var currLength = 0
        DungeonAPI.mapData?.colors?.forEachIndexed { index, byte ->
            if (byte == 30.toByte()) {
                if (currLength == 0) start = index
                currLength++
            } else {
                if (currLength >= 16) {
                    return Pair(start, currLength)
                }
                currLength = 0
            }
        }
        return Pair(start, currLength)
    }
}