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
        ConfigManager.addFeature(
            "Mimic alert",
            "Mimic alert",
            "Alerts",
            ConfigElement(
                "mimicAlert",
                ElementType.Switch(false)
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
    private val enabled by ConfigDelegate<Boolean>("mimicAlert")

    private const val DURATION = 2000

    override fun initialize() {

    }

    fun displayTitle() {
        if(!enabled) return

        TitleUtils.showTitle(message, duration = DURATION)
        if(sendMessage) sendChatMessage()
    }

    fun sendChatMessage() {
        KnitChat.sendMessage("/pc $message")
    }
}