package xyz.meowing.krypt.api.dungeons.enums.map

data class RoomMetadata(
    val name: String,
    val type: String,
    val cores: List<Int>,
    val secrets: Int = 0,
    val crypts: Int = 0,
    val trappedChests: Int = 0,
    val shape: String? = null,
    val doors: String? = null,
    val roomID: Int? = null,
    val clearType: String? = null
)