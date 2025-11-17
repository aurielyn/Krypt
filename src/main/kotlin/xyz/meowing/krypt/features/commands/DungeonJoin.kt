package xyz.meowing.krypt.features.commands

import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.command.Commodore
import xyz.meowing.knit.api.command.utils.GreedyString
import xyz.meowing.krypt.Krypt.prefix
import xyz.meowing.krypt.annotations.Command

@Command
object DungeonJoin : Commodore("jd") {
    private val regex = Regex("""[^0-9]""")
    private val floorNames = listOf("ENTRANCE", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN")

    init {
        runs { floor: GreedyString ->
            if (floor.string.length > 2) {
                KnitChat.fakeMessage("$prefix §fInvalid Floor.")
                return@runs
            }

            val masterMode = floor.string.contains("m", true)
            val floorString = floor.string
                .replace("e", "0", true)
                .replace(regex, "")

            val floorIndex = floorString.toIntOrNull() ?: -1

            if (floorIndex < 0 || floorIndex >= floorNames.size) {
                KnitChat.fakeMessage("$prefix §fInvalid Floor.")
                return@runs
            }

            val dungeonType = (if (masterMode) "MASTER_" else "") + "CATACOMBS"
            val dungeonString = dungeonType + (if (floorIndex != 0) "_FLOOR_" else "_") + floorNames[floorIndex]

            KnitChat.sendCommand("joindungeon $dungeonString")
        }
    }
}