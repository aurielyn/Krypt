package xyz.meowing.krypt.api.dungeons.enums.map

enum class RoomClearType {
    MOB,
    MINIBOSS,
    OTHER
    ;

    companion object {
        fun fromData(data: RoomMetadata): RoomClearType {
            return when (data.clearType?.lowercase()) {
                "mob" -> MOB
                "miniboss" -> MINIBOSS
                else -> OTHER
            }
        }
    }
}