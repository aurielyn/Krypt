package xyz.meowing.krypt.utils

import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos
import xyz.meowing.knit.api.KnitClient

object WorldUtils {
    fun getBlockStateAt(x: Int, y: Int, z: Int): BlockState {
        val world = KnitClient.world ?: return Blocks.AIR.defaultState
        return world.getBlockState(BlockPos(x, y, z))
    }

    fun getBlockNumericId(x: Int, y: Int, z: Int): Int {
        val state = getBlockStateAt(x, y, z)
        return LegIDs.getLegacyId(state)
    }

    fun checkIfAir(x: Int, y: Int, z: Int): Int {
        val state = getBlockStateAt(x, y, z)
        if (state.isAir) return 0

        return LegIDs.getLegacyId(state)
    }
}