package xyz.meowing.krypt.api.dungeons.enums.map

enum class RoomShape(val jsonString: String) {
    UNKNOWN("unknown"),
    SHAPE_1X1("1x1"),
    SHAPE_1X2("1x2"),
    SHAPE_1X3("1x3"),
    SHAPE_1X4("1x4"),
    SHAPE_2X2("2x2"),
    SHAPE_L("L");

    companion object {
        fun fromString(str: String?): RoomShape {
            return entries.find { it.jsonString.equals(str, ignoreCase = true) } ?: UNKNOWN
        }

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