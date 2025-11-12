@file:Suppress("UNUSED")

package xyz.meowing.krypt.events.core

import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.state.PlayerRenderState
import com.mojang.blaze3d.vertex.PoseStack
import xyz.meowing.knit.api.events.CancellableEvent
import xyz.meowing.knit.api.events.Event
import xyz.meowing.knit.api.render.world.RenderContext

sealed class RenderEvent {
    sealed class World {
        /**
         * Posted at the end of world rendering.
         *
         * @see xyz.meowing.knit.mixins.MixinWorldRenderer
         * @since 1.2.0
         */
        class Last(
            val context: RenderContext
        ) : Event()

        /**
         * Posted after the entities have rendered.
         *
         * @see xyz.meowing.knit.mixins.MixinWorldRenderer
         * @since 1.2.0
         */
        class AfterEntities(
            val context: RenderContext
        ) : Event()

        /**
         * Posted when the block outline is being rendered.
         *
         * @see xyz.meowing.knit.mixins.MixinWorldRenderer
         * @since 1.2.0
         */
        class BlockOutline(
            val context: RenderContext
        ) : CancellableEvent()
    }

    sealed class Entity {
        /**
         * Posted before the entity has rendered.
         *
         * @see xyz.meowing.krypt.mixins.MixinEntityRenderDispatcher
         * @since 1.2.0
         */
        class Pre(
            val entity: net.minecraft.world.entity.Entity,
            val matrices: PoseStack,
            val vertex: MultiBufferSource?,
            val light: Int
        ) : CancellableEvent()

        /**
         * Posted after the entity has rendered.
         *
         * @see xyz.meowing.krypt.mixins.MixinEntityRenderDispatcher
         * @since 1.2.0
         */
        class Post(
            val entity: net.minecraft.world.entity.Entity,
            val matrices: PoseStack,
            val vertex: MultiBufferSource?,
            val light: Int
        ) : Event()
    }
}