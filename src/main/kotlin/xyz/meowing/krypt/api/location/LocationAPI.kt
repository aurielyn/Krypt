@file:OptIn(ExperimentalTime::class)
@file:Suppress("UNUSED")

package xyz.meowing.krypt.api.location

import net.hypixel.data.type.GameType
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyOnSkyBlock
import tech.thatgravyboat.skyblockapi.api.events.hypixel.ServerChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardTitleUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.info.TabListChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.location.ServerDisconnectEvent
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.anyMatch
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findGroup
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.LocationEvent
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Module
object LocationAPI {
    private val unknownAreas = mutableMapOf<String, SkyBlockIsland?>()

    private val locationRegex = Regex(" *[⏣ф] *(?<location>(?:\\s?[^ൠ\\s]+)*)(?: ൠ x\\d)?",)
    private val guestRegex = Regex("^ *\u270C *\\((?<guests>\\d+)/(?<max>\\d+)\\) *$",)
    private val playerCountRegex = Regex(" *(?:players|party) \\((?<count>\\d+)\\) *",)

    var forceOnSkyblock: Boolean = false

    var isOnSkyBlock: Boolean = false
        get() = field || forceOnSkyblock
        private set

    var island: SkyBlockIsland? = null
        private set

    var area: SkyBlockArea = SkyBlockAreas.NONE
        private set

    var serverId: String? = null
        private set

    var isGuest: Boolean = false
        private set

    var onHypixel: Boolean = false
        private set

    var onAlpha: Boolean = false
        private set

    var playerCount: Int = 0
        get() = field.coerceAtLeast(KnitClient.players.size)
        private set

    val maxPlayercount: Int?
        get() = when {
            serverId?.startsWith("mega") == true -> 60
            else -> when (island) {
                SkyBlockIsland.PRIVATE_ISLAND, SkyBlockIsland.GARDEN -> null
                SkyBlockIsland.KUUDRA -> 4
                SkyBlockIsland.MINESHAFT -> 4
                SkyBlockIsland.THE_CATACOMBS -> 5
                SkyBlockIsland.BACKWATER_BAYOU -> 16
                SkyBlockIsland.HUB -> 26
                SkyBlockIsland.JERRYS_WORKSHOP -> 27
                SkyBlockIsland.DARK_AUCTION -> 30
                else -> 24
            }
        }

    var lastServerChange: Instant = Instant.DISTANT_PAST
        private set

    init {
        SkyBlockAPI.eventBus.register(this)
    }

    @Subscription
    fun onServerChange(event: ServerChangeEvent) {
        lastServerChange = Clock.System.now()
        val wasOnSkyblock = isOnSkyBlock
        isOnSkyBlock = event.type == GameType.SKYBLOCK

        val newIsland = if (isOnSkyBlock && event.mode != null) SkyBlockIsland.getById(event.mode!!) else null
        val oldIsland = island

        if (!wasOnSkyblock && isOnSkyBlock) EventBus.post(LocationEvent.SkyblockJoin(newIsland))

        island = newIsland
        EventBus.post(LocationEvent.IslandChange(oldIsland, newIsland))

        serverId = event.name
    }

    @Subscription
    @OnlyOnSkyBlock
    fun onTabListUpdate(event: TabListChangeEvent) {
        val component = event.new.firstOrNull()?.firstOrNull() ?: return
        playerCount = playerCountRegex.findGroup(component.stripped.lowercase(), "count")?.toIntOrNull() ?: 0
    }

    @Subscription
    @OnlyOnSkyBlock
    fun onScoreboardTitleUpdate(event: ScoreboardTitleUpdateEvent) {
        isGuest = event.new.contains("guest", ignoreCase = true)
    }

    @Subscription
    @OnlyOnSkyBlock
    fun onScoreboardChange(event: ScoreboardUpdateEvent) {
        locationRegex.anyMatch(event.added, "location") { (location) ->
            val old = area
            area = SkyBlockArea(location)
            EventBus.post(LocationEvent.AreaChange(old, area))

            val knownArea = SkyBlockAreas.registeredAreas.entries.find { it.value.name == location } != null
            if (!knownArea) {
                unknownAreas.putIfAbsent(location, island)
            }
        }

        guestRegex.anyMatch(event.added, "guests") { (current) ->
            playerCount = current.toIntOrNull() ?: 0
        }
    }

    private fun reset() {
        isOnSkyBlock = false
        isGuest = false
        onHypixel = false
        onAlpha = false
        serverId = null
        area = SkyBlockAreas.NONE

        val old = island
        island = null

        if (old != null) {
            EventBus.post(LocationEvent.IslandChange(old, null))
        }
    }

    @Subscription(ServerDisconnectEvent::class)
    fun onServerDisconnect() = reset()
}