package xyz.meowing.krypt.api.dungeons.core.enums

import net.minecraft.entity.Entity
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitClient.client

/**
 * @author SkyblockAPI
 */
class DungeonPlayer(
    val name: String,
    dungeonClass: DungeonClass?,
    classLevel: Int?
) {
    val entity: Entity? get() = client.world?.entities?.find { it.name.stripped == name }
    val mapIcon = DungeonMapPlayer(this)

    var dead: Boolean = false
        internal set

    var dungeonClass: DungeonClass? = dungeonClass
        internal set
    var classLevel: Int? = classLevel
        internal set

    internal fun missingData(): Boolean = dungeonClass == null || classLevel == null

}