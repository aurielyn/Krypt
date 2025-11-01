package xyz.meowing.krypt.events

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import xyz.meowing.knit.Knit
import xyz.meowing.knit.api.events.Event
import xyz.meowing.knit.api.events.EventBus
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.knit.internal.events.TickEvent
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.ServerEvent
import xyz.meowing.krypt.managers.events.EventBusManager

@Module
object EventBus : EventBus(true) {
    init {
        ClientReceiveMessageEvents.ALLOW_GAME.register { message, isActionBar ->
            !post(ChatEvent.Receive(message, isActionBar))
        }

        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            post(ServerEvent.Connect())
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            post(ServerEvent.Disconnect())
        }

        Knit.EventBus.register<TickEvent.Client.Start> { TickScheduler.Client.onTick() }

        Knit.EventBus.register<TickEvent.Server.Start> { TickScheduler.Server.onTick() }
    }

    inline fun <reified T : Event> registerIn(
        vararg islands: SkyBlockIsland,
        skyblockOnly: Boolean = false,
        noinline callback: (T) -> Unit
    ) {
        val eventCall = register<T>(add = false, callback = callback)
        val islandSet = if (islands.isNotEmpty()) islands.toSet() else null
        EventBusManager.trackConditionalEvent(islandSet, skyblockOnly, eventCall)
    }
}