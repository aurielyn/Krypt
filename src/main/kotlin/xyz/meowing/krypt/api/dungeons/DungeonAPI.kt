@file:Suppress("UNUSED")

package xyz.meowing.krypt.api.dungeons

import net.minecraft.item.map.MapState
import tech.thatgravyboat.skyblockapi.api.data.Candidate
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import tech.thatgravyboat.skyblockapi.utils.extentions.parseDuration
import tech.thatgravyboat.skyblockapi.utils.extentions.parseRomanOrArabic
import tech.thatgravyboat.skyblockapi.utils.extentions.toIntValue
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.find
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findThenNull
import tech.thatgravyboat.skyblockapi.utils.regex.matchWhen
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.core.enums.DungeonClass
import xyz.meowing.krypt.api.dungeons.core.enums.DungeonFloor
import xyz.meowing.krypt.api.dungeons.core.enums.DungeonKey
import xyz.meowing.krypt.api.dungeons.core.enums.DungeonPlayer
import xyz.meowing.krypt.api.dungeons.core.map.RoomState
import xyz.meowing.krypt.api.dungeons.core.map.Tile
import xyz.meowing.krypt.api.dungeons.core.map.UniqueRoom
import xyz.meowing.krypt.api.dungeons.core.map.Unknown
import xyz.meowing.krypt.api.dungeons.core.utils.MapUtils
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.PlayerEvent
import xyz.meowing.krypt.events.core.ScoreboardEvent
import xyz.meowing.krypt.events.core.TablistEvent
import kotlin.time.Duration

@Module
object DungeonAPI {
    private val cryptsRegex = Regex("^ Crypts: (?<count>\\d+)$")
    private val dungeonFloorRegex = Regex("The Catacombs \\((?<floor>.+)\\)")
    private val timeRegex = Regex("Time Elapsed: (?<time>[\\dhms ]+)")
    private val roomIdRegex = Regex("\\d+/\\d+/\\d+ .+? (?<id>.+)")
    private val partyAmountRegex = Regex("\\s*Party \\((?<amount>\\d+)\\)")
    private val milestoneRegex = Regex("\\s*Your Milestone: ☠(?<milestone>.)")

    private val classRegex = Regex("(?:\\[.+] ?)*(?<name>\\S+) .*\\((?<class>\\S+) (?<level>.+)\\)")
    private val deadTeammateRegex = Regex("\\[.+] (?<name>\\S+) .*\\(DEAD\\)")
    private val deathRegex = Regex("^ ☠ (?:You were|(?<username>\\w+)) (?<reason>.+?)(?: and became a ghost)?\\.$")

    private val puzzleRegex = Regex(" (.+): \\[[✦✔✖].+")

    private val keyObtainedRegex = Regex("(?:\\[.+] ?)?\\w+ has obtained (?<type>\\w+) Key!")
    private val keyPickedUpRegex = Regex("A (?<type>\\w+) Key was picked up!")
    private val witherDoorOpenRegex = Regex("(?:\\[.+] )?(?<name>\\w+) opened a WITHER door!")
    private val bloodDoorOpenRegex = Regex("The BLOOD DOOR has been opened!")

    private val watcherSpawnRegex = Regex("\\[BOSS] The Watcher: That will be enough for now\\.")
    private val watcherClearRegex = Regex("\\[BOSS] The Watcher: You have proven yourself\\. You may pass\\.")

    private val startRegex = Regex("\\[NPC] Mort: Here, I found this map when I first entered the dungeon\\.")
    private val endRegex = Regex("\\s+(?:Master Mode|The) Catacombs - (?:Entrance|Floor [XVI]+)")
    private val uniqueClassRegex = Regex("Your .+ stats are doubled because you are the only player using this class!")

    var cryptCount: Int = 0
        private set
    var dungeonFloor: DungeonFloor? = null
        private set(value) {
            if (field != value) {
                field = value
                EventBus.post(LocationEvent.DungeonFloorChange(value))
            }
        }
    var milestone: Int = 0
        private set
    var partySize: Int = 0
        private set
    var roomId: String? = null
        private set
    var time: Duration = Duration.ZERO
        private set

