package xyz.meowing.krypt.api.dungeons.enums

/**
 * @author SkyblockAPI
 */
enum class DungeonFloor(
    val bossName: String,
    val chatBossName: String = bossName,
    val floorNumber: Int,
    val isMasterMode: Boolean
) {
    E("The Watcher", 0, false),

    F1("Bonzo", 1, false),
    F2("Scarf", 2, false),
    F3("The Professor", 3, false),
    F4("Thorn", 4, false),
    F5("Livid", 5, false),
    F6("Sadan", 6, false),
    F7("Necron", "Maxor", 7, false),

    M1("Bonzo", 1, true),
    M2("Scarf", 2, true),
    M3("The Professor", 3, true),
    M4("Thorn", 4, true),
    M5("Livid", 5, true),
    M6("Sadan", 6, true),
    M7("Necron", "Maxor", 7, true),
    ;

    constructor(
        bossName: String,
        floorNumber: Int,
        isMasterMode: Boolean
    ) : this(bossName, bossName, floorNumber, isMasterMode)

    companion object {
        fun getByName(name: String) = runCatching { DungeonFloor.valueOf(name) }.getOrNull()
    }
}