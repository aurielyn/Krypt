package xyz.meowing.krypt.events.core

import net.minecraft.client.gui.GuiGraphics
import xyz.meowing.knit.api.events.Event

sealed class GuiEvent {
    sealed class Render {
        class HUD(
            val context: GuiGraphics
        ) : Event()
    }
}