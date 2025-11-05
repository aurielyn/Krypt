package xyz.meowing.krypt.api.dungeons.players

import tech.thatgravyboat.skyblockapi.utils.extentions.parseRomanOrArabic
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.find
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findThenNull
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.utils.DungeonClass
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.TablistEvent

@Module
object DungeonPlayerManager {
    private val playerTabRegex = Regex("(?:\\[.+] ?)*(?<name>\\S+) .*\\((?<class>\\S+) (?<level>.+)\\)")
    private val deadTeammateRegex = Regex("\\[.+] (?<name>\\S+) .*\\(DEAD\\)")

    var players: List<DungeonPlayer> = emptyList()
        private set

    init {
        EventBus.register<LocationEvent.IslandChange> { reset() }

        EventBus.registerIn<TablistEvent.Change>(SkyBlockIsland.THE_CATACOMBS) { event ->
            val firstColumn = event.new.firstOrNull() ?: return@registerIn

            val ownName = KnitPlayer.name

            for (line in firstColumn) {
                val stripped = line.stripped

                playerTabRegex.findThenNull(stripped, "name", "class", "level") { (name, dungeonClass, level) ->
                    val dungeonPlayer = players.find { it.name == name }

                    if (dungeonPlayer != null) {
                        if (name != ownName) dungeonPlayer.dead = false

                        if (dungeonPlayer.missingData()) {
                            dungeonPlayer.dungeonClass = DungeonClass.from(dungeonClass)
                            dungeonPlayer.classLevel = level.parseRomanOrArabic()
                        }

                        return@findThenNull
                    }

                    val playerClass = DungeonClass.from(dungeonClass)
                    val player = DungeonPlayer(name, playerClass, level.parseRomanOrArabic())

                    players += player
                } ?: continue

                deadTeammateRegex.find(stripped, "name") { (name) ->
                    var dungeonPlayer = players.find { it.name == name }

                    if (dungeonPlayer == null) {
                        dungeonPlayer = DungeonPlayer(name, null, null)
                        players += dungeonPlayer
                    }

                    dungeonPlayer.dead = true
                    dungeonPlayer.deaths++
                }
            }
        }
    }

    fun getPlayer(name: String): DungeonPlayer? {
        return players.firstOrNull { it.name == name }
    }

    fun updateAllSecrets() {
        players.forEach { it.updateSecrets() }
    }

    fun reset() {
        players = emptyList()
    }
}