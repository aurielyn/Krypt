package xyz.meowing.krypt.api.dungeons.core.handlers

import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.registry.tag.BlockTags
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.krypt.Krypt
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.core.map.Room
import xyz.meowing.krypt.api.dungeons.core.map.RoomData
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.utils.LegIDs.getLegID
import xyz.meowing.krypt.utils.NetworkUtils
import xyz.meowing.krypt.utils.StringUtils.equalsOneOf
import xyz.meowing.krypt.utils.WorldUtils.getBlockStateAt
import kotlin.math.floor

@Module
object WorldScanner {
    val roomList = mutableListOf<RoomData>()
    private var tickCounter = 0

    @JvmStatic
    var currentRoom: Room? = null

    @JvmStatic
    var lastKnownRoom: Room? = null

    init {
        EventBus.registerIn<TickEvent.Client> (SkyBlockIsland.THE_CATACOMBS) {
            if (++tickCounter % 5 != 0) return@registerIn
            if (DungeonAPI.inBoss) return@registerIn

            val player = KnitClient.player ?: return@registerIn
            val room = getRoomFromPos(player.blockPos)
            if (currentRoom == room) return@registerIn

            lastKnownRoom = currentRoom
            currentRoom = room

            EventBus.post(DungeonEvent.Room.Change(lastKnownRoom, currentRoom))
        }

        EventBus.register<LocationEvent.WorldChange> {
            currentRoom = null
            lastKnownRoom = null
        }

        NetworkUtils.fetchJson<List<RoomData>>(
            url = "https://raw.githubusercontent.com/Skytils/SkytilsMod/refs/heads/1.x/src/main/resources/assets/catlas/rooms.json",
            onSuccess = { rooms ->
                roomList += rooms
                Krypt.LOGGER.info("[krypt] WorldScanner: Loaded ${rooms.size} rooms from Skytils.")
            },
            onError = { error ->
                Krypt.LOGGER.error("[krypt] WorldScanner: Failed to load room data — ${error.message}")
            }
        )
    }

    fun getRoomData(hash: Int): RoomData? {
        if (hash == -336135931) return roomList.find { it.name == "Tic Tac Toe" }
        return roomList.find { hash in it.cores }
    }

    fun getRoomComponent(pos: Vec3i): Pair<Int, Int> {
        val gx = floor((pos.x + 200 + 0.5) / 32).toInt()
        val gz = floor((pos.z + 200 + 0.5) / 32).toInt()
        return Pair(gx, gz)
    }

    fun getRoomCorner(pair: Pair<Int, Int>): Pair<Int, Int> {
        return Pair(
            -200 + pair.first * 32,
            -200 + pair.second * 32
        )
    }

    fun getRoomCenter(pair: Pair<Int, Int>): Pair<Int, Int> {
        return Pair(pair.first + 15, pair.second + 15)
    }

    fun getRoomCenterAt(pos: Vec3i): BlockPos {
        return getRoomCenter(getRoomCorner(getRoomComponent(pos))).let {
            BlockPos(it.first, 0, it.second)
        }
    }

    fun getRoomFromPos(pos: BlockPos) = DungeonAPI.dungeonList.filterIsInstance<Room>().find { room ->
        room.getRoomComponent() == getRoomComponent(pos)
    }?.uniqueRoom?.mainRoom

    fun getEntityRoom(entity: Entity) = getRoomFromPos(entity.blockPos)

    fun getRoomFromPos(pos: Vec3d) = getRoomFromPos(BlockPos(pos.x.toInt(), pos.y.toInt(), pos.z.toInt()))

    fun getCore(x: Int, z: Int): Int {
        val sb = StringBuilder(150)
        val world = client.world ?: return 0
        val chunk = world.getChunk(BlockPos(x, 0, z))

        for (y in 140 downTo 12) {
            val id = chunk.getBlockState(BlockPos(x, y, z)).getLegID()
            if (id.equalsOneOf(5, 54, 146)) continue
            sb.append(id)
        }
        return sb.toString().hashCode()
    }

    fun getRotation(center: BlockPos, relativeCoords: Map<Block, BlockPos>): Int? {
        relativeCoords.forEach { (block, coords) ->
            for (i in 0 .. 3) {
                val pos = getRealCoord(coords, center, i * 90)
                if (getBlockStateAt(pos.x, pos.y, pos.z) == block) {
                    return i * 90
                }
            }
        }
        return null
    }

    fun BlockPos.rotate(degree: Int): BlockPos {
        return when ((degree % 360 + 360) % 360) {
            0 -> BlockPos(x, y, z)
            90 -> BlockPos(z, y, - x)
            180 -> BlockPos(- x, y, - z)
            270 -> BlockPos(- z, y, x)
            else -> BlockPos(x, y, z)
        }
    }

    fun getRealCoord(pos: BlockPos, roomCenter: BlockPos, rotation: Int): BlockPos {
        return pos.rotate(rotation).add(roomCenter.x, 0, roomCenter.z)
    }

    fun getHighestBlockAt(x: Int, z: Int): Int? {
        for (y in 255 downTo 0) {
            val block = getBlockStateAt(x, y, z)
            if (block == Blocks.AIR || block.isIn(BlockTags.WOOL)) continue
            return y
        }

        return null
    }
}