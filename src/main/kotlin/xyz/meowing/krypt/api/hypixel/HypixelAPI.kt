package xyz.meowing.krypt.api.hypixel

import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.fabric.event.HypixelModAPICallback
import net.hypixel.modapi.packet.impl.clientbound.ClientboundHelloPacket
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket
import xyz.meowing.knit.api.scheduler.TimeScheduler
import xyz.meowing.krypt.Krypt
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.utils.NetworkUtils
import kotlin.jvm.optionals.getOrNull

@Module
object HypixelAPI {
    init {
        HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket::class.java)
        HypixelModAPICallback.EVENT.register { event ->
            when (event) {
                is ClientboundLocationPacket -> {
                    EventBus.post(LocationEvent.ServerChange(
                        event.serverName,
                        event.serverType.getOrNull(),
                        event.lobbyName.getOrNull(),
                        event.mode.getOrNull(),
                        event.map.getOrNull(),
                    ))
                }

                is ClientboundHelloPacket -> {
                    EventBus.post(LocationEvent.HypixelJoin(event.environment))
                }
            }
        }
    }

    fun fetchSecrets(uuid: String, cacheMs: Long, onResult: (Int) -> Unit) {
        if (SecretsCache.isFresh(uuid, cacheMs)) {
            SecretsCache.get(uuid)?.let(onResult)
            return
        }

        NetworkUtils.fetchJson<Int>(
            url = "https://api.tenios.dev/secrets/$uuid",
            headers = mapOf("User-Agent" to "Krypt"),
            onSuccess = { secrets ->
                SecretsCache.put(uuid, secrets)
                onResult(secrets)
            },
            onError = { error ->
                Krypt.LOGGER.error("Failed to fetch secrets for $uuid: ${error.message}")
                onResult(0)
            }
        )
    }

    object SecretsCache {
        private val data = mutableMapOf<String, Pair<Long, Int>>() // UUID → (timestamp, secrets)
        private const val EXPIRY_MS = 5 * 60 * 1000L

        init {
            TimeScheduler.repeat(60_000L) { cleanup() }
        }

        fun cleanup() {
            val now = System.currentTimeMillis()
            data.entries.removeIf { now - it.value.first > EXPIRY_MS }
        }

        fun get(uuid: String): Int? = data[uuid]?.second

        fun put(uuid: String, secrets: Int) {
            data[uuid] = System.currentTimeMillis() to secrets
        }

        fun isFresh(uuid: String, cacheMs: Long): Boolean {
            val timestamp = data[uuid]?.first ?: return false
            return System.currentTimeMillis() - timestamp < cacheMs
        }
    }
}