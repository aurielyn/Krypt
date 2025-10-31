package xyz.meowing.krypt

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import org.apache.logging.log4j.LogManager
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.text.KnitText
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.managers.feature.FeatureManager

object Krypt : ClientModInitializer {
    private var showLoad = true

    @JvmStatic
    val prefix = "§7[§cKrypt§7]"

    @JvmStatic
    var LOGGER = LogManager.getLogger("krypt")

    @Target(AnnotationTarget.CLASS)
    annotation class Module

    @Target(AnnotationTarget.CLASS)
    annotation class Command

    override fun onInitializeClient() {
        ConfigManager.createConfigUI()
        FeatureManager.loadFeatures()
        FeatureManager.initializeFeatures()
        ConfigManager.executePending()

        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            if (!showLoad) return@register

            val loadMessage = KnitText
                .literal("$prefix §fMod loaded.")
                .onHover("§c${FeatureManager.moduleCount} modules §8- §c${FeatureManager.loadTime}ms §8- §c${FeatureManager.commandCount} commands")

            KnitChat.fakeMessage(loadMessage)

            showLoad = false
        }
    }
}