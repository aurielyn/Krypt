package xyz.meowing.krypt.features.general

import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.scheduler.TimeScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.location.LocationAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.TitleUtils.showTitle

@Module
object CryptReminder : Feature(
    "cryptReminder",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private val delay by ConfigDelegate<Double>("cryptReminder.delay")
    private val sendToParty by ConfigDelegate<Boolean>("cryptReminder.sendToParty")

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Crypt reminder",
                "Crypt reminder",
                "General",
                ConfigElement(
                    "cryptReminder",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Crypt reminder delay",
                ConfigElement(
                    "cryptReminder.delay",
                    ElementType.Slider(1.0, 5.0, 2.0, false)
                )
            )
            .addFeatureOption("Send to party",
                ConfigElement(
                    "cryptReminder.sendToParty",
                    ElementType.Switch(true)
                )
            )
    }

    override fun initialize() {
        register<ChatEvent.Receive> { event ->
            if (event.isActionBar) return@register
            if (event.message.stripped == "[NPC] Mort: Good luck.") {
                TimeScheduler.schedule(1000 * 60 * delay.toLong()) {
                    val cryptCount = DungeonAPI.cryptCount

                    if (cryptCount == 5 || LocationAPI.island != SkyBlockIsland.THE_CATACOMBS || DungeonAPI.inBoss) return@schedule

                    showTitle("§c$cryptCount§7/§c5 §fcrypts", null, 3000)
                    if (sendToParty) KnitChat.sendCommand("pc $cryptCount/5 crypts")
                }
            }
        }
    }
}