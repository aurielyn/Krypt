package xyz.meowing.krypt

import net.fabricmc.api.ClientModInitializer
import org.apache.logging.log4j.LogManager
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.managers.feature.FeatureManager

object Krypt : ClientModInitializer {
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
    }
}