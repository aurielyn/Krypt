@file:Suppress("UNUSED")

package xyz.meowing.krypt.api.dungeons

import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.find
import tech.thatgravyboat.skyblockapi.utils.regex.matchWhen
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.map.Door
import xyz.meowing.krypt.api.dungeons.map.Room
import xyz.meowing.krypt.api.dungeons.map.WorldScanner
import xyz.meowing.krypt.api.dungeons.players.DungeonPlayer
import xyz.meowing.krypt.api.dungeons.players.DungeonPlayerManager
import xyz.meowing.krypt.api.dungeons.score.DungeonScore
import xyz.meowing.krypt.api.dungeons.score.MimicTrigger
import xyz.meowing.krypt.api.dungeons.utils.DungeonClass
import xyz.meowing.krypt.api.dungeons.utils.DungeonFloor
import xyz.meowing.krypt.api.dungeons.utils.DungeonKey
import xyz.meowing.krypt.api.dungeons.utils.MapUtils
import xyz.meowing.krypt.api.dungeons.utils.RoomRegistry
import xyz.meowing.krypt.api.dungeons.utils.WorldScanUtils
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.utils.StringUtils.removeFormatting

@Module
object DungeonAPI {
    private val watcherSpawnedAllRegex = Regex("""\[BOSS] The Watcher: That will be enough for now\.""")
    private val watcherKilledAllRegex = Regex("\\[BOSS] The Watcher: You have proven yourself\\. You may pass\\.")

    private val roomSecretsRegex = Regex("""\b([0-9]|10)/([0-9]|10)\s+Secrets\b""")
    private val dungeonFloorRegex = Regex("The Catacombs \\((?<floor>.+)\\)")

    private val keyObtainedRegex = Regex("(?:\\[.+] ?)?\\w+ has obtained (?<type>\\w+) Key!")
    private val keyPickedUpRegex = Regex("A (?<type>\\w+) Key was picked up!")

    private val witherDoorOpenRegex = Regex("\\w+ opened a WITHER door!")
    private val bloodDoorOpenRegex = Regex("The BLOOD DOOR has been opened!")

    private val startRegex = Regex("\\[NPC] Mort: Here, I found this map when I first entered the dungeon\\.")
    private val endRegex = Regex("\\s+(?:Master Mode|The) Catacombs - (?:Entrance|Floor [XVI]+)")

    private val uniqueClassRegex = Regex("Your .+ stats are doubled because you are the only player using this class!")

    val rooms = Array<Room?>(36) { null }
    val doors = Array<Door?>(60) { null }
    val uniqueRooms = mutableSetOf<Room>()
    val uniqueDoors = mutableSetOf<Door>()
    val discoveredRooms = mutableMapOf<String, DiscoveredRoom>()

    var bloodOpened = false
        private set
    var bloodKilledAll = false
        private set
    var bloodSpawnedAll = false
        private set

    var floorStarted = false
        private set
    var floorCompleted = false
        private set

    var currentRoom: Room? = null
    var holdingLeaps = false
        private set

    var witherKeys = 0
        private set
    var bloodKeys = 0
        private set

    var floor: DungeonFloor? = null
        private set(value) {
            if (field != value) {
                field = value
                EventBus.post(LocationEvent.DungeonFloorChange(value))
            }
        }

    val inBoss: Boolean
        get() = floor != null && KnitPlayer.player?.let {
            val (x, z) = WorldScanUtils.realCoordToComponent(it.x.toInt(), it.z.toInt())
            6 * z + x > 35
        } == true

    var mapLine1 = ""
        private set
    var mapLine2 = ""
        private set

    var uniqueClass = false
        private set

    val mimicDead: Boolean
        get() = MimicTrigger.mimicDead

    val players: Array<DungeonPlayer?>
        get() = DungeonPlayerManager.players

    val score: Int
        get() = DungeonScore.score

    val ownPlayer: DungeonPlayer?
        get() = players.find { it?.name == KnitPlayer.player?.name?.string }

    val dungeonClass: DungeonClass?
        get() = ownPlayer?.dungeonClass

    val classLevel: Int
        get() = ownPlayer?.classLevel ?: 0

    data class DiscoveredRoom(val x: Int, val z: Int, val room: Room)

    init {
        EventBus.registerIn<LocationEvent.AreaChange>(SkyBlockIsland.THE_CATACOMBS) { event ->
            dungeonFloorRegex.find(event.new.name, "floor") { (f) ->
                floor = DungeonFloor.getByName(f)
                floor?.let { EventBus.post(DungeonEvent.Enter(it)) }
            }
        }

        EventBus.register<LocationEvent.IslandChange> { reset() }

        EventBus.registerIn<ChatEvent.Receive>(SkyBlockIsland.THE_CATACOMBS) { event ->
            val message = event.message.string.removeFormatting()

            when {
                watcherSpawnedAllRegex.matches(message) -> {
                    bloodSpawnedAll = true
                }

                watcherKilledAllRegex.matches(message) -> {
                    bloodKilledAll = true
                }

                uniqueClassRegex.matches(message) -> {
                    uniqueClass = true
                }

                endRegex.matches(message) -> {
                    DungeonPlayerManager.updateAllSecrets()
                    floorCompleted = true
                    floor?.let { EventBus.post(DungeonEvent.End(it)) }
                }

                !floorStarted && startRegex.matches(message) -> {
                    floorStarted = true
                    floor?.let { EventBus.post(DungeonEvent.Start(it)) }
                }
            }

            matchWhen(message) {
                case(keyObtainedRegex, "type") { (type) ->
                    handleGetKey(type)
                }
                case(keyPickedUpRegex, "type") { (type) ->
                    handleGetKey(type)
                }
                case(witherDoorOpenRegex) {
                    if (witherKeys > 0) --witherKeys
                }
                case(bloodDoorOpenRegex) {
                    if (bloodKeys > 0) --bloodKeys
                    bloodOpened = true
                }
            }

            if (!event.isActionBar) return@registerIn

            val room = currentRoom ?: return@registerIn
            val match = roomSecretsRegex.find(event.message.string.removeFormatting()) ?: return@registerIn
            val (found, _) = match.destructured
            val secrets = found.toInt()
            if (secrets != room.secretsFound) room.secretsFound = secrets
        }

        EventBus.registerIn<TickEvent.Client>(SkyBlockIsland.THE_CATACOMBS) {
            updateHudLines()
            updateHeldItem()
        }

        RoomRegistry.loadFromRemote()
        WorldScanner.init()
        MapUtils.init()
    }

