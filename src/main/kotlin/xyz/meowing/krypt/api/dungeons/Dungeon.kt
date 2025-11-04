package xyz.meowing.krypt.api.dungeons

import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.find
import tech.thatgravyboat.skyblockapi.utils.regex.matchWhen
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.knit.internal.events.TickEvent
import xyz.meowing.krypt.api.dungeons.map.Door
import xyz.meowing.krypt.api.dungeons.map.Room
import xyz.meowing.krypt.api.dungeons.map.WorldScanner
import xyz.meowing.krypt.api.dungeons.players.DungeonPlayerManager
import xyz.meowing.krypt.api.dungeons.score.DungeonScore
import xyz.meowing.krypt.api.dungeons.score.MimicTrigger
import xyz.meowing.krypt.api.dungeons.utils.DungeonFloor
import xyz.meowing.krypt.api.dungeons.utils.DungeonKey
import xyz.meowing.krypt.api.dungeons.utils.MapUtils
import xyz.meowing.krypt.api.dungeons.utils.RoomRegistry
import xyz.meowing.krypt.api.dungeons.utils.WorldScanUtils
import xyz.meowing.krypt.api.location.LocationAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.utils.StringUtils.removeFormatting

/**
 * Central dungeon state manager.
 * Basically one-stop shop for everything dungeons
 */
object Dungeon {

    // Regex patterns for chat parsing
    private val watcherRegex = Regex("""\[BOSS] The Watcher: That will be enough for now\.""")
    private val dungeonCompleteRegex = Regex("""^\s*(Master Mode)?\s?(?:The)? Catacombs - (Entrance|Floor .{1,3})$""")
    private val roomSecretsRegex = Regex("""\b([0-9]|10)/([0-9]|10)\s+Secrets\b""")
    private val dungeonFloorRegex = Regex("The Catacombs \\((?<floor>.+)\\)")
    private val keyObtainedRegex = Regex("(?:\\[.+] ?)?\\w+ has obtained (?<type>\\w+) Key!")
    private val keyPickedUpRegex = Regex("A (?<type>\\w+) Key was picked up!")
    private val witherDoorOpenRegex = Regex("\\w+ opened a WITHER door!")
    private val bloodDoorOpenRegex = Regex("The BLOOD DOOR has been opened!")

    // Room and door data
    val rooms = Array<Room?>(36) { null }
    val doors = Array<Door?>(60) { null }
    val uniqueRooms = mutableSetOf<Room>()
    val uniqueDoors = mutableSetOf<Door>()
    val discoveredRooms = mutableMapOf<String, DiscoveredRoom>()

    // Dungeon state
    var bloodOpened = false
    var bloodClear = false
    var bloodDone = false
    var complete = false
    var currentRoom: Room? = null
    var holdingLeaps = false
    var witherKeys: Int = 0
    var bloodKeys: Int = 0

    // Floor info
    var floor: DungeonFloor? = null
    var inDungeon = false
    val inBoss: Boolean
        get() = floor != null && KnitPlayer.player?.let {
            val (x, z) = WorldScanUtils.realCoordToComponent(it.x.toInt(), it.z.toInt())
            6 * z + x > 35
        } == true

    // HUD lines
    var mapLine1 = ""
    var mapLine2 = ""

    // Shortcuts
    val players get() = DungeonPlayerManager.players
    val score get() = DungeonScore.score

    data class DiscoveredRoom(val x: Int, val z: Int, val room: Room)

    /** Initializes all dungeon systems and event listeners */
    fun init() {
        EventBus.registerIn<LocationEvent.AreaChange>(SkyBlockIsland.THE_CATACOMBS) { event ->
            dungeonFloorRegex.find(event.new.name, "floor") { (f) ->
                floor = DungeonFloor.getByName(f)
                val floorValue = floor ?: return@find
                EventBus.post(DungeonEvent.Enter(floorValue))
            }
        }

        EventBus.register<LocationEvent.IslandChange> {
            inDungeon = LocationAPI.island == SkyBlockIsland.THE_CATACOMBS
            if (!inDungeon) reset()
        }


        EventBus.registerIn<ChatEvent.Receive>(SkyBlockIsland.THE_CATACOMBS) { event ->
            val msg = event.message.string.removeFormatting()
            if (watcherRegex.containsMatchIn(msg)) bloodDone = true
            if (dungeonCompleteRegex.containsMatchIn(msg)) {
                DungeonPlayerManager.updateAllSecrets()
                complete = true
                return@registerIn
            }

            matchWhen(msg) {
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

        EventBus.registerIn<TickEvent.Client.Start>(SkyBlockIsland.THE_CATACOMBS){
            updateHudLines()
            updateHeldItem()
        }

        RoomRegistry.loadFromRemote()
        WorldScanner.init()
        DungeonPlayerManager.init()
        DungeonScore.init()
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
        bloodClear = false
        bloodDone = false
        bloodOpened = false
        complete = false
        holdingLeaps = false
        mapLine1 = ""
        mapLine2 = ""
        witherKeys = 0
        bloodKeys = 0
        WorldScanner.reset()
        DungeonPlayerManager.reset()
        DungeonScore.reset()
        MapUtils.reset()
    }

    /** Handles Key Events **/
    private fun handleGetKey(type: String) {
         when {
            type.equals("wither", true) -> {
                ++witherKeys
                DungeonKey.getById(type)?.let { EventBus.post(DungeonEvent.KeyPickUp(it)) }
            }
            type.equals("blood", true) -> {
                ++bloodKeys
                DungeonKey.getById(type)?.let { EventBus.post(DungeonEvent.KeyPickUp(it)) }
            }
        }
    }

    /** Updates HUD lines for map overlay */
    private fun updateHudLines() {
        val run = DungeonScore.data

        val dSecrets = "§7Secrets: §b${run.secretsFound}§8-§e${run.secretsRemaining}§8-§c${run.totalSecrets}"
        val dCrypts = "§7Crypts: " + when {
            run.crypts >= 5 -> "§a${run.crypts}"
            run.crypts > 0  -> "§e${run.crypts}"
            else            -> "§c0"
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
            else             -> "§c${run.score}"
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