package xyz.meowing.krypt.api.dungeons.players

import net.minecraft.entity.player.PlayerEntity
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.krypt.api.dungeons.map.MapScanner.RoomClearInfo
import xyz.meowing.krypt.api.dungeons.map.Room
import xyz.meowing.krypt.api.dungeons.utils.DungeonClass
import xyz.meowing.krypt.api.hypixel.HypixelAPI
import java.util.*
import java.util.concurrent.CompletableFuture

class DungeonPlayer(val name: String) {
    // position
    var iconX: Double? = null
    var iconZ: Double? = null
    var realX: Double? = null
    var realZ: Double? = null
    var yaw: Float? = null

    // score stuff
    var deaths = 0
    var minRooms = 0
    var maxRooms = 0
    var dclass = DungeonClass.UNKNOWN
    val alive get() = dclass != DungeonClass.DEAD;

    private var initSecrets: Int? = null
    private var currSecrets: Int? = null
    val secrets get() = (currSecrets ?: initSecrets ?: 0) - (initSecrets ?: 0)

    // api
    var uuid: UUID? = null
    var inRender = false

    var currRoom: Room? = null
    var lastRoom: Room? = null

    val clearedRooms = mutableMapOf(
        "WHITE" to mutableMapOf<String, RoomClearInfo>(),
        "GREEN" to mutableMapOf<String, RoomClearInfo>()
    )

    init {
        uuid = findPlayerUUID(name)

        HypixelAPI.fetchSecrets(uuid.toString(), 120_000) { secrets ->
            initSecrets = secrets
            currSecrets = secrets
        }
    }

    private fun findPlayerUUID(name: String): UUID? {
        val world = KnitClient.world ?: return null
        return world.entities
            .asSequence()
            .filterIsInstance<PlayerEntity>()
            .find { it.gameProfile.name == name }
            ?.uuid
    }

    fun updateSecrets() {
        if (uuid == null) return

        HypixelAPI.fetchSecrets(uuid.toString(), cacheMs = 0) { secrets ->
            currSecrets = secrets
        }
    }

    fun getGreenChecks(): MutableMap<String, RoomClearInfo> = clearedRooms["GREEN"] ?: mutableMapOf()
    fun getWhiteChecks(): MutableMap<String, RoomClearInfo> = clearedRooms["WHITE"] ?: mutableMapOf()
}