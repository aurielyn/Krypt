package xyz.meowing.krypt.events.core

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import xyz.meowing.knit.api.events.Event

sealed class WorldEvent {
    class BlockUpdate(
        val pos: BlockPos,
        val old: BlockState,
        val new: BlockState
    ) : Event()
}