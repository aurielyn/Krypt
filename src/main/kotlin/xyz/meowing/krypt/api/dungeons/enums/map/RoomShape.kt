package xyz.meowing.krypt.api.dungeons.enums.map

enum class RoomShape {
    UNKNOWN,
    SHAPE_1X1,
    SHAPE_1X2,
    SHAPE_1X3,
    SHAPE_1X4,
    SHAPE_2X2,
    SHAPE_L;

    companion object {
        fun fromComponents(comps: List<Pair<Int, Int>>): RoomShape {
            val count = comps.size
            val xSet = comps.map { it.first }.toSet()
            val zSet = comps.map { it.second }.toSet()
            val distX = xSet.size
            val distZ = zSet.size

            return when {
                comps.isEmpty() || count > 4 -> UNKNOWN
                count == 1 -> SHAPE_1X1
                count == 2 -> SHAPE_1X2
                count == 4 -> if (distX == 1 || distZ == 1) SHAPE_1X4 else SHAPE_2X2
                distX == count || distZ == count -> SHAPE_1X3
                else -> SHAPE_L
            }
        }
    }
}