package xyz.meowing.krypt.api.dungeons.core.enums

import xyz.meowing.krypt.api.dungeons.DungeonAPI

enum class DungeonKey(private val getter: () -> Int) {
    WITHER(DungeonAPI::witherKeys),
    BLOOD(DungeonAPI::bloodKeys),
    ;

    val current: Int get() = getter()

    companion object {
        fun getById(id: String) = entries.firstOrNull { it.name.equals(id, ignoreCase = true) }
    }
}