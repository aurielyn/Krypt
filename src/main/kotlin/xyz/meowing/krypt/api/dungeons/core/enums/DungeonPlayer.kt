package xyz.meowing.krypt.api.dungeons.core.enums

import net.minecraft.entity.Entity
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.krypt.api.dungeons.core.map.Room
import xyz.meowing.krypt.api.dungeons.core.map.RoomState

/**
 * @author SkyblockAPI
 */
class DungeonPlayer(
    val name: String,
    dungeonClass: DungeonClass?,
    classLevel: Int?
) {
    data class RoomClearRecord(
        val room: Room,
        val state: RoomState,
        val clearTime: Long,
        val solo: Boolean
    )

    val entity: Entity? get() = client.world?.entities?.find { it.name.stripped == name }
    val mapIcon = DungeonMapPlayer(this)

    var dead: Boolean = false
        internal set

    var dungeonClass: DungeonClass? = dungeonClass
        internal set
    var classLevel: Int? = classLevel
        internal set

    var minRooms = 0
    var maxRooms = 0

    internal fun missingData(): Boolean = dungeonClass == null || classLevel == null

    val clearedRooms: MutableList<RoomClearRecord> = mutableListOf()
}