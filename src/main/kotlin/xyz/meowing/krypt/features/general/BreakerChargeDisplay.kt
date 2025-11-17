package xyz.meowing.krypt.features.general

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.hud.HudManager
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.rendering.Render2D

@Module
object BreakerChargeDisplay : Feature(
    "breakerChargeDisplay",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private const val NAME = "Breaker Charge Display"
    var renderString = ""

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Breaker charge display",
                "Displays the charges left and the max charges on your dungeon breaker.",
                "General",
                ConfigElement(
                    "breakerChargeDisplay",
                    ElementType.Switch(false)
                )
            )
    }

    override fun initialize() {
        HudManager.register(NAME, "§bCharges: §e20§7/§e20§c⸕", "breakerChargeDisplay")

        register<PacketEvent.Received> { event ->
            val packet = event.packet as? ClientboundContainerSetSlotPacket ?: return@register
            val stack = packet.item ?: return@register

            if (stack.getData(DataTypes.SKYBLOCK_ID)?.skyblockId?.equals("DUNGEONBREAKER") == false) return@register

            val charges = stack.getData(DataTypes.DUNGEONBREAKER_CHARGES) ?: return@register
            val first = charges.first
            val max = charges.second

            val colorCode = when {
                first >= 15 -> "§a"
                first >= 10 -> "§b"
                else -> "§c"
            }

            renderString = "§bCharges: ${colorCode}${first}§7/§a${max}§c⸕"
        }

        register<GuiEvent.Render.HUD> { event ->
            if (renderString.isEmpty()) return@register

            val x = HudManager.getX(NAME)
            val y = HudManager.getY(NAME)
            val scale = HudManager.getScale(NAME)

            Render2D.renderString(event.context, renderString, x, y, scale)
        }

        register<LocationEvent.WorldChange> {
            renderString = ""
        }
    }
}