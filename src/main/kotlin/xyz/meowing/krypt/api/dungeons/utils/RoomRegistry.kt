package xyz.meowing.krypt.api.dungeons.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import xyz.meowing.krypt.Krypt
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.utils.NetworkUtils
import java.io.File
import java.io.FileNotFoundException

@Module
object RoomRegistry {
    private val byCore = mutableMapOf<Int, RoomMetadata>()
    private val allRooms = mutableListOf<RoomMetadata>()
    private const val ROOM_DATA_URL = "https://raw.githubusercontent.com/Skytils/SkytilsMod/refs/heads/2.x/mod/src/main/resources/assets/catlas/rooms.json"
    private val LOCAL_ROOMS_FILE = File("config/krypt/rooms.json")

    init {
        NetworkUtils.fetchJson<List<RoomMetadata>>(
            url = ROOM_DATA_URL,
            onSuccess = { rooms ->
                populateRooms(rooms)
                Krypt.LOGGER.info("RoomRegistry: Loaded ${rooms.size} rooms from Skytils")
            },
            onError = { error ->
                Krypt.LOGGER.info("RoomRegistry: Failed to load room data — ${error.message}")
                loadFromLocal()
            }
        )
    }

    fun loadFromLocal() {
        runCatching {
            if (!LOCAL_ROOMS_FILE.exists()) throw FileNotFoundException("rooms.json not found in config directory")

            val json = LOCAL_ROOMS_FILE.readText(Charsets.UTF_8)
            val type = object : TypeToken<List<RoomMetadata>>() {}.type
            val rooms: List<RoomMetadata> = Gson().fromJson(json, type)
            populateRooms(rooms)
            Krypt.LOGGER.info("RoomRegistry: Loaded ${rooms.size} rooms from local config")
        }.onFailure {
            Krypt.LOGGER.info("RoomRegistry: Failed to load local room data — ${it.message}")
        }
    }

    private fun populateRooms(rooms: List<RoomMetadata>) {
        allRooms += rooms
        for (room in rooms) {
            for (core in room.cores) {
                byCore[core] = room
            }
        }
    }

    fun getByCore(core: Int): RoomMetadata? = byCore[core]
    fun getAll(): List<RoomMetadata> = allRooms
}