package xyz.meowing.krypt.api.dungeons.utils

import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.DungeonAPI.inBoss
import net.minecraft.item.FilledMapItem
import net.minecraft.item.map.MapDecoration
import net.minecraft.item.map.MapDecorationTypes
import net.minecraft.item.map.MapState
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.map.MapScanner
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.events.core.TickEvent

@Module
object MapUtils {
    val MapDecoration.mapX get() = (this.x() + 128) shr 1
    val MapDecoration.mapZ get() = (this.z + 128) shr 1
    val MapDecoration.yaw get() = this.rotation * 22.5f

    var mapCorners = Pair(5, 5)
    var mapRoomSize = 16
    var mapGapSize = 0
    var coordMultiplier = 0.625
    var calibrated = false

    var mapData: MapState? = null
    var guessMapData: MapState? = null

    init {
        EventBus.registerIn<PacketEvent.Received>(SkyBlockIsland.THE_CATACOMBS) { event->
            val packet = event.packet as? MapUpdateS2CPacket ?: return@registerIn

            if (mapData == null) {
                val world = KnitClient.world ?: return@registerIn
                val id = packet.mapId.id
                if (id and 1000 == 0) {
                    val guess = FilledMapItem.getMapState(packet.mapId, world) ?: return@registerIn
                    if (guess.decorations.any {it.type == MapDecorationTypes.FRAME }) {
                        guessMapData = guess
                    }
                }
            }
        }

        EventBus.registerIn<TickEvent.Client>(SkyBlockIsland.THE_CATACOMBS) {
            if (!calibrated) {
                if (mapData == null) {
                    mapData = getCurrentMapState()
                }

                calibrated = calibrateDungeonMap()
            } else if (!inBoss) {
                (mapData ?: guessMapData)?.let {
                    MapScanner.updatePlayers(it)
                    MapScanner.scan(it)
                }
            }
        }
    }

    fun getCurrentMapState(): MapState? {
        val stack = KnitClient.player?.inventory?.getStack(8) ?: return null
        if (stack.item !is FilledMapItem || !stack.name.string.contains("Magical Map")) return null
        return FilledMapItem.getMapState(stack, KnitClient.world)
    }

    fun calibrateDungeonMap(): Boolean {
        val mapState = getCurrentMapState() ?: return false
        val entranceInfo = findEntranceCorner(mapState.colors) ?: return false

        val (startIndex, size) = entranceInfo
        mapRoomSize = size
        mapGapSize = mapRoomSize + 4 // compute gap size from room width

        var x = (startIndex % 128) % mapGapSize
        var z = (startIndex / 128) % mapGapSize

        val floor = DungeonAPI.floor?.floorNumber?: return false
        if (floor in listOf(0, 1)) x += mapGapSize
        if (floor == 0) z += mapGapSize

        mapCorners = x to z
        coordMultiplier = mapGapSize / ScanUtils.roomDoorCombinedSize.toDouble()

        return true
    }

    fun findEntranceCorner(colors: ByteArray): Pair<Int, Int>? {
        for (i in colors.indices) {
            if (colors[i] != 30.toByte()) continue

            // Check horizontal 15-block chain
            if (i + 15 < colors.size && colors[i + 15] == 30.toByte()) {
                // Check vertical 15-block chain
                if (i + 128 * 15 < colors.size && colors[i + 128 * 15] == 30.toByte()) {
                    var length = 0
                    while (i + length < colors.size && colors[i + length] == 30.toByte()) {
                        length++
                    }
                    return Pair(i, length)
                }
            }
        }
        return null
    }

    fun reset() {
        mapCorners = Pair(5, 5)
        mapRoomSize = 16
        mapGapSize = 0
        coordMultiplier = 0.625
        calibrated = false
        mapData = null
        guessMapData = null
    }
}