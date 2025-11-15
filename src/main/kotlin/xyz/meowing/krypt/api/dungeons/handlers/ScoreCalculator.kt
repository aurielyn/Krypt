package xyz.meowing.krypt.api.dungeons.handlers

import tech.thatgravyboat.skyblockapi.utils.extentions.stripColor
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findGroup
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findGroups
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.DungeonAPI.inBoss
import xyz.meowing.krypt.api.dungeons.DungeonAPI.isPaul
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.api.dungeons.enums.map.Checkmark
import xyz.meowing.krypt.api.dungeons.enums.map.RoomType
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.ScoreboardEvent
import xyz.meowing.krypt.events.core.TablistEvent
import kotlin.math.floor

/**
 * Modified from Noamm's ScoreCalculator
 * Original File: [GitHub](https://github.com/Noamm9/NoammAddons/blob/master/src/main/kotlin/noammaddons/features/impl/dungeons/dmap/handlers/ScoreCalculation.kt)
 */
object ScoreCalculator {
    private val secretsFoundPattern = Regex("Secrets Found: (?<secrets>\\d+)")
    private val secretsFoundPercentagePattern = Regex("Secrets Found: (?<percentage>[\\d.]+)%")
    private val cryptsPattern = Regex("Crypts: (?<crypts>\\d+)")
    private val completedRoomsRegex = Regex("Completed Rooms: (?<count>\\d+)")
    private val dungeonClearedPattern = Regex("Cleared: (?<percentage>\\d+)% \\(\\d+\\)")
    private val timeElapsedPattern = Regex(" Elapsed: (?:(?<hrs>\\d+)h )?(?:(?<min>\\d+)m )?(?:(?<sec>\\d+)s)?")
    private var bloodDone = false

    var deathCount: Int = 0
    var totalSecrets: Int = 0
    var foundSecrets: Int = 0
    var cryptsCount: Int = 0

    var secretPercentage: Double = 0.0
    var clearedPercentage: Int = 0
    var completedRooms: Int = 0
    var secondsElapsed: Int = 0

    var mimicKilled: Boolean = false
    var princeKilled: Boolean = false

    private val dungeonFloor: DungeonFloor?
        get() = DungeonAPI.floor

    private val dungeonFloorNumber: Int?
        get() = DungeonAPI.floor?.floorNumber

    private val totalRooms
        get() = if (completedRooms > 0 && clearedPercentage > 0)
            floor((completedRooms / (clearedPercentage / 100.0)) + 0.4).toInt()
        else 36

    // TODO: not calculate on every call
    val score: Int
        get() {
            val currentFloor = DungeonAPI.floor ?: return 0
            val effectiveCompletedRooms = completedRooms + (if (! bloodDone) 1 else 0) + (if (!inBoss) 1 else 0)
            val puzzles = DungeonAPI.rooms.filter { it?.type == RoomType.PUZZLE }
            val secretsScore = floor((secretPercentage / (requiredSecrets[currentFloor]!!)) / 100.0 * 40.0).coerceIn(.0, 40.0).toInt()
            val completedRoomScore = (effectiveCompletedRooms.toDouble() / totalRooms.toDouble() * 60.0).coerceIn(.0, 60.0).toInt()

            val skillRooms = floor(effectiveCompletedRooms.toDouble() / totalRooms.toDouble() * 80f).coerceIn(.0, 80.0).toInt()
            val puzzlePenalty = (puzzles.size - puzzles.count { it?.checkmark == Checkmark.GREEN }) * 10
            val deathPenalty = (deathCount * 2 - 1).coerceAtLeast(0)

            val score = secretsScore + completedRoomScore + (20 + skillRooms - puzzlePenalty - deathPenalty).coerceIn(20, 100) + bonusScore + speedScore

            return score
        }

    val bonusScore: Int
        get() {
            var score = cryptsCount.coerceAtMost(5)
            if (mimicKilled && (dungeonFloorNumber ?: 0) > 5) score += 2
            if (princeKilled) score += 1
            if (isPaul) score += 10
            return score
        }

    val speedScore: Int
        get() {
            val limit = timeLimits[dungeonFloor] ?: return 100
            if (secondsElapsed <= limit) return 100
            val percentageOver = (secondsElapsed - limit) * 100f / limit
            return (100 - getSpeedDeduction(percentageOver)).toInt().coerceAtLeast(0)
        }


