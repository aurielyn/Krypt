package xyz.meowing.krypt

import net.fabricmc.api.ClientModInitializer
import org.apache.logging.log4j.LogManager
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.text.KnitText
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.ServerEvent
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.managers.feature.FeatureManager

object Krypt : ClientModInitializer {
    private var showLoad = true

    @JvmStatic
    val prefix = "§7[§dKrypt§7]"

    @JvmStatic
    var LOGGER = LogManager.getLogger("krypt")

    @JvmStatic
    val NAMESPACE = "krypt"

    override fun onInitializeClient() {
        ConfigManager.createConfigUI()
        FeatureManager.loadFeatures()
        FeatureManager.initializeFeatures()
        ConfigManager.executePending()

        EventBus.register<ServerEvent.Connect> {
            if (!showLoad) return@register

            val loadMessage = KnitText
                .literal("$prefix §fMod loaded.")
                .onHover("§d${FeatureManager.moduleCount} modules §8- §d${FeatureManager.loadTime}ms §8- §d${FeatureManager.commandCount} commands")

            KnitChat.fakeMessage(loadMessage)

            showLoad = false
        }
    }
}