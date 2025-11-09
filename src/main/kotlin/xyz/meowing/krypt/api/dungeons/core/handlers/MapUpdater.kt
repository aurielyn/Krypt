package xyz.meowing.krypt.api.dungeons.core.handlers

import kotlinx.coroutines.*
import net.minecraft.block.Blocks
import net.minecraft.item.map.MapState
import net.minecraft.util.math.BlockPos
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.core.enums.DungeonMapPlayer
import xyz.meowing.krypt.api.dungeons.core.map.*
import xyz.meowing.krypt.api.dungeons.core.utils.MapUtils.mapX
import xyz.meowing.krypt.api.dungeons.core.utils.MapUtils.mapZ
import xyz.meowing.krypt.api.dungeons.core.utils.MapUtils.yaw
import xyz.meowing.krypt.mixins.AccessorMapState
import xyz.meowing.krypt.utils.StringUtils.equalsOneOf
import java.util.concurrent.ConcurrentHashMap

object MapUpdater {
    private val playerHeadScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val playerJobs = ConcurrentHashMap<String, Job>()

    fun updatePlayers(mapData: MapState) {
        if (DungeonAPI.teammates.isEmpty()) return
        val mapDecorations = (mapData as AccessorMapState).decorations.entries.toList()
        val teammates = DungeonAPI.teammates.filterNot { it.dead }

        teammates.forEach { teammate ->
            val decoration = mapDecorations.find { it.key == teammate.mapIcon.icon }?.value ?: return@forEach
            smoothUpdatePlayer(teammate.mapIcon, decoration.mapX, decoration.mapZ, decoration.yaw - 180f)
        }
    }

    fun onPlayerDeath() {
        playerJobs.forEach { it.value.cancel() }
        playerJobs.clear()
    }

    private fun smoothUpdatePlayer(player: DungeonMapPlayer, targetX: Float, targetZ: Float, targetYaw: Float) {
        if (player.mapX == 0f && player.mapZ == 0f && player.yaw == 0f) {
            player.mapX = targetX
            player.mapZ = targetZ
            player.yaw = targetYaw
            return
        }

        if (player.mapX == targetX && player.mapZ == targetZ && player.yaw == targetYaw) {
            playerJobs.remove(player.dungeonPlayer.name)?.cancel()
            return
        }

        playerHeadScope.launch {
            val oldJob = playerJobs.put(player.dungeonPlayer.name, this.coroutineContext.job)
            oldJob?.cancelAndJoin()

            val startX = player.mapX
            val startZ = player.mapZ
            val startYaw = player.yaw

            val animationDuration = 250L
            val startTime = System.currentTimeMillis()
            var progress = 0f

            while (progress < 1f && isActive) {
                val elapsedTime = System.currentTimeMillis() - startTime
                progress = (elapsedTime.toFloat() / animationDuration).coerceAtMost(1f)

                player.mapX = lerp(startX, targetX, progress).toFloat()
                player.mapZ = lerp(startZ, targetZ, progress).toFloat()
                player.yaw = interpolateYaw(startYaw, targetYaw, progress)

                delay(10)
            }

            if (isActive) {
                player.mapX = targetX
                player.mapZ = targetZ
                player.yaw = targetYaw
            }
        }
    }

    fun updateRooms(mapData: MapState) {
        if (DungeonAPI.inBoss) return
        if (DungeonAPI.completed) return
        if (DungeonAPI.ownPlayer?.dead == true) return
        HotbarMapColorParser.updateMap(mapData)

        for (x in 0..10) {
            for (z in 0..10) {
                val index = z * 11 + x
                val room = DungeonAPI.dungeonList[index]
                val mapTile = HotbarMapColorParser.getTile(x, z)

                if (room is Unknown) {
                    DungeonAPI.dungeonList[index] = mapTile

                    if (mapTile is Room && !mapTile.isSeparator) {
                        ensureRoomInUniqueSet(mapTile, x, z)
                    }
                    continue
                }

                if (room is Room && !room.isSeparator) {
                    ensureRoomInUniqueSet(room, x, z)
                }

                if (mapTile.state.ordinal < room.state.ordinal) {
                    room.state = mapTile.state
                }

                if (mapTile is Room && room is Room && mapTile.data.type != room.data.type) {
                    room.data = mapTile.data
                }

                if (mapTile is Door && room is Door) {
                    if (mapTile.type == DoorType.WITHER && room.type != DoorType.WITHER) {
                        room.type = mapTile.type
                    }
                }

                if (room is Door && room.type.equalsOneOf(DoorType.ENTRANCE, DoorType.WITHER, DoorType.BLOOD)) {
                    if (mapTile is Door && mapTile.type == DoorType.WITHER) {
                        room.opened = false
                    } else if (!room.opened) {
                        val world = client.world
                        if (world?.isChunkLoaded(room.x shr 4, room.z shr 4) == true) {
                            val chunk = world.getChunk(room.x shr 4, room.z shr 4)
                            if (chunk.getBlockState(BlockPos(room.x, 69, room.z)).block == Blocks.AIR) {
                                room.opened = true
                            }
                        } else if (mapTile is Door && mapTile.state == RoomState.DISCOVERED) {
                            if (room.type == DoorType.BLOOD) {
                                val bloodRoom = DungeonAPI.uniqueRooms.find { r ->
                                    r.mainRoom.data.type == RoomType.BLOOD
                                }

                                if (bloodRoom != null && bloodRoom.mainRoom.state != RoomState.UNOPENED) {
                                    room.opened = true
                                }
                            } else {
                                room.opened = true
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ensureRoomInUniqueSet(room: Room, x: Int, z: Int) {
        if (room.uniqueRoom != null) return

        val existingUnique = DungeonAPI.uniqueRooms.find { unique ->
            unique.tiles.any { it.x == room.x && it.z == room.z }
        }

        if (existingUnique != null) room.uniqueRoom = existingUnique else room.addToUnique(z, x)
    }

    private fun lerp(prev: Number, newPos: Number, partialTicks: Number): Double {
        return prev.toDouble() + (newPos.toDouble() - prev.toDouble()) * partialTicks.toDouble()
    }

    private fun interpolateYaw(startYaw: Float, targetYaw: Float, progress: Float): Float {
        var delta = (targetYaw - startYaw) % 360

        if (delta > 180) delta -= 360
        if (delta < -180) delta += 360

        return (startYaw + delta * progress)
    }
}