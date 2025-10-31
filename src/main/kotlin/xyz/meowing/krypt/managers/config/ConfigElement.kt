package xyz.meowing.krypt.managers.config

import xyz.meowing.krypt.config.ui.ConfigData
import xyz.meowing.krypt.config.ui.types.ElementType

data class ConfigElement(
    val configKey: String,
    val type: ElementType,
    val shouldShow: (ConfigData) -> Boolean = { true },
    val value: Any? = null,
) {
    var parent: ConfigContainer? = null
}