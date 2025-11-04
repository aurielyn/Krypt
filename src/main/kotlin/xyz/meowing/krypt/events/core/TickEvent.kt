package xyz.meowing.krypt.events.core

import xyz.meowing.knit.api.events.Event

sealed class TickEvent {
    class Client: Event()
    class Server: Event()
}