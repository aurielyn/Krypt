package xyz.meowing.krypt.features.alerts

import xyz.meowing.knit.api.KnitChat
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.TitleUtils

@Module
object MimicAlert : Feature(
    "mimicAlert",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Mimic alert",
                "Mimic alert",
                "Alerts",
                ConfigElement(
                    "mimicAlert",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Show title",
                ConfigElement(
                    "mimicAlert.showTitle",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Send chat message",
                ConfigElement(
                    "mimicAlert.sendMessage",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Mimic message",
                ConfigElement(
                    "mimicAlert.message",
                    ElementType.TextInput("Mimic Killed!")
                )
            )
    }

    private val message by ConfigDelegate<String>("mimicAlert.message")
    private val sendMessage by ConfigDelegate<Boolean>("mimicAlert.sendMessage")
    private val showTitle by ConfigDelegate<Boolean>("mimicAlert.showTitle")
    private val enabled by ConfigDelegate<Boolean>("mimicAlert")

    fun displayTitle() {
        if (!enabled) return

        if (showTitle) TitleUtils.showTitle(message, duration = 2000)
        if (sendMessage) KnitChat.sendMessage("/pc $message")
    }
}