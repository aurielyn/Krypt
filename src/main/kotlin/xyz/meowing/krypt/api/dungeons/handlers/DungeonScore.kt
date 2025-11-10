package xyz.meowing.krypt.api.dungeons.handlers

import tech.thatgravyboat.skyblockapi.api.data.Perk
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.api.dungeons.enums.ScoreData
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.ScoreboardEvent
import xyz.meowing.krypt.events.core.TablistEvent
import xyz.meowing.krypt.utils.StringUtils.removeFormatting
import kotlin.math.ceil
import kotlin.math.floor

@Module
object DungeonScore {
    private val SECRETS_FOUND_PATTERN = Regex("""Secrets Found: ([\d,.]+)$""")
    private val SECRETS_PERCENT_PATTERN = Regex("""Secrets Found: ([\d,.]+)%$""")
    private val MILESTONE_PATTERN = Regex("""Your Milestone: .(.)$""")
    private val COMPLETED_PATTERN = Regex("""Completed Rooms: (\d+)$""")
    private val DEATHS_PATTERN = Regex("""Team Deaths: (\d+)$""")
    private val PUZZLE_COUNT_PATTERN = Regex("""Puzzles: \((\d+)\)$""")
    private val CRYPTS_PATTERN = Regex("""Crypts: (\d+)$""")
    private val PUZZLE_STATE_PATTERN = Regex("""[\w ]+: \[([✦✔✖])]""")
    private val CLEAR_PERCENT_PATTERN = Regex("""Cleared: (\d+)%""")
    private val TIME_PATTERN = Regex("""Time: (?:(\d+)h)?\s?(?:(\d+)m)?\s?(?:(\d+)s)?$""")

    private val requiredSecrets = mapOf(
        DungeonFloor.E to 0.3, DungeonFloor.F1 to 0.3, DungeonFloor.F2 to 0.4,
        DungeonFloor.F3 to 0.5, DungeonFloor.F4 to 0.6, DungeonFloor.F5 to 0.7,
        DungeonFloor.F6 to 0.85, DungeonFloor.F7 to 1.0, DungeonFloor.M1 to 1.0,
        DungeonFloor.M2 to 1.0, DungeonFloor.M3 to 1.0, DungeonFloor.M4 to 1.0,
        DungeonFloor.M5 to 1.0, DungeonFloor.M6 to 1.0, DungeonFloor.M7 to 1.0
    )

    private val timeLimits = mapOf(
        DungeonFloor.E to 600, DungeonFloor.F1 to 600, DungeonFloor.F2 to 600,
        DungeonFloor.F3 to 600, DungeonFloor.F4 to 720, DungeonFloor.F5 to 600,
        DungeonFloor.F6 to 720, DungeonFloor.F7 to 840, DungeonFloor.M1 to 480,
        DungeonFloor.M2 to 480, DungeonFloor.M3 to 480, DungeonFloor.M4 to 480,
        DungeonFloor.M5 to 480, DungeonFloor.M6 to 600, DungeonFloor.M7 to 840
    )

    val hasPaul get() = Perk.EZPZ.active
    var data = ScoreData()
    val score get() = data.score

    private var bloodDone = false

    fun reset() {
        data = ScoreData()
        bloodDone = false
        MimicTrigger.reset()
    }

    init {
        EventBus.registerIn<TablistEvent.Change>(SkyBlockIsland.THE_CATACOMBS) { event ->
            event.new.flatten().forEach { parseTablist(it.stripped.trim()) }
        }

        EventBus.registerIn<ScoreboardEvent.Update>(SkyBlockIsland.THE_CATACOMBS) { event ->
            event.new.forEach { parseSidebar(it.removeFormatting().trim()) }
        }
    }

