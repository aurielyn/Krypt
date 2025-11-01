package xyz.meowing.krypt.api.dungeons

/**
 * @author SkyblockAPI
 */
enum class DungeonClass(val displayName: String) {
    ARCHER("Archer"),
    BERSERKER("Berserk"),
    HEALER("Healer"),
    MAGE("Mage"),
    TANK("Tank")
    ;

    companion object {
        fun getByName(name: String) = entries.find { it.displayName == name }
    }
}