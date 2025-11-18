package xyz.meowing.krypt.utils

import net.minecraft.network.chat.Component
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.text.KnitText
import xyz.meowing.knit.api.text.internal.TextBuilder

fun KnitChat.modMessage(message: Component) {
    val prefix = buildKryptPrefix().build()
    val space = Component.literal(" ")
    val fullMessage = prefix.copy().append(space).append(message)
    this.fakeMessage(fullMessage)
}

fun KnitChat.modMessage(message: String) {
    val prefix = buildKryptPrefix().build()
    val space = Component.literal(" ")
    val textComponent = Component.literal(message)
    val fullMessage = prefix.copy().append(space).append(textComponent)
    this.fakeMessage(fullMessage)
}

fun KnitChat.modMessage(message: TextBuilder) {
    val prefix = buildKryptPrefix().build()
    val space = Component.literal(" ")
    val fullMessage = prefix.copy().append(space).append(message.build())
    this.fakeMessage(fullMessage)
}

private fun buildKryptPrefix(): TextBuilder {
    val bracket1 = Component.literal("[").withColor(0x45AC45)
    val k = Component.literal("K").withColor(0x60B571)
    val r = Component.literal("r").withColor(0x7ABD9C)
    val y = Component.literal("y").withColor(0x8BBB8C)
    val p = Component.literal("p").withColor(0x7ABD9C)
    val t = Component.literal("t").withColor(0x60B571)
    val bracket2 = Component.literal("]").withColor(0x45AC45)

    val prefix = bracket1
        .copy()
        .append(k)
        .append(r)
        .append(y)
        .append(p)
        .append(t)
        .append(bracket2)

    return KnitText.fromVanilla(prefix)
}