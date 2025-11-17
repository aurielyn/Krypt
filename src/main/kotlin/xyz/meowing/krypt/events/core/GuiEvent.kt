package xyz.meowing.krypt.events.core

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.inventory.AbstractContainerMenu
import xyz.meowing.knit.api.events.Event

sealed class GuiEvent {
    class Open(
        val screen: Screen
    ) : Event()

    class Close(
        val screen: Screen,
        val handler: AbstractContainerMenu
    ) : Event()

    sealed class Render {
        class HUD(
            val context: GuiGraphics
        ) : Event()
    }
}