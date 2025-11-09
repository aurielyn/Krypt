package xyz.meowing.krypt.api.dungeons.core.enums

import net.minecraft.util.math.BlockPos
import xyz.meowing.krypt.api.dungeons.core.handlers.DungeonScanner
import xyz.meowing.krypt.api.dungeons.core.utils.MapUtils.coordMultiplier
import xyz.meowing.krypt.api.dungeons.core.utils.MapUtils.startCorner

data class DungeonMapPlayer(val dungeonPlayer: DungeonPlayer) {
    var mapX = 0f
    var mapZ = 0f
    var yaw = 0f
    var icon = ""

    fun getRealPos() = BlockPos(
        ((mapX - startCorner.first) / coordMultiplier + DungeonScanner.startX - 15).toInt(),
        //#if MC >= 1.21.9
        //$$ dungeonPlayer.entity?.entityPos?.y?.toInt() ?: 0
        //#else
        dungeonPlayer.entity?.pos?.y?.toInt() ?: 0,
        //#endif
        ((mapZ - startCorner.second) / coordMultiplier + DungeonScanner.startZ - 15).toInt()
    )
}