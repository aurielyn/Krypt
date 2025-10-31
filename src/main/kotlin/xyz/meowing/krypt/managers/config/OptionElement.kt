package xyz.meowing.krypt.managers.config

class OptionElement(
    val optionName: String,
    val description: String = "",
    val optionsSection: String = "Options",
    val configElement: ConfigElement
) : ConfigContainer
