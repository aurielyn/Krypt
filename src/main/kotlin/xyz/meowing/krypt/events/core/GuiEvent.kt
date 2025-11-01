package xyz.meowing.krypt.events.core

import net.minecraft.client.gui.DrawContext
import xyz.meowing.knit.api.events.Event

sealed class GuiEvent {
    class RenderHUD(
        val context: DrawContext
    ) : Event()
}