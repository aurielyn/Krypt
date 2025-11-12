package xyz.meowing.krypt.features.alerts

import net.minecraft.world.entity.decoration.ArmorStand
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.matchOrNull
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.EntityEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.TitleUtils.showTitle

@Module
object KeyAlert : Feature(
    "keyAlerts",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private val keyObtainedRegex = Regex("(?:\\[.+] ?)?(?<user>\\w+) has obtained (?<type>\\w+) Key!")
    private val keyPickedUpRegex = Regex("A (?<type>\\w+) Key was picked up!")

    private var lastKeyID: Int? = null

    private val spawnAlert by ConfigDelegate<Boolean>("keyAlerts.spawnAlert")
    private val pickUpAlert by ConfigDelegate<Boolean>("keyAlerts.pickUpAlert")

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Key alerts",
                "",
                "Alerts",
                ConfigElement(
                    "keyAlerts",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Spawn alert",
                ConfigElement(
                    "keyAlerts.spawnAlert",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Pickup alert",
                ConfigElement(
                    "keyAlerts.pickUpAlert",
                    ElementType.Switch(false)
                )
            )
    }

    override fun initialize() {
        register<LocationEvent.WorldChange> { lastKeyID = null }

        register<EntityEvent.Packet.Metadata> { event ->
            if (DungeonAPI.bloodOpened || !spawnAlert) return@register

            val entity = event.entity as? ArmorStand ?: return@register

            if (lastKeyID == entity.id) return@register // why? to make it only send once for the entity

            lastKeyID = entity.id

            val name = entity.name?.stripped ?: return@register

            when {
                name.contains("Wither Key") -> showTitle("§8Wither §fkey spawned!", null, 2000)
                name.contains("Blood Key") -> showTitle("§cBlood §fkey spawned!", null, 2000)
            }
        }

        register<ChatEvent.Receive> { event ->
            if (event.isActionBar || !pickUpAlert) return@register
            val message = event.message.stripped

            keyObtainedRegex.matchOrNull(message, "user", "type") { (user, type) ->
                val color = when (type.lowercase()) {
                    "wither" -> "§8"
                    "blood" -> "§c"
                    else -> "§f"
                }

                showTitle("$color$type §fkey picked up by §b$user§f!", null, 2000)
            }?.let { return@register }

            keyPickedUpRegex.matchOrNull(message, "type") { (type) ->
                val color = when (type.lowercase()) {
                    "wither" -> "§8"
                    "blood" -> "§c"
                    else -> "§f"
                }

                showTitle("$color$type §fkey picked up!", null, 2000)
            }
        }
    }
}