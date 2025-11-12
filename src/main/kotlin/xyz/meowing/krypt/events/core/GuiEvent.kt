package xyz.meowing.krypt.events.core

import net.minecraft.client.gui.GuiGraphics
import xyz.meowing.knit.api.events.Event

sealed class GuiEvent {
    class RenderHUD(
        val context: GuiGraphics
    ) : Event()
}