    var ownPlayer: DungeonPlayer? = null
        private set
    var teammates: List<DungeonPlayer> = emptyList()
        private set
    var lastDoorOpener: DungeonPlayer? = null
        private set
    val dungeonClass: DungeonClass?
        get() = ownPlayer?.dungeonClass
    val classLevel: Int
        get() = ownPlayer?.classLevel ?: 0

    data class Puzzle(val name: String, var state: RoomState)
    var puzzles = mutableListOf<Puzzle>()
        private set

    var witherKeys: Int = 0
        private set
    var bloodKeys: Int = 0
        private set
    var bloodOpened: Boolean = false
        private set

    var started: Boolean = false
        private set
    var completed: Boolean = false
        private set
    var inBoss: Boolean = false
        private set
    var uniqueClass: Boolean = false
        private set

    var dungeonStartTime: Long? = null
        private set
    var bloodOpenTime: Long? = null
        private set
    var watcherSpawnTime: Long? = null
        private set
    var watcherClearTime: Long? = null
        private set
    var bossEntryTime: Long? = null
        private set
    var dungeonEndTime: Long? = null
        private set

    val isPaul: Boolean
        get() = Candidate.PAUL.isActive

    val dungeonList = Array<Tile>(121) { Unknown(0, 0) }
    val uniqueRooms = mutableSetOf<UniqueRoom>()
    var roomCount = 0

    var trapType: String? = null
    var witherDoors = 0
    var secretCount = 0

    var mapData: MapState? = null

    init {
        EventBus.register<LocationEvent.WorldChange> { reset() }

        EventBus.registerIn<TablistEvent.Change>(SkyBlockIsland.THE_CATACOMBS) { event ->
            val firstColumn = event.new.firstOrNull() ?: return@registerIn
            val first = firstColumn.firstOrNull() ?: return@registerIn

            partyAmountRegex.find(first.stripped, "amount") { (amount) ->
                this.partySize = amount.toIntValue()
            }

            val ownName = KnitPlayer.name

            for (line in firstColumn) {
                val stripped = line.stripped

                classRegex.findThenNull(stripped, "name", "class", "level") { (name, dungeonClass, level) ->
                    val dungeonPlayer = teammates.find { it.name == name }

                    if (dungeonPlayer != null) {
                        if (name != ownName) dungeonPlayer.dead = false

                        if (dungeonPlayer.missingData()) {
                            dungeonPlayer.dungeonClass = DungeonClass.getByName(dungeonClass)
                            dungeonPlayer.classLevel = level.parseRomanOrArabic()
                        }

                        return@findThenNull
                    }

                    val playerClass = DungeonClass.getByName(dungeonClass)
                    val player = DungeonPlayer(name, playerClass, level.parseRomanOrArabic())

                    if (name == ownName) ownPlayer = player

                    teammates += player
                } ?: continue

                teammates.filterNot { it.dead }.forEachIndexed { i, teammate ->
                    teammate.mapIcon.icon = "icon-$i"
                }

                deadTeammateRegex.find(stripped, "name") { (name) ->
                    var dungeonPlayer = teammates.find { it.name == name }

                    if (dungeonPlayer == null) {
                        dungeonPlayer = DungeonPlayer(name, null, null)
                        teammates += dungeonPlayer
                    }

                    dungeonPlayer.dead = true
                }
            }

            val secondColumn = event.new.getOrNull(1) ?: return@registerIn

            for (line in secondColumn) {
                val stripped = line.stripped

                milestoneRegex.find(stripped, "milestone") { (milestone) ->
                    this.milestone = milestoneCharToInt(milestone.first())
                }

                cryptsRegex.find(stripped, "count") { (count) ->
                    cryptCount = count.toIntOrNull() ?: cryptCount
                }
            }

            event.new.forEach { column ->
                column.forEach { line ->
                    val stripped = line.stripped
                    puzzleRegex.find(stripped)?.let { match ->
                        val name = match.groupValues[1].takeUnless { "?" in it } ?: return@let
                        val state = when {
                            "✔" in stripped -> RoomState.DISCOVERED
                            "✖" in stripped -> RoomState.FAILED
                            "✦" in stripped -> RoomState.DISCOVERED
                            else -> RoomState.UNOPENED
                        }

                        val puzzle = puzzles.find { it.name == name }
                        if (puzzle != null) {
                            puzzle.state = state
                        } else {
                            puzzles.add(Puzzle(name, state))
                        }
                    }
                }
            }
        }

        EventBus.registerIn<LocationEvent.AreaChange>(SkyBlockIsland.THE_CATACOMBS) { event ->
            dungeonFloorRegex.find(event.new.name, "floor") { (floor) ->
                dungeonFloor = DungeonFloor.getByName(floor)
                val floorValue = dungeonFloor ?: return@find
                EventBus.post(DungeonEvent.Enter(floorValue))
            }
        }

        EventBus.registerIn<ScoreboardEvent.Update>(SkyBlockIsland.THE_CATACOMBS) { event ->
            for (line in event.added) {
                timeRegex.findThenNull(line, "time") { (time) ->
                    this.time = time.parseDuration() ?: return@findThenNull
                } ?: continue

                roomIdRegex.findThenNull(line, "id") { (roomId) ->
                    this.roomId = roomId
                } ?: continue
            }
        }

        EventBus.registerIn<ChatEvent.Receive>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if (event.isActionBar) return@registerIn
            val message = event.message.string

            if (!started && startRegex.matches(message)) {
                started = true
                dungeonStartTime = System.currentTimeMillis()
                dungeonFloor?.let { EventBus.post(DungeonEvent.Start(it)) }
                return@registerIn
            }

            if (uniqueClassRegex.matches(message)) {
                uniqueClass = true
                return@registerIn
            }

            if (watcherClearRegex.matches(message)) {
                watcherClearTime = System.currentTimeMillis()
                return@registerIn
            }

            if (watcherSpawnRegex.matches(message)) {
                watcherSpawnTime = System.currentTimeMillis()
                return@registerIn
            }

            if (started && endRegex.matches(message)) {
                completed = true
                dungeonEndTime = System.currentTimeMillis()
                return@registerIn
            }

            deathRegex.findThenNull(message, "username", "reason") { (username, reason) ->
                val name = username.takeIf { it.isNotEmpty() } ?: KnitPlayer.name
                teammates.find { it.name == name }?.dead = true
            } ?: return@registerIn

            matchWhen(message) {
                case(keyObtainedRegex, "type") { (type) ->
                    handleGetKey(type)
                }
                case(keyPickedUpRegex, "type") { (type) ->
                    handleGetKey(type)
                }
                case(witherDoorOpenRegex, "name") { (name) ->
                    if (witherKeys > 0) --witherKeys
                    lastDoorOpener = teammates.find { it.name == name }
                }
                case(bloodDoorOpenRegex) {
                    if (bloodKeys > 0) --bloodKeys
                    bloodOpened = true
                }
            }
        }

        EventBus.registerIn<PlayerEvent.HotbarChange>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if (event.slot != 0) return@registerIn

            val id = event.item.getData(DataTypes.ID)
            ownPlayer?.dead = id == "HAUNT_ABILITY"
        }
    }

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

    private fun milestoneCharToInt(char: Char): Int = if (char in '❶'..'❾') '❶'.code - char.code + 1 else 0

    private fun reset() {
        dungeonFloor = null
        ownPlayer = null
        uniqueClass = false
        started = false
        completed = false
        inBoss = false
        milestone = 0
        partySize = 0
        cryptCount = 0
        teammates = emptyList()
        time = Duration.ZERO
        roomId = null
        witherKeys = 0
        bloodKeys = 0
        bloodOpened = false
        dungeonStartTime = null
        bloodOpenTime = null
        watcherClearTime = null
        watcherSpawnTime = null
        bossEntryTime = null
        dungeonEndTime = null
        lastDoorOpener = null
        dungeonList.fill(Unknown(0, 0))
        roomCount = 0
        trapType = ""
        witherDoors = 0
        secretCount = 0
        mapData = null
        uniqueRooms.clear()
        puzzles.clear()

        MapUtils.reset()
    }
}