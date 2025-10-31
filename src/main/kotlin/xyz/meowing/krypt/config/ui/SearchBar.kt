package xyz.meowing.krypt.config.ui

import xyz.meowing.vexel.animations.EasingType
import xyz.meowing.vexel.animations.colorTo
import xyz.meowing.vexel.elements.TextInput
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.krypt.ui.Theme

class SearchBar(
    private val onSearch: (String) -> Unit
) : VexelElement<SearchBar>() {
    var text: String = ""
        private set

    val input = TextInput(
        placeholder = "Search...",
        fontSize = 20f,
        backgroundColor = Theme.Bg.color,
        borderColor = Theme.Primary.color,
        hoverColor = Theme.BgLight.color,
        pressedColor = Theme.BgDark.color,
        borderRadius = 8f,
        borderThickness = 2f,
        padding = floatArrayOf(8f, 12f, 8f, 12f)
    )
        .setSizing(350f, Size.Pixels, 40f, Size.Pixels)
        .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
        .childOf(this)

    init {
        setSizing(350f, Size.Pixels, 40f, Size.Pixels)
        setPositioning(0f, Pos.ScreenCenter, -50f, Pos.ScreenPixels)
        alignBottom()

        input.onValueChange { newValue ->
            text = newValue as String
            onSearch(text)
        }

        input.onHover(
            { _, _ -> input.background.colorTo(Theme.BgLight.color, 200, EasingType.EASE_OUT) },
            { _, _ -> input.background.colorTo(Theme.Bg.color, 200, EasingType.EASE_IN) }
        )
    }

    override fun onRender(mouseX: Float, mouseY: Float) {}
}