package xyz.meowing.krypt.events.core

import net.minecraft.entity.Entity
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket
import xyz.meowing.knit.api.events.CancellableEvent
import xyz.meowing.knit.api.events.Event

sealed class EntityEvent {
    /**
     * Posted when the entity spawns into the ClientWorld
     *
     * @see xyz.meowing.knit.api.events.EventBus
     * @since 1.2.0
     */
    class Join(
        val entity: Entity
    ) : Event()

    /**
     * Posted when the entity dies.
     *
     * @see xyz.meowing.krypt.mixins.MixinLivingEntity
     * @since 1.2.0
     */
    class Death(
        val entity: Entity
    ) : Event()

    sealed class Packet {
        /**
         * Posted when the client receives the EntityTrackerUpdateS2CPacket packet.
         *
         * @see xyz.meowing.krypt.mixins.MixinClientPlayNetworkHandler
         * @since 1.2.0
         */
        class Metadata(
            val packet: EntityTrackerUpdateS2CPacket,
            val entity: Entity,
            val name: String
        ) : CancellableEvent()
    }
}