    /** Clears all dungeon state */
    fun reset() {
        rooms.fill(null)
        doors.fill(null)
        uniqueRooms.clear()
        uniqueDoors.clear()
        discoveredRooms.clear()

        currentRoom = null
        holdingLeaps = false

        bloodKilledAll = false
        bloodSpawnedAll = false
        bloodOpened = false

        floorCompleted = false
        floorStarted = false

        mapLine1 = ""
        mapLine2 = ""

        witherKeys = 0
        bloodKeys = 0

        uniqueClass = false

        WorldScanner.reset()
        DungeonPlayerManager.reset()
        DungeonScore.reset()
        MapUtils.reset()
    }

    private fun handleGetKey(type: String) {
        val key = DungeonKey.getById(type) ?: return
        when (key) {
            DungeonKey.WITHER -> ++witherKeys
            DungeonKey.BLOOD -> ++bloodKeys
        }
        EventBus.post(DungeonEvent.KeyPickUp(key))
    }

    private fun updateHudLines() {
        val run = DungeonScore.data

        val dSecrets = "§7Secrets: §b${run.secretsFound}§8-§e${run.secretsRemaining}§8-§c${run.totalSecrets}"
        val dCrypts = "§7Crypts: " + when {
            run.crypts >= 5 -> "§a${run.crypts}"
            run.crypts > 0 -> "§e${run.crypts}"
            else -> "§c0"
        }
        val dMimic = if (floor?.floorNumber in listOf(6, 7)) {
            "§7Mimic: " + if (MimicTrigger.mimicDead) "§a✔" else "§c✘"
        } else ""

        val minSecrets = "§7Min Secrets: " + when {
            run.secretsFound == 0 -> "§b?"
            run.minSecrets > run.secretsFound -> "§e${run.minSecrets}"
            else -> "§a${run.minSecrets}"
        }

        val dDeaths = "§7Deaths: " + if (run.teamDeaths > 0) "§c${run.teamDeaths}" else "§a0"
        val dScore = "§7Score: " + when {
            run.score >= 300 -> "§a${run.score}"
            run.score >= 270 -> "§e${run.score}"
            else -> "§c${run.score}"
        } + if (DungeonScore.hasPaul) " §b★" else ""

        mapLine1 = "$dSecrets    $dCrypts    $dMimic".trim()
        mapLine2 = "$minSecrets    $dDeaths    $dScore".trim()
    }

    /** Updates leap detection based on held item */
    private fun updateHeldItem() {
        val item = KnitPlayer.player?.mainHandStack ?: return
        holdingLeaps = "leap" in item.name.string.removeFormatting().lowercase()
    }

    // Room accessors
    fun getRoomIdx(comp: Pair<Int, Int>) = 6 * comp.second + comp.first
    fun getRoomAtIdx(idx: Int) = rooms.getOrNull(idx)
    fun getRoomAtComp(comp: Pair<Int, Int>) = getRoomAtIdx(getRoomIdx(comp))
    fun getRoomAt(x: Int, z: Int) = getRoomAtComp(WorldScanUtils.realCoordToComponent(x, z))

    // Door accessors
    fun getDoorIdx(comp: Pair<Int, Int>): Int {
        val base = ((comp.first - 1) shr 1) + 6 * comp.second
        return base - (base / 12)
    }

    fun getDoorAtIdx(idx: Int) = doors.getOrNull(idx)
    fun getDoorAtComp(comp: Pair<Int, Int>) = getDoorAtIdx(getDoorIdx(comp))
    fun getDoorAt(x: Int, z: Int) = getDoorAtComp(WorldScanUtils.realCoordToComponent(x, z))

    /** Adds a door to the map and tracks it as unique */
    fun addDoor(door: Door) {
        val idx = getDoorIdx(door.getComp())
        if (idx in doors.indices) {
            doors[idx] = door
            uniqueDoors += door
        }
    }

    /** Merges two rooms into one unified instance */
    fun mergeRooms(room1: Room, room2: Room) {
        uniqueRooms.remove(room2)
        for (comp in room2.components) {
            if (!room1.hasComponent(comp.first, comp.second)) {
                room1.addComponent(comp, update = false)
            }
            val idx = getRoomIdx(comp)
            if (idx in rooms.indices) rooms[idx] = room1
        }
        uniqueRooms += room1
        room1.update()
    }
}