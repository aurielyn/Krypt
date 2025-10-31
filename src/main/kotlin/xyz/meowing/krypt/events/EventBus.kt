package xyz.meowing.krypt.events

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import xyz.meowing.knit.api.events.EventBus
import xyz.meowing.krypt.events.core.ChatEvent

object EventBus : EventBus(true) {
    init {
        ClientReceiveMessageEvents.ALLOW_GAME.register { message, isActionBar ->
            !post(ChatEvent.Receive(message, isActionBar))
        }

        SkyBlockAPI.eventBus.register(this)
    }
}