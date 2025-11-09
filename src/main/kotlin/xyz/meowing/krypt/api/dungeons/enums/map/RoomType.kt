package xyz.meowing.krypt.api.dungeons.enums.map

import xyz.meowing.krypt.features.general.map.DungeonMap
import java.awt.Color

enum class RoomType {
    NORMAL,
    PUZZLE,
    TRAP,
    YELLOW,
    BLOOD,
    FAIRY,
    ENTRANCE,
    UNKNOWN
    ;

    val color: Color
        get() = when (this) {
            NORMAL -> DungeonMap.normalRoomColor
            PUZZLE -> DungeonMap.puzzleRoomColor
            TRAP -> DungeonMap.trapRoomColor
            YELLOW -> DungeonMap.yellowRoomColor
            BLOOD -> DungeonMap.bloodRoomColor
            FAIRY -> DungeonMap.fairyRoomColor
            ENTRANCE -> DungeonMap.entranceRoomColor
            UNKNOWN -> Color.GRAY
        }

    companion object {
        fun fromMapColor(color: Int): RoomType? = when (color) {
            18 -> BLOOD
            30 -> ENTRANCE
            63, 85 -> NORMAL
            82 -> FAIRY
            62 -> TRAP
            74 -> YELLOW
            66 -> PUZZLE
            else -> null
        }
    }
}