    init {
        EventBus.registerIn<TablistEvent.Change> (SkyBlockIsland.THE_CATACOMBS) { event ->
            event.new.flatten().forEach { parseTablist(it.stripped.trim()) }
        }

        EventBus.registerIn<ScoreboardEvent.Update>(SkyBlockIsland.THE_CATACOMBS) { event ->
            event.new.forEach { parseSidebar(it.stripColor().trim()) }
        }
    }

    fun reset() {
        deathCount = 0
        foundSecrets = 0
        cryptsCount = 0
        secretPercentage = 0.0
        clearedPercentage = 0
        completedRooms = 0
        secondsElapsed = 0
        mimicKilled = false
        princeKilled = false
        bloodDone = false
    }

    private fun parseTablist(line: String) {
        when {
            line.contains("Crypts:") -> {
                cryptsPattern
                    .findGroup(line, "crypts")
                    ?.toIntOrNull()
                    ?.let { cryptsCount = it }
            }
            line.contains("Completed Rooms:") -> {
                completedRoomsRegex
                    .findGroup(line, "count")
                    ?.toIntOrNull()
                    ?.let { completedRooms = it }
            }
            line.contains("Secrets Found:") -> {
                if (line.contains('%')) {
                    secretsFoundPercentagePattern
                        .findGroup(line, "percentage")
                        ?.toDoubleOrNull()
                        ?.let { secretPercentage = it }
                } else {
                    secretsFoundPattern
                        .findGroup(line, "secrets")
                        ?.toIntOrNull()
                        ?.let { foundSecrets = it }
                }
            }
        }
    }

    private fun parseSidebar(line: String) {
        when {
            line.startsWith("Cleared:") -> {
                dungeonClearedPattern
                    .findGroup(line, "percentage")
                    ?.toIntOrNull()
                    ?.let { newCompletedRooms ->
                        if (newCompletedRooms != clearedPercentage && DungeonAPI.bloodKilledAll) bloodDone = true
                        clearedPercentage = newCompletedRooms
                    }
            }

            line.startsWith("Time Elapsed:") -> {
                timeElapsedPattern.findGroups(line, "hrs", "min", "sec")?.let { (hrs, min, sec) ->
                    val hours = hrs.toIntOrNull() ?: 0
                    val minutes = min.toIntOrNull() ?: 0
                    val seconds = sec.toIntOrNull() ?: 0
                    secondsElapsed = (hours * 3600 + minutes * 60 + seconds)
                }
            }
        }
    }

    private fun getSpeedDeduction(percentage: Float): Float {
        var remaining = percentage
        var deduction = 0f

        deduction += (remaining.coerceAtMost(20f) / 2f)
        remaining -= 20f
        if (remaining <= 0) return deduction

        deduction += (remaining.coerceAtMost(20f) / 3.5f)
        remaining -= 20f
        if (remaining <= 0) return deduction

        deduction += (remaining.coerceAtMost(10f) / 4f)
        remaining -= 10f
        if (remaining <= 0) return deduction

        deduction += (remaining.coerceAtMost(10f) / 5f)
        remaining -= 10f
        if (remaining <= 0) return deduction

        deduction += (remaining / 6f)
        return deduction
    }

    private val requiredSecrets = mapOf(
        DungeonFloor.E to 0.3,
        DungeonFloor.F1 to 0.3,
        DungeonFloor.F2 to 0.4,
        DungeonFloor.F3 to 0.5,
        DungeonFloor.F4 to 0.6,
        DungeonFloor.F5 to 0.7,
        DungeonFloor.F6 to 0.85,
        DungeonFloor.F7 to 1.0,
        DungeonFloor.M1 to 1.0,
        DungeonFloor.M2 to 1.0,
        DungeonFloor.M3 to 1.0,
        DungeonFloor.M4 to 1.0,
        DungeonFloor.M5 to 1.0,
        DungeonFloor.M6 to 1.0,
        DungeonFloor.M7 to 1.0
    )

    private val timeLimits = mapOf(
        DungeonFloor.E to 600,
        DungeonFloor.F1 to 600,
        DungeonFloor.F2 to 600,
        DungeonFloor.F3 to 600,
        DungeonFloor.F4 to 720,
        DungeonFloor.F5 to 600,
        DungeonFloor.F6 to 720,
        DungeonFloor.F7 to 840,
        DungeonFloor.M1 to 480,
        DungeonFloor.M2 to 480,
        DungeonFloor.M3 to 480,
        DungeonFloor.M4 to 480,
        DungeonFloor.M5 to 480,
        DungeonFloor.M6 to 600,
        DungeonFloor.M7 to 840
    )
}