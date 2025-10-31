package xyz.meowing.krypt

import net.fabricmc.api.ClientModInitializer
import xyz.meowing.knit.api.events.EventBus

object Krypt : ClientModInitializer {
    val eventBus = EventBus(true)

    override fun onInitializeClient() {}
}