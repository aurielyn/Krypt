package xyz.meowing.krypt.api.dungeons.enums.map

import xyz.meowing.krypt.api.dungeons.utils.WorldScanUtils
import xyz.meowing.krypt.features.general.map.DungeonMap
import xyz.meowing.krypt.utils.WorldUtils
import java.awt.Color

class Door(val worldPos: Pair<Int, Int>, val componentPos: Pair<Int, Int>) {
    var opened: Boolean = false
    var rotation: Int? = null
    var type: DoorType = DoorType.NORMAL
    var state = DoorState.UNDISCOVERED

    val color: Color
        get() {
            return when (this.type) {
                DoorType.BLOOD -> DungeonMap.bloodDoorColor
                DoorType.ENTRANCE -> DungeonMap.entranceDoorColor
                DoorType.WITHER -> DungeonMap.witherDoorColor
                DoorType.NORMAL -> DungeonMap.normalDoorColor
            }
        }

    fun getPos(): Triple<Int, Int, Int> {
        return Triple(worldPos.first, 69, worldPos.second)
    }

    fun setType(type: DoorType): Door {
        this.type = type
        return this
    }

    fun setState(state: DoorState): Door {
        this.state = state
        return this
    }

    fun check() {
        val (x, y, z) = getPos()
        if (!WorldScanUtils.isChunkLoaded(x, z)) return

        val id = WorldUtils.getBlockNumericId(x, y, z)
        opened = (id == 0) && this.type != DoorType.WITHER
    }
}