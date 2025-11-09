package xyz.meowing.krypt.api.dungeons.core.utils

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkSectionPos
import net.minecraft.world.chunk.Chunk
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.events.core.WorldEvent

/**
 * Inspired by Skytils
 * Under GPL 3.0 License
 */
@Module
object HeightProvider {
    val heightMap = Long2IntOpenHashMap().also { it.defaultReturnValue(Integer.MIN_VALUE) }

    init {
        EventBus.register<WorldEvent.ChunkLoad> { event ->
            val chunk = event.chunk
            val highestSection = chunk.highestNonEmptySection

            if (highestSection == -1) {
                val bottomY = chunk.bottomY
                val baseX = chunk.pos.startX
                val baseZ = chunk.pos.startZ
                repeat(16) { x ->
                    val worldX = baseX + x
                    repeat(16) { z ->
                        heightMap[BlockPos.asLong(worldX, 0, baseZ + z)] = bottomY
                    }
                }
                return@register
            }

            val highestY = ChunkSectionPos.getBlockCoord(chunk.sectionIndexToCoord(highestSection) + 1) + 15
            val lowestY = chunk.bottomY
            val startX = chunk.pos.startX
            val endX = chunk.pos.endX
            val startZ = chunk.pos.startZ
            val endZ = chunk.pos.endZ
            val mut = BlockPos.Mutable()

            var left = lowestY
            var right = highestY

            while (left < right) {
                var mid = (left + right) / 2

                if (chunk.getBlockState(BlockPos(startX, mid, startZ)).isAir)
                    right = mid - 1
                else
                    left = mid + 1
            }

            fun findHeightHorizontal(x: Int, y: Int, z: Int) {
                var y = y
                if(chunk.getBlockState(BlockPos(x, y, z)).isAir)
                    while(chunk.getBlockState(BlockPos(x, y - 1, z)).isAir)
                        y--
                else
                    while(chunk.getBlockState(BlockPos(x, y + 1, z)).isAir)
                        y++

                heightMap[BlockPos.asLong(x, 0, z)] = y

                if(z + 1 <= endZ)
                    findHeightHorizontal(x, y, z)
            }

            fun findHeightVertical(x: Int, y: Int, z: Int) {
                var y = y
                if(chunk.getBlockState(BlockPos(x, y, z)).isAir)
                    while(chunk.getBlockState(BlockPos(x, y - 1, z)).isAir)
                        y--
                else
                    while(chunk.getBlockState(BlockPos(x, y + 1, z)).isAir)
                        y++

                heightMap[BlockPos.asLong(x, 0, z)] = y

                if(x + 1 <= endX)
                    findHeightVertical(x + 1, y, z)

                findHeightHorizontal(x, y, z + 1)
            }
        }

        EventBus.register<WorldEvent.BlockStateChange> { event ->
            if (event.newState.isAir) return@register
            val pos = event.pos
            val key = BlockPos.asLong(pos.x, 0, pos.z)
            val current = heightMap[key]
            if (current == Integer.MIN_VALUE || pos.y > current) {
                heightMap[key] = pos.y
            }
        }

        EventBus.register<LocationEvent.WorldChange> {
            heightMap.clear()
        }

        EventBus.register<PacketEvent.Received> { event ->
            val packet = event.packet as? UnloadChunkS2CPacket ?: return@register
            val startX = packet.pos.startX
            val endX = packet.pos.endX
            val startZ = packet.pos.startZ
            val endZ = packet.pos.endZ

            for (x in startX..endX) {
                for (z in startZ..endZ) {
                    heightMap.remove(BlockPos.asLong(x, 0, z))
                }
            }
        }
    }

    fun getHeight(pos: BlockPos): Int = heightMap[BlockPos.asLong(pos.x, 0, pos.z)]
    fun getHeight(x: Int, z: Int): Int = heightMap[BlockPos.asLong(x, 0, z)]
}