package xyz.meowing.krypt.config.ui.types

import xyz.meowing.krypt.config.ui.elements.MCColorCode
import java.awt.Color

sealed class ElementType {
    data class Button(val text: String, val onClick: () -> Unit) : ElementType()
    data class Switch(val default: Boolean) : ElementType()
    data class Slider(val min: Double, val max: Double, val default: Double, val showDouble: Boolean) : ElementType()
    data class Dropdown(val options: List<String>, val default: Int) : ElementType()
    data class TextInput(val default: String, val placeholder: String = "", val maxLength: Int = Int.MAX_VALUE) : ElementType()
    data class TextParagraph(val text: String) : ElementType()
    data class ColorPicker(val default: Color) : ElementType()
    data class Keybind(val default: Int) : ElementType()
    data class MultiCheckbox(val options: List<String>, val default: Set<Int> = emptySet()) : ElementType()
    data class MCColorPicker(val default: MCColorCode) : ElementType()
}