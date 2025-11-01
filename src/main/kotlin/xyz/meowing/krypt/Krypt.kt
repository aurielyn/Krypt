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
    val prefix = "§7[§aKrypt§7]"

    @JvmStatic
    var LOGGER = LogManager.getLogger("krypt")

    override fun onInitializeClient() {
        ConfigManager.createConfigUI()
        FeatureManager.loadFeatures()
        FeatureManager.initializeFeatures()
        ConfigManager.executePending()

        EventBus.register<ServerEvent.Connect> {
            if (!showLoad) return@register

            val loadMessage = KnitText
                .literal("$prefix §fMod loaded.")
                .onHover("§a${FeatureManager.moduleCount} modules §8- §a${FeatureManager.loadTime}ms §8- §a${FeatureManager.commandCount} commands")

            KnitChat.fakeMessage(loadMessage)

            showLoad = false
        }
    }
}