    private fun parseTablist(msg: String) = with(data) {
        TIME_PATTERN.find(msg)?.let {
            val (h, m, s) = it.destructured
            dungeonSeconds = (h.toIntOrNull() ?: 0) * 3600 + (m.toIntOrNull() ?: 0) * 60 + (s.toIntOrNull() ?: 0)
        }

        secretsFound = msg.extract(SECRETS_FOUND_PATTERN, secretsFound)
        secretsFoundPercent = msg.extractDouble(SECRETS_PERCENT_PATTERN, secretsFoundPercent)
        crypts = msg.extract(CRYPTS_PATTERN, crypts)
        milestone = msg.extractString(MILESTONE_PATTERN, milestone)
        completedRooms = msg.extract(COMPLETED_PATTERN, completedRooms)
        puzzleCount = msg.extract(PUZZLE_COUNT_PATTERN, puzzleCount)
        teamDeaths = msg.extract(DEATHS_PATTERN, teamDeaths)

        PUZZLE_STATE_PATTERN.find(msg)?.groupValues?.get(1)?.let { if (it == "✔") puzzlesDone++ }

        calculateScore()
    }

    private fun parseSidebar(msg: String) = with(data) {
        CLEAR_PERCENT_PATTERN.find(msg)?.groupValues?.get(1)?.toIntOrNull()?.let { newPercent ->
            if (newPercent != clearedPercent && DungeonAPI.bloodKilledAll) bloodDone = true
            clearedPercent = newPercent
        }
    }

    private fun calculateScore() = with(data) {
        val floor = DungeonAPI.floor ?: return
        val adjustedRooms = completedRooms + (if (!bloodDone) 1 else 0) + (if (!DungeonAPI.inBoss) 1 else 0)
        val totalRooms = if (completedRooms > 0 && clearedPercent > 0) {
            floor((completedRooms / (clearedPercent / 100.0)) + 0.4).toInt()
        } else 36

        val reqSecretPct = requiredSecrets[floor] ?: 1.0
        secretsScore = floor((secretsFoundPercent / 100.0 / reqSecretPct) * 40.0).coerceIn(0.0, 40.0)
        roomsScore = (adjustedRooms.toDouble() / totalRooms * 60.0).coerceIn(0.0, 60.0)

        val skillRooms = floor(adjustedRooms.toDouble() / totalRooms * 80.0).coerceIn(0.0, 80.0).toInt()
        val puzzlePenalty = (puzzleCount - puzzlesDone) * 10
        val deathPenalty = (teamDeaths * 2 - 1).coerceAtLeast(0)
        skillScore = (20.0 + skillRooms - puzzlePenalty - deathPenalty).coerceIn(20.0, 100.0)

        bonusScore = crypts.coerceAtMost(5) + (if (MimicTrigger.mimicDead && floor.floorNumber > 5) 2 else 0) + (if (hasPaul) 10 else 0)

        val speedScore = timeLimits[floor]?.let { limit ->
            if (dungeonSeconds <= limit) 100
            else (100 - getSpeedDeduction((dungeonSeconds - limit) * 100f / limit)).toInt().coerceAtLeast(0)
        } ?: 100

        score = (secretsScore + roomsScore + skillScore + bonusScore + speedScore).toInt()

        totalSecrets = if (secretsFoundPercent > 0) ((100.0 / secretsFoundPercent) * secretsFound + 0.5).toInt() else 0
        secretsRemaining = totalSecrets - secretsFound
        this.totalRooms = totalRooms
        this.adjustedRooms = adjustedRooms
        this.deathPenalty = deathPenalty
        maxSecrets = ceil(totalSecrets * reqSecretPct).toInt()
        minSecrets = floor(maxSecrets * ((40.0 - bonusScore + deathPenalty) / 40.0)).toInt()
    }

    private fun getSpeedDeduction(pct: Float): Float {
        var remaining = pct
        var deduction = (remaining.coerceAtMost(20f) / 2f).also { remaining -= 20f }
        if (remaining <= 0) return deduction
        deduction += (remaining.coerceAtMost(20f) / 3.5f).also { remaining -= 20f }
        if (remaining <= 0) return deduction
        deduction += (remaining.coerceAtMost(10f) / 4f).also { remaining -= 10f }
        if (remaining <= 0) return deduction
        deduction += (remaining.coerceAtMost(10f) / 5f).also { remaining -= 10f }
        return if (remaining <= 0) deduction else deduction + remaining / 6f
    }

    private fun String.extract(regex: Regex, fallback: Int) =
        regex.find(this)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: fallback
    private fun String.extractDouble(regex: Regex, fallback: Double) =
        regex.find(this)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: fallback
    private fun String.extractString(regex: Regex, fallback: String) =
        regex.find(this)?.groupValues?.get(1) ?: fallback
}