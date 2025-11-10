package xyz.meowing.krypt.events.core

import xyz.meowing.knit.api.events.Event
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.api.dungeons.enums.DungeonKey
import xyz.meowing.krypt.api.dungeons.enums.DungeonPlayer
import xyz.meowing.krypt.api.dungeons.enums.map.Checkmark

sealed class DungeonEvent {
    /**
     * Posted when the dungeon starts.
     *
     * @see xyz.meowing.krypt.api.dungeons.DungeonAPI
     * @since 1.2.0
     */
    class Start(
        val floor: DungeonFloor
    ) : Event()

    class End(
        val floor: DungeonFloor
    ) : Event()

    /**
     * Posted when the player loads into a Dungeon.
     *
     * @see xyz.meowing.krypt.api.dungeons.DungeonAPI
     * @since 1.2.0
     */
    class Enter(
        val floor: DungeonFloor
    ) : Event()

    sealed class Room {
        class Change(
            val old: xyz.meowing.krypt.api.dungeons.enums.map.Room,
            val new: xyz.meowing.krypt.api.dungeons.enums.map.Room
        ) : Event()

        class StateChange(
            val room: xyz.meowing.krypt.api.dungeons.enums.map.Room,
            val oldState: Checkmark,
            val newState: Checkmark,
            val roomPlayers: List<DungeonPlayer>
        ) : Event()
    }

    /**
     * Posted when a key is picked up
     *
     * @see xyz.meowing.krypt.api.dungeons.DungeonAPI
     * @since 1.2.0
     */
    class KeyPickUp(
        val key: DungeonKey
    ) : Event()
}