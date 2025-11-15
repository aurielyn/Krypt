package xyz.meowing.krypt.features.general

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitClient.world
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager

@Module
object HideNonStarredTags : Feature(
    "hideNonStarredTags",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    override fun addConfig() {
        ConfigManager.addFeature(
            "Hide non-starred nametags",
            "Hides non-starred mob's nametags in dungeons.",
            "General",
            ConfigElement(
                "hideNonStarredTags",
                ElementType.Switch(false)
            )
        )
    }

    override fun initialize() {
        register<PacketEvent.Received> { event ->
            val packet = event.packet as? ClientboundAddEntityPacket ?: return@register
            if (packet.type != EntityType.ARMOR_STAND) return@register

            TickScheduler.Server.post {
                val entity = world?.getEntity(packet.id) ?: return@post
                val name = entity.customName?.stripped ?: return@post

                if (!name.endsWith("❤")) return@post
                if (name.contains("Blaze ") && name.contains("/")) return@post
                if (name.contains("✯")) return@post

                world?.removeEntity(packet.id, Entity.RemovalReason.DISCARDED)
            }
        }
    }
}