package xyz.meowing.krypt.api.dungeons

import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.info.TabListChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.find
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.krypt.annotations.Module

@Module
object DungeonAPI {
    private val cryptsRegex = Regex("^ Crypts: (\\d+)$")

    var cryptCount: Int = 0
        private set

    init {
        SkyBlockAPI.eventBus.register(this)
    }

    @Subscription
    @OnlyIn(SkyBlockIsland.THE_CATACOMBS)
    fun updateTablist(event: TabListChangeEvent) {
        val firstColumn = event.new.firstOrNull() ?: return

        for (line in firstColumn) {
            cryptsRegex.find(line.stripped, "1") { (count) ->
                cryptCount = count.toIntOrNull() ?: cryptCount
            }
        }
    }

    @Subscription(IslandChangeEvent::class)
    fun onIslandChange() {
        cryptCount = 0
    }
}