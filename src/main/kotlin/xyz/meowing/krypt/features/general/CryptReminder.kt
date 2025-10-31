package xyz.meowing.krypt.features.general

import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonAPI
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.scheduler.TimeScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.LocationAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.StringUtils.removeFormatting

@Module
object CryptReminder : Feature("general.cryptReminder", area = SkyBlockIsland.THE_CATACOMBS) {
    private val delay by ConfigDelegate<Double>("general.cryptReminder.delay")
    private val sendToParty by ConfigDelegate<Boolean>("general.cryptReminder.sendToParty")

    override fun addConfig() {
        ConfigManager
            .addFeature("Crypt reminder", "Crypt reminder", "Dungeons", ConfigElement(
                "general.cryptReminder",
                ElementType.Switch(false)
            ))
            .addFeatureOption("Crypt reminder delay", "Crypt reminder delay", "Options", ConfigElement(
                "general.cryptReminder.delay",
                ElementType.Slider(1.0, 5.0, 2.0, false)
            ))
            .addFeatureOption("Send to party", "", "Options", ConfigElement(
                "general.cryptReminder.sendToParty",
                ElementType.Switch(true)
            ))
    }

    override fun initialize() {
        register<ChatEvent.Receive> { event ->
            if (event.isActionBar) return@register
            if (event.message.string.removeFormatting() == "[NPC] Mort: Good luck.") {
                TimeScheduler.schedule(1000 * 60 * delay.toLong()) {
                    if (LocationAPI.island != SkyBlockIsland.THE_CATACOMBS || DungeonAPI.inBoss) return@schedule
                    val cryptCount = xyz.meowing.krypt.api.dungeons.DungeonAPI.cryptCount
                    if (sendToParty) KnitChat.sendCommand("pc $cryptCount/5 crypts")
                }
            }
        }
    }
}