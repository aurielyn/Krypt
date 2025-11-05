package xyz.meowing.krypt.api.dungeons.players

import net.minecraft.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.krypt.Krypt
import xyz.meowing.krypt.api.dungeons.utils.DungeonClass
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.TablistEvent

object DungeonPlayerManager {
    /**
     * Match a player entry.
     * Group 1: name
     * Group 2: class (or literal "EMPTY" pre map start)
     * Group 3: level (or nothing, if pre map start)
     * This regex filters out the ironman icon as well as rank prefixes and emblems
     * \[\d+\] (?:\[[A-Za-z]+\] )?(?&lt;name&gt;[A-Za-z0-9_]+) (?:.+ )?\((?&lt;class&gt;\S+) ?(?&lt;level&gt;[LXVI0]+)?\)
     *
     * Taken from Skyblocker
     */
    val playerTabPattern = Regex("\\[\\d+] (?:\\[[A-Za-z]+] )?(?<name>[A-Za-z0-9_]+) (?:.+ )?\\((?<class>\\S+) ?(?<level>[LXVI0]+)?\\)")
    val playerGhostPattern = Regex(" ☠ (?<name>[A-Za-z0-9_]+) .+ became a ghost\\.")

    val players = Array<DungeonPlayer?>(5) { null }

    fun init() {
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
                    players[i]!!.dclass = clazz
                } else {
                    players[i] = DungeonPlayer(name).apply { dclass = clazz }
                }
            }
        }

        EventBus.registerIn<ChatEvent.Receive>(SkyBlockIsland.THE_CATACOMBS) { onDeath(it.message) }
    }

    private fun onDeath(text: Text) {
        val match = playerGhostPattern.find(text.string) ?: return

        var name = match.groups["name"]?.value ?: return
        if (name == "You") KnitPlayer.player?.let { name = it.name.string }

        val player = getPlayer(name)
        if (player != null) {
            player.dclass = DungeonClass.DEAD
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