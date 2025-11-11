package xyz.meowing.krypt.features.alerts

import net.minecraft.network.packet.s2c.play.*
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.TitleUtils.showTitle

@Module
object ShadowAssassin : Feature(
    "shadowAssassinAlert",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Shadow assassin alert",
                "",
                "Alerts",
                ConfigElement(
                    "shadowAssassinAlert",
                    ElementType.Switch(false)
                )
            )
    }

    override fun initialize() {
        register<PacketEvent.Received> { event ->
            val packet = event.packet as? WorldBorderInitializeS2CPacket ?: return@register
            if (packet.size != 1.0) return@register

            showTitle(subtitle = if (DungeonAPI.floor?.floorNumber == 1) "§cBonzo Respawn" else "§8Shadow Assassin", duration = 2000)
        }
    }
}