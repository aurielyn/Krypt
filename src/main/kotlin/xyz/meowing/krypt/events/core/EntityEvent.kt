package xyz.meowing.krypt.events.core

import net.minecraft.entity.Entity
import xyz.meowing.knit.api.events.Event

sealed class EntityEvent {
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