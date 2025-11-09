package xyz.meowing.krypt.api.dungeons.core.map

import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.core.handlers.DungeonScanner
import xyz.meowing.krypt.api.dungeons.core.handlers.WorldScanner
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.features.map.DungeonMap
import xyz.meowing.krypt.utils.WorldUtils.getBlockStateAt
import java.awt.Color
import kotlin.properties.Delegates

class Room(override val x: Int, override val z: Int, var data: RoomData): Tile {
    var core = 0
    var isSeparator = false
    var rotation: Int? = null
    var highestBlock: Int? = null

    override var state: RoomState by Delegates.observable(RoomState.UNDISCOVERED) { _, oldValue, newValue ->
        if (uniqueRoom?.mainRoom != this) return@observable
        if (oldValue == newValue) return@observable
        if (data.name == "Unknown") return@observable

        val roomPlayers = DungeonAPI.teammates.filter {
            WorldScanner.getRoomFromPos(it.mapIcon.getRealPos())?.data?.name == data.name
        }

        EventBus.post(DungeonEvent.Room.StateChange(this, oldValue, newValue, roomPlayers))
    }

    override val color: Color
        get() {
            return if (state == RoomState.UNOPENED) Color(65, 65, 65)
            else when (data.type) {
                RoomType.BLOOD -> DungeonMap.bloodRoomColor
                RoomType.CHAMPION -> DungeonMap.yellowRoomColor
                RoomType.RARE -> DungeonMap.yellowRoomColor
                RoomType.ENTRANCE -> DungeonMap.entranceRoomColor
                RoomType.FAIRY -> DungeonMap.fairyRoomColor
                RoomType.PUZZLE -> DungeonMap.puzzleRoomColor
                RoomType.TRAP -> DungeonMap.trapRoomColor
                else -> DungeonMap.normalRoomColor
            }
        }
    var uniqueRoom: UniqueRoom? = null

    fun getArrayPosition(): Pair<Int, Int> {
        return Pair((x - DungeonScanner.startX) / 16, (z - DungeonScanner.startZ) / 16)
    }

    fun getRoomComponent(): Pair<Int, Int> = WorldScanner.getRoomComponent(net.minecraft.util.math.BlockPos(x, 0, z))

    fun addToUnique(row: Int, column: Int, roomName: String = data.name) {
        val unique = DungeonAPI.uniqueRooms.find { it.name == roomName }

        if (unique == null) {
            UniqueRoom(column, row, this).let {
                DungeonAPI.uniqueRooms.add(it)
                uniqueRoom = it
            }
        }
        else {
            unique.addTile(column, row, this)
            uniqueRoom = unique
        }
    }

    fun findRotation() {
        if (rotation != null) return
        if (highestBlock == null) {
            highestBlock = WorldScanner.getHighestBlockAt(x, z)
            return
        }
        if (data.type == RoomType.FAIRY) {
            rotation = 0
            return
        }

        val realComponents = uniqueRoom?.tiles?.map { Pair(it.x, it.z) } ?: return
        for (c in realComponents) {
            val (x, z) = c
            DungeonScanner.clayBlocksCorners.withIndex().forEach { (i, offset) ->
                val (rx, rz) = offset
                val state = getBlockStateAt(x + rx, highestBlock!!, z + rz)
                if (!state.isOf(net.minecraft.block.Blocks.BLUE_TERRACOTTA)) return@forEach

                rotation = i * 90
                return
            }
        }
    }
}