package xyz.meowing.krypt.events.core

import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.world.chunk.WorldChunk
import xyz.meowing.knit.api.events.Event

sealed class WorldEvent {
    class ChunkLoad(
        val chunk: WorldChunk
    ) : Event()

    class BlockStateChange(
        val pos: BlockPos,
        val oldState: BlockState,
        val newState: BlockState
    ) : Event()
}