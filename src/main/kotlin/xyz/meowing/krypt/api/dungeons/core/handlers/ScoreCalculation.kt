package xyz.meowing.krypt.api.dungeons.core.handlers

import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.DungeonAPI.cryptCount
import xyz.meowing.krypt.api.dungeons.DungeonAPI.dungeonFloor
import xyz.meowing.krypt.api.dungeons.DungeonAPI.inBoss
import xyz.meowing.krypt.api.dungeons.DungeonAPI.puzzles
import xyz.meowing.krypt.api.dungeons.DungeonAPI.watcherClearTime
import xyz.meowing.krypt.api.dungeons.core.enums.DungeonFloor
import xyz.meowing.krypt.api.dungeons.core.map.RoomState
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.ScoreboardEvent
import xyz.meowing.krypt.events.core.TablistEvent
import kotlin.math.floor

@Module
object ScoreCalculation {
    private val secretsFoundPattern = Regex("§r Secrets Found: §r§b(?<secrets>\\d+)§r")
    private val secretsFoundPercentagePattern = Regex("§r Secrets Found: §r§[ae](?<percentage>[\\d.]+)%§r")
    private val completedRoomsRegex = Regex("§r Completed Rooms: §r§d(?<count>\\d+)§r")
    private val dungeonClearedPattern = Regex("Cleared: (?<percentage>\\d+)% \\(\\d+\\)")
    private val timeElapsedPattern = Regex(" Elapsed: (?:(?<hrs>\\d+)h )?(?:(?<min>\\d+)m )?(?:(?<sec>\\d+)s)?")
    private var bloodDone = false

    var alerted300 = false
    var alerted270 = false

    var deathCount = 0
    var foundSecrets = 0

    var secretPercentage = 0.0
    var clearedPercentage = 0
    var completedRooms = 0
    var secondsElapsed = 0
    var mimicKilled = false
    var princeKilled = false

    private val totalRooms
        get() = if (completedRooms > 0 && clearedPercentage > 0) floor((completedRooms / (clearedPercentage / 100.0)) + 0.4).toInt() else 36

    val score: Int
        get() {
            val currentFloor = dungeonFloor ?: return 0
            val effectiveCompletedRooms = completedRooms + (if (! bloodDone) 1 else 0) + (if (!inBoss) 1 else 0)

            val secretsScore = floor((secretPercentage / (requiredSecretPercentage[currentFloor]!!)) / 100.0 * 40.0).coerceIn(.0, 40.0).toInt()
            val completedRoomScore = (effectiveCompletedRooms.toDouble() / totalRooms.toDouble() * 60.0).coerceIn(.0, 60.0).toInt()

            val skillRooms = floor(effectiveCompletedRooms.toDouble() / totalRooms.toDouble() * 80f).coerceIn(.0, 80.0).toInt()
            val puzzlePenalty = (puzzles.size - puzzles.count { it.state == RoomState.GREEN }) * 10
            val deathPenalty = (deathCount * 2 - 1).coerceAtLeast(0)

            val score = secretsScore + completedRoomScore + (20 + skillRooms - puzzlePenalty - deathPenalty).coerceIn(20, 100) + bonusScore + speedScore

            //if (score >= 270 && ! alerted270) ScoreCalculator.on270Score()
            //if (score >= 300 && ! alerted300) ScoreCalculator.on300Score()
            return score
        }

    val bonusScore: Int
        get() {
            var score = cryptCount.coerceAtMost(5)
            //if (MimicDetector.mimicKilled.get() && (dungeonFloor?.floorNumber ?: 0) > 5) score += 2
            //if (MimicDetector.princeKilled.get()) score += 1
            if (DungeonAPI.isPaul) score += 10
            return score
        }

    val speedScore: Int
        get() {
            val limit = timeLimit[dungeonFloor] ?: return 100
            if (secondsElapsed <= limit) return 100
            val percentageOver = (secondsElapsed - limit) * 100f / limit
            return (100 - getSpeedDeduction(percentageOver)).toInt().coerceAtLeast(0)
        }

    init {
        EventBus.registerIn<TablistEvent.Change> (SkyBlockIsland.THE_CATACOMBS) { event ->
            event.new.forEach { column ->
                column.forEach { line ->
                    val line = line.stripped
                    when {
                        line.contains("Completed Rooms:") -> {
                            completedRoomsRegex.find(line)?.let {
                                completedRooms = it.groups["count"]?.value?.toIntOrNull() ?: completedRooms
                            }
                        }

                        line.contains("Secrets Found:") -> {
                            if (line.contains('%')) secretsFoundPercentagePattern.find(line)?.let {
                                secretPercentage = it.groups["percentage"]?.value?.toDoubleOrNull() ?: secretPercentage
                            }
                        }

                        else -> {
                            secretsFoundPattern.find(line)?.let { foundSecrets = it.groups["secrets"]?.value?.toIntOrNull() ?: foundSecrets }
                        }
                    }
                }
            }
        }

        EventBus.registerIn<ScoreboardEvent.Update> (SkyBlockIsland.THE_CATACOMBS) { event ->
            event.new.forEach { text ->
                when {
                    text.startsWith("Cleared:") -> dungeonClearedPattern.find(text)?.let {
                        val newCompletedRooms = it.groups["percentage"]?.value?.toIntOrNull()
                        if (newCompletedRooms != clearedPercentage && watcherClearTime != null) bloodDone = true
                        clearedPercentage = newCompletedRooms ?: clearedPercentage
                    }

                    text.startsWith("Time Elapsed:") -> timeElapsedPattern.find(text)?.let { matcher ->
                        val hours = matcher.groups["hrs"]?.value?.toIntOrNull() ?: 0
                        val minutes = matcher.groups["min"]?.value?.toIntOrNull() ?: 0
                        val seconds = matcher.groups["sec"]?.value?.toIntOrNull() ?: 0
                        secondsElapsed = (hours * 3600 + minutes * 60 + seconds)
                    }
                }
            }
        }

        EventBus.register<LocationEvent.WorldChange> {
            reset()
        }
    }

    fun reset() {
        deathCount = 0
        foundSecrets = 0
        secretPercentage = 0.0
        clearedPercentage = 0
        completedRooms = 0
        secondsElapsed = 0
        mimicKilled = false
        princeKilled = false
        alerted300 = false
        alerted270 = false
        bloodDone = false
    }

    private fun getSpeedDeduction(percentage: Float): Float {
        var percentageOver = percentage
        var deduction = 0f
        deduction += (percentageOver.coerceAtMost(20f) / 2f).also { percentageOver -= 20f }
        if (percentageOver <= 0) return deduction
        deduction += (percentageOver.coerceAtMost(20f) / 3.5f).also { percentageOver -= 20f }
        if (percentageOver <= 0) return deduction
        deduction += (percentageOver.coerceAtMost(10f) / 4f).also { percentageOver -= 10f }
        if (percentageOver <= 0) return deduction
        deduction += (percentageOver.coerceAtMost(10f) / 5f).also { percentageOver -= 10f }
        if (percentageOver <= 0) return deduction
        deduction += (percentageOver / 6f)
        return deduction
    }

    private val requiredSecretPercentage = mapOf(
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

    private val timeLimit = mapOf(
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