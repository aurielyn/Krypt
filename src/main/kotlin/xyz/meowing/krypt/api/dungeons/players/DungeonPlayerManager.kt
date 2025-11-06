package xyz.meowing.krypt.api.dungeons.players

import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.krypt.Krypt
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.utils.DungeonClass
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.TablistEvent

@Module
object DungeonPlayerManager {
    val playerTabPattern = Regex("\\[\\d+] (?:\\[[A-Za-z]+] )?(?<name>[A-Za-z0-9_]+) (?:.+ )?\\((?<class>\\S+) ?(?<level>[LXVI0]+)?\\)")
    val playerGhostPattern = Regex(" ☠ (?<name>[A-Za-z0-9_]+) .+ became a ghost\\.")

    val players = Array<DungeonPlayer?>(5) { null }

    init {
        EventBus.registerIn<TablistEvent.Change>(SkyBlockIsland.THE_CATACOMBS){ event ->
            val firstColumn = event.new.firstOrNull() ?: return@registerIn

            for (i in 0 until 5) {
                val index = 1 + i * 4
                if (index !in firstColumn.indices) continue
                val match = playerTabPattern.find(firstColumn[index].stripped)
                if (match == null) {
                    players[i] = null
                    continue
                }

                val name = match.groups["name"]?.value ?: continue
                val clazz = DungeonClass.from(match.groups["class"]?.value ?: "EMPTY")

                if (players[i] != null && players[i]!!.name == name) {
                    players[i]!!.dungeonClass = clazz
                } else {
                    players[i] = DungeonPlayer(name).apply { dungeonClass = clazz }
                }
            }
        }

        EventBus.registerIn<ChatEvent.Receive>(SkyBlockIsland.THE_CATACOMBS) { onDeath(it.message.string) }
    }

    private fun onDeath(text: String) {
        val match = playerGhostPattern.find(text) ?: return

        var name = match.groups["name"]?.value ?: return
        if (name == "You") KnitPlayer.player?.let { name = it.name.stripped }

        val player = getPlayer(name)
        if (player != null) {
            player.dead = true
            player.deaths ++
        } else {
            Krypt.LOGGER.error(
                "[Dungeon Player Manager] Received ghost message for player '{}' but player was not found in the player list: {}",
                match.groups["name"]?.value,
                players.contentToString()
            )
        }
    }

    fun getPlayer(name: String): DungeonPlayer? {
        return players
            .asSequence()
            .filterNotNull()
            .firstOrNull { it.name == name }
    }

    fun updateAllSecrets() {
        players.filterNotNull().forEach { it.updateSecrets() }
    }

    fun reset() {
        players.fill(null)
    }
}