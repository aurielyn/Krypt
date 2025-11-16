package xyz.meowing.krypt.api.dungeons.enums.map

enum class RoomRotations(
    val x: Int,
    val z: Int,
    val degrees: Int
) {
    SOUTH(-15, -15, 0),
    WEST(15, -15, 90),
    NORTH(15, 15, 180),
    EAST(-15, 15, 270),
    NONE(0, 0, -1)
    ;
}