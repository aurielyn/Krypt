package xyz.meowing.krypt.api.dungeons.score

import tech.thatgravyboat.skyblockapi.api.data.Perk
import tech.thatgravyboat.skyblockapi.utils.extentions.parseFormattedDouble
import tech.thatgravyboat.skyblockapi.utils.extentions.parseFormattedInt
import tech.thatgravyboat.skyblockapi.utils.extentions.toIntValue
import tech.thatgravyboat.skyblockapi.utils.regex.RegexGroup
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findGroup
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.match
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.Dungeon
import xyz.meowing.krypt.api.dungeons.utils.DungeonFloor
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.ScoreboardEvent
import xyz.meowing.krypt.events.core.TablistEvent
import xyz.meowing.krypt.utils.StringUtils.removeFormatting
import kotlin.math.ceil
import kotlin.math.floor

@Module
object DungeonScore {
    private val PUZZLE_STATES = mapOf("✦" to 0, "✔" to 1, "✖" to 2)
    private val MILESTONES = listOf("⓿", "❶", "❷", "❸", "❹", "❺", "❻", "❼", "❽", "❾")
    private val FLOOR_SECRETS = mapOf("F1" to 0.3, "F2" to 0.4, "F3" to 0.5, "F4" to 0.6, "F5" to 0.7, "F6" to 0.85)
    private val FLOOR_TIMES = mapOf("F3" to 120, "F4" to 240, "F5" to 120, "F6" to 240, "F7" to 360, "M6" to 120, "M7" to 360)

    private val regexGroup = RegexGroup("dungeons.score")
    private val SECRETS_FOUND_PATTERN = regexGroup.create("secrets.found", """^Secrets Found: (?<count>[\d,.]+)$""")
    private val SECRETS_PERCENT_PATTERN = regexGroup.create("secrets.percent", """^Secrets Found: (?<percent>[\d,.]+)%$""")
    private val MILESTONES_PATTERN = regexGroup.create("milestone", """^Your Milestone: .(?<milestone>.)$""")
    private val COMPLETED_ROOMS_PATTERN = regexGroup.create("rooms.completed", """^Completed Rooms: (?<count>\d+)$""")
    private val TEAM_DEATHS_PATTERN = regexGroup.create("deaths", """^Team Deaths: (?<count>\d+)$""")
    private val PUZZLE_COUNT_PATTERN = regexGroup.create("puzzle.count", """^Puzzles: \((?<count>\d+)\)$""")
    private val CRYPTS_PATTERN = regexGroup.create("crypts", """^Crypts: (?<count>\d+)$""")
    private val PUZZLE_STATE_PATTERN = regexGroup.create("puzzle.state", """^(?<name>[\w ]+): \[(?<state>[✦✔✖])]\s?\(?(?<player>\w{1,16})?\)?$""")
    private val OPENED_ROOMS_PATTERN = regexGroup.create("rooms.opened", """^Opened Rooms: (?<count>\d+)$""")
    private val CLEARED_ROOMS_PATTERN = regexGroup.create("rooms.cleared", """^Completed Rooms: (?<count>\d+)$""")
    private val CLEAR_PERCENT_PATTERN = regexGroup.create("clear.percent", """^Cleared: (?<percent>\d+)% \(\d+\)$""")
    private val DUNGEON_TIME_PATTERN = regexGroup.create("time", """^Time: (?:(?<hours>\d+)h)?\s?(?:(?<minutes>\d+)m)?\s?(?:(?<seconds>\d+)s)?$""")

    val hasPaul = Perk.EZPZ.active
    var data = ScoreData()
        private set
    val score get() = data.score

    fun reset() {
        data = ScoreData()
        MimicTrigger.reset()
    }

    init {
        EventBus.registerIn<TablistEvent.Change>(SkyBlockIsland.THE_CATACOMBS) { event ->
            event.new.forEach { column ->
                column.forEach { line ->
                    parseTablist(line.stripped.trim())
                }
            }
        }

        EventBus.registerIn<ScoreboardEvent.Update>(SkyBlockIsland.THE_CATACOMBS) { event ->
            event.new.forEach { line ->
                parseSidebar(line.removeFormatting().trim())
            }
        }
    }

    private fun parseTablist(msg: String) = with(data) {
        DUNGEON_TIME_PATTERN.match(msg) { (h, m, s) ->
            dungeonSeconds = h.toIntValue() * 3600 + m.toIntValue() * 60 + s.toIntValue()
        }

        secretsFound = SECRETS_FOUND_PATTERN.findGroup(msg, "count").parseFormattedInt(secretsFound)
        secretsFoundPercent = SECRETS_PERCENT_PATTERN.findGroup(msg, "percent").parseFormattedDouble(secretsFoundPercent)
        crypts = CRYPTS_PATTERN.findGroup(msg, "count").parseFormattedInt(crypts)
        milestone = MILESTONES_PATTERN.findGroup(msg, "milestone") ?: milestone
        completedRooms = COMPLETED_ROOMS_PATTERN.findGroup(msg, "count").toIntValue()
        puzzleCount = PUZZLE_COUNT_PATTERN.findGroup(msg, "count").toIntValue()
        teamDeaths = TEAM_DEATHS_PATTERN.findGroup(msg, "count").toIntValue()
        openedRooms = OPENED_ROOMS_PATTERN.findGroup(msg, "count").toIntValue()
        clearedRooms = CLEARED_ROOMS_PATTERN.findGroup(msg, "count").toIntValue()

        PUZZLE_STATE_PATTERN.match(msg) { (_, state) ->
            if (PUZZLE_STATES[state] == 1) puzzlesDone++
        }

        calculateScore()
    }

    private fun parseSidebar(msg: String) = with(data) {
        CLEAR_PERCENT_PATTERN.match(msg) { (percent) ->
            clearedPercent = percent.toIntValue()
        }
        secretsPercentNeeded = FLOOR_SECRETS[Dungeon.floor?.name] ?: 1.0
    }

    private fun calculateScore() = with(data) {
        val floor = Dungeon.floor ?: return
        val missingPuzzles = puzzleCount - puzzlesDone

        totalSecrets = ((100.0 / secretsFoundPercent) * secretsFound + 0.5).toInt()
        secretsRemaining = totalSecrets - secretsFound

        val estimatedRooms = (100.0 / clearedPercent) * completedRooms + 0.4
        totalRooms = estimatedRooms.toInt().takeIf { it > 0 } ?: 36

        adjustedRooms = completedRooms + if (!Dungeon.bloodSpawnedAll || !Dungeon.inBoss) 1 else 0
        if (completedRooms <= totalRooms - 1 && !Dungeon.bloodSpawnedAll) adjustedRooms++

        deathPenalty = (teamDeaths * -2) + if (hasSpiritPet && teamDeaths > 0) 1 else 0
        completionRatio = adjustedRooms.toDouble() / totalRooms
        roomsScore = (80 * completionRatio).coerceIn(0.0, 80.0)
        skillScore = (20 + roomsScore - 10 * missingPuzzles + deathPenalty).coerceIn(20.0, 100.0)

        secretsScore = (40 * ((secretsFoundPercent / 100.0) / secretsPercentNeeded)).coerceIn(0.0, 40.0)
        exploreScore = if (clearedPercent == 0) 0.0 else (60 * completionRatio + secretsScore).coerceIn(0.0, 100.0)

        bonusScore = crypts.coerceAtMost(5) + if (MimicTrigger.mimicDead) 2 else 0 + if (hasPaul) 10 else 0

        val timeOffset = dungeonSeconds - (FLOOR_TIMES[floor.name] ?: 0)
        val speedScore = calculateSpeedScore(timeOffset, if (floor == DungeonFloor.E) 0.7 else 1.0)

        score = (skillScore + exploreScore + speedScore + bonusScore).toInt()
        maxSecrets = ceil(totalSecrets * secretsPercentNeeded).toInt()
        minSecrets = floor(maxSecrets * ((40.0 - bonusScore + deathPenalty) / 40.0)).toInt()
    }

    private fun calculateSpeedScore(time: Int, scale: Double): Int = when {
        time < 492 -> 100.0 * scale
        time < 600 -> (140 - time / 12.0) * scale
        time < 840 -> (115 - time / 24.0) * scale
        time < 1140 -> (108 - time / 30.0) * scale
        time < 3570 -> (98.5 - time / 40.0) * scale
        else -> 0.0
    }.toInt()

    fun getMilestone(asIndex: Boolean = false): Any = if (asIndex) MILESTONES.indexOf(data.milestone) else data.milestone
}