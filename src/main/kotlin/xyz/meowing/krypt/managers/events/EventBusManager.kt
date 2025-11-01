package xyz.meowing.krypt.managers.events

import xyz.meowing.knit.api.events.EventCall
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.LocationAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.LocationEvent

@Module
object EventBusManager {
    private data class ConditionalEventCall(
        val islands: Set<SkyBlockIsland>?,
        val skyblockOnly: Boolean,
        val eventCall: EventCall,
        var isRegistered: Boolean = false
    )

    private val conditionalEventCalls = mutableListOf<ConditionalEventCall>()

    init {
        EventBus.register<LocationEvent.IslandChange> {
            updateRegistrations()
        }

        EventBus.register<LocationEvent.ServerChange> {
            updateRegistrations()
        }
    }

    private fun updateRegistrations() {
        val currentIsland = LocationAPI.island
        val onSkyblock = LocationAPI.isOnSkyBlock

        conditionalEventCalls.forEach { call ->
            val shouldBeRegistered = when {
                call.skyblockOnly && !onSkyblock -> false
                call.islands != null -> currentIsland in call.islands
                else -> true
            }

            if (shouldBeRegistered && !call.isRegistered) {
                call.eventCall.register()
                call.isRegistered = true
            } else if (!shouldBeRegistered && call.isRegistered) {
                call.eventCall.unregister()
                call.isRegistered = false
            }
        }
    }

    fun trackConditionalEvent(islands: Set<SkyBlockIsland>?, skyblockOnly: Boolean, eventCall: EventCall) {
        conditionalEventCalls.add(ConditionalEventCall(islands, skyblockOnly, eventCall, false))
        updateRegistrations()
    }
}

