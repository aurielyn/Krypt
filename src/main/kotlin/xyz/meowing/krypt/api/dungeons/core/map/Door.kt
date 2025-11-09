package xyz.meowing.krypt.api.dungeons.core.map

import xyz.meowing.krypt.features.general.map.DungeonMap
import java.awt.Color

class Door(override val x: Int, override val z: Int, var type: DoorType): Tile {
    var opened = false

    override var state: RoomState = RoomState.UNDISCOVERED

    override val color: Color
        get() {
            return if (state == RoomState.UNOPENED) {
                DungeonMap.unopenedDoorColor
            } else when (this.type) {
                DoorType.BLOOD -> DungeonMap.bloodDoorColor
                DoorType.ENTRANCE -> DungeonMap.entranceDoorColor
                DoorType.WITHER -> DungeonMap.witherDoorColor
                else -> DungeonMap.normalDoorColor
            }
        }
}