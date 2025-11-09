package xyz.meowing.krypt.api.dungeons.core.handlers

import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.util.math.BlockPos
import net.minecraft.world.Heightmap
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.core.map.*
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.utils.LegIDs.getLegID
import kotlin.math.floor

object DungeonScanner {
    const val roomSize = 32
    const val startX = -185
    const val startZ = -185

    private val halfRoomSize = floor((roomSize - 1.0) / 2.0).toInt()
    private val mc = MinecraftClient.getInstance()

    val clayBlocksCorners = listOf(
        Pair(-halfRoomSize, -halfRoomSize),
        Pair(halfRoomSize, -halfRoomSize),
        Pair(halfRoomSize, halfRoomSize),
        Pair(-halfRoomSize, halfRoomSize)
    )

    var lastScanTime = 0L
    var isScanning = false
    var hasScanned = false

    val shouldScan get() = !isScanning && !hasScanned && System.currentTimeMillis() - lastScanTime >= 250 && DungeonAPI.dungeonFloor?.floorNumber != null

    init {
        EventBus.register<LocationEvent.WorldChange> {
            hasScanned = false
            isScanning = false
            lastScanTime = 0L
        }
    }

    fun scan() {
        if (!shouldScan) return
        isScanning = true
        var allChunksLoaded = true

        for (x in 0..10) {
            for (z in 0..10) {
                val xPos = startX + x * (roomSize shr 1)
                val zPos = startZ + z * (roomSize shr 1)

                val world = mc.world ?: continue

                if (!world.isChunkLoaded(xPos shr 4, zPos shr 4)) {
                    allChunksLoaded = false
                    continue
                }

                val roomInGrid = DungeonAPI.dungeonList[x + z * 11]
                if (roomInGrid !is Unknown && (roomInGrid as? Room)?.data?.name != "Unknown") {
                    if (roomInGrid is Room) {
                        roomInGrid.highestBlock = WorldScanner.getHighestBlockAt(xPos, zPos)
                        roomInGrid.findRotation()
                    }
                    continue
                }

                scanRoom(xPos, zPos, z, x)?.let {
                    DungeonAPI.dungeonList[z * 11 + x] = it
                }
            }
        }

        if (allChunksLoaded) {
            DungeonAPI.roomCount = DungeonAPI.dungeonList.filter { it is Room && !it.isSeparator }.size
            hasScanned = true
        }

        lastScanTime = System.currentTimeMillis()
        isScanning = false
    }

    private fun scanRoom(x: Int, z: Int, row: Int, column: Int): Tile? {
        val world = mc.world ?: return null
        val chunk = world.getChunk(x shr 4, z shr 4)
        val height = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, x and 15, z and 15)

        if (height == 0) return null

        val rowEven = row and 1 == 0
        val columnEven = column and 1 == 0

        return when {
            rowEven && columnEven -> {
                val roomCore = WorldScanner.getCore(x, z)
                Room(x, z, WorldScanner.getRoomData(roomCore) ?: return null).apply {
                    addToUnique(row, column)
                    core = roomCore
                    highestBlock = height
                    findRotation()
                }
            }

            !rowEven && !columnEven -> {
                DungeonAPI.dungeonList[column - 1 + (row - 1) * 11].let {
                    if (it is Room) {
                        Room(x, z, it.data).apply {
                            isSeparator = true
                            addToUnique(row, column)
                        }
                    } else null
                }
            }

            height == 68 || height == 74 || height == 82 -> {
                val blockState = world.getBlockState(BlockPos(x, 69, z))
                val adjacentRoom = getAdjacentRoom(row, column, rowEven) as? Room

                val doorType = when {
                    blockState.block == Blocks.COAL_BLOCK -> DoorType.WITHER
                    blockState.getLegID() == 97 -> DoorType.ENTRANCE
                    blockState.getLegID() == 159 -> DoorType.BLOOD
                    else -> DoorType.NORMAL
                }

                Door(x, z, doorType).apply {
                    if (adjacentRoom != null && adjacentRoom.state == RoomState.UNOPENED) {
                        state = RoomState.UNOPENED
                    }
                }
            }

            else -> {
                DungeonAPI.dungeonList[if (rowEven) row * 11 + column - 1 else (row - 1) * 11 + column].let {
                    when {
                        it !is Room -> null
                        it.data.type == RoomType.ENTRANCE -> Door(x, z, DoorType.ENTRANCE)
                        else -> Room(x, z, it.data).apply { isSeparator = true }
                    }
                }
            }
        }
    }

    private fun getAdjacentRoom(row: Int, column: Int, rowEven: Boolean): Tile? {
        val index = if (rowEven) row * 11 + column - 1 else (row - 1) * 11 + column
        return DungeonAPI.dungeonList.getOrNull(index)
    }
}