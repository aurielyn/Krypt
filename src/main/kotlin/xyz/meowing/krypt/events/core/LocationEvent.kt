package xyz.meowing.krypt.events.core

import net.hypixel.data.region.Environment
import net.hypixel.data.type.ServerType
import xyz.meowing.knit.api.events.Event
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.api.location.SkyBlockArea
import xyz.meowing.krypt.api.location.SkyBlockIsland

sealed class LocationEvent {
    class ServerChange(
        val name: String,
        val type: ServerType?,
        val lobby: String?,
        val mode: String?,
        val map: String?,
    ) : Event()

    class IslandChange(
        val old: SkyBlockIsland?,
        val new: SkyBlockIsland?
    ) : Event()

    class AreaChange(
        val old: SkyBlockArea,
        val new: SkyBlockArea
    ) : Event()

    class DungeonFloorChange(
        val new: DungeonFloor?
    ) : Event()

    class WorldChange : Event()

    class HypixelJoin(
        val environment: Environment
    ) : Event() {
        val onAlpha: Boolean get() = environment == Environment.BETA
    }

    class SkyblockJoin : Event()

    class SkyblockLeave : Event()
}