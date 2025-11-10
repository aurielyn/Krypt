package xyz.meowing.krypt.events.core

import net.minecraft.entity.Entity
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
}