package xyz.meowing.krypt.api.dungeons.core.utils

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkSectionPos
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

            for (x in startX..endX) {
                for (z in startZ..endZ) {
                    mut.set(x, 0, z)
                    for (y in highestY downTo lowestY) {
                        if (!chunk.getBlockState(mut.setY(y)).isAir) {
                            heightMap[BlockPos.asLong(x, 0, z)] = y
                            break
                        }
                    }
                }
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