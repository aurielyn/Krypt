package xyz.meowing.krypt.utils

import net.minecraft.world.entity.Entity
import kotlin.time.Duration

/**
 * Modified from SkyOcean's implementation
 *
 * Original File: [GitHub](https://github.com/meowdding/SkyOcean/blob/main/src/common/main/kotlin/me/owdding/skyocean/helpers/EntityHelper.kt)
 * @author Meowdding
 */
internal interface EntityAccessor {
    fun `krypt$setGlowing`(glowing: Boolean)
    fun `krypt$setGlowingColor`(color: Int)
    fun `krypt$glowTime`(time: Long)
    fun `krypt$setGlowingThisFrame`(glowing: Boolean)
}

var Entity.isCurrentlyGlowing: Boolean
    get() = this.isCurrentlyGlowing
    set(value) {
        (this as EntityAccessor).`krypt$setGlowing`(value)
    }

var Entity.glowTime: Duration
    get() = Duration.INFINITE
    set(value) {
        (this as EntityAccessor).`krypt$glowTime`(value.inWholeMilliseconds)
    }

var Entity.glowingColor: Int
    get() = this.teamColor
    set(value) {
        (this as EntityAccessor).`krypt$setGlowingColor`(value)
    }

var Entity.glowThisFrame: Boolean
    get() = false
    set(value) {
        (this as EntityAccessor).`krypt$setGlowingThisFrame`(value)
    }