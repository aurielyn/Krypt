package xyz.meowing.krypt.api.dungeons.handlers

import xyz.meowing.knit.api.KnitClient
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.utils.WorldScanUtils
import xyz.meowing.krypt.api.dungeons.DungeonAPI.rooms
import xyz.meowing.krypt.api.dungeons.enums.DungeonPlayer
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.map.Door
import xyz.meowing.krypt.api.dungeons.enums.map.Room
import xyz.meowing.krypt.api.dungeons.enums.map.RoomType
import xyz.meowing.krypt.api.dungeons.utils.ScanUtils
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.utils.WorldUtils

@Module
object WorldScanner {
    private const val SCAN_INTERVAL = 5
    private const val SCAN_COOLDOWN = 250L
    private const val DOOR_HEIGHT_THRESHOLD = 85
    private const val DUNGEON_MIN_X = -200.0
    private const val DUNGEON_MAX_X = -10.0
    private const val DUNGEON_MIN_Z = -200.0
    private const val DUNGEON_MAX_Z = -10.0
    private const val MAX_ROOM_INDEX = 35

    val availableComponents = ScanUtils.getScanCoord().toMutableList()
    var lastIdx: Int? = null

    private var tickCounter = 0
    private var hasScanned = false
    private var isScanning = false
    private var lastScanTime = 0L

    val shouldScan get() = !isScanning && !hasScanned &&
            System.currentTimeMillis() - lastScanTime >= SCAN_COOLDOWN &&
            availableComponents.isNotEmpty()

    init {
        EventBus.register<LocationEvent.WorldChange> { reset() }

        EventBus.registerIn<TickEvent.Client>(SkyBlockIsland.THE_CATACOMBS) {
            val player = KnitClient.player ?: return@registerIn

            checkPlayerState()

            if (++tickCounter % SCAN_INTERVAL != 0) return@registerIn

            val (x, z) = WorldScanUtils.realCoordToComponent(player.x.toInt(), player.z.toInt())
            val idx = 6 * z + x

            if (idx >= MAX_ROOM_INDEX) return@registerIn

            scan()
            checkRoomState()
            checkDoorState()

            val prevRoom = lastIdx?.let { rooms[it] }
            val currRoom = rooms.getOrNull(idx)

            if (prevRoom != null && currRoom != null && prevRoom != currRoom) {
                EventBus.post(DungeonEvent.Room.Change(prevRoom, currRoom))
            }

            if (lastIdx == idx) return@registerIn

            lastIdx = idx
            DungeonAPI.currentRoom = DungeonAPI.getRoomAt(player.x.toInt(), player.z.toInt())
            DungeonAPI.currentRoom?.let { room ->
                room.explored = true
                room.components.firstOrNull()?.let { (rmx, rmz) ->
                    DungeonAPI.discoveredRooms.remove("$rmx/$rmz")
                }
            }
        }
    }

    fun reset() {
        availableComponents.clear()
        availableComponents += ScanUtils.getScanCoord()
        lastIdx = null
        hasScanned = false
        isScanning = false
        lastScanTime = 0L
        tickCounter = 0
    }

    fun scan() {
        if (!shouldScan) return
        isScanning = true
        var allChunksLoaded = true

        for (idx in availableComponents.indices.reversed()) {
            val (cx, cz, rxz) = availableComponents[idx]
            val (rx, rz) = rxz

            if (!WorldScanUtils.isChunkLoaded(rx, rz)) {
                allChunksLoaded = false
                continue
            }

            val roofHeight = WorldScanUtils.getHighestY(rx, rz) ?: continue
            availableComponents.removeAt(idx)

            if (cx % 2 == 1 || cz % 2 == 1) {
                scanDoorComponent(cx, cz, rx, rz, roofHeight)
                continue
            }

            scanRoomComponent(cx, cz, rx, rz, roofHeight)
        }

        if (allChunksLoaded && availableComponents.isEmpty()) {
            hasScanned = true
        }

        lastScanTime = System.currentTimeMillis()
        isScanning = false
    }

    private fun scanDoorComponent(cx: Int, cz: Int, rx: Int, rz: Int, roofHeight: Int) {
        if (roofHeight >= DOOR_HEIGHT_THRESHOLD) return

        val comp = cx to cz
        val doorIdx = DungeonAPI.getDoorIdx(comp)

        if (DungeonAPI.getDoorAtIdx(doorIdx) != null) return

        val door = Door(rx to rz, comp).apply {
            rotation = if (cz % 2 == 1) 0 else 1
        }
        DungeonAPI.addDoor(door)
    }

    private fun scanRoomComponent(cx: Int, cz: Int, rx: Int, rz: Int, roofHeight: Int) {
        val x = cx / 2
        val z = cz / 2
        val roomIdx = DungeonAPI.getRoomIdx(x to z)

        val room = rooms[roomIdx]?.apply {
            if (height == null) height = roofHeight
            scan()
        } ?: Room(x to z, roofHeight).scan().also {
            rooms[roomIdx] = it
            DungeonAPI.uniqueRooms.add(it)
        }

        scanRoomNeighbors(room, cx, cz, x, z, rx, rz, roofHeight)
    }

    private fun scanRoomNeighbors(room: Room, cx: Int, cz: Int, x: Int, z: Int, rx: Int, rz: Int, roofHeight: Int) {
        for ((dx, dz, cxOff, zOff) in ScanUtils.directions) {
            val doorCx = cx + dx
            val doorCz = cz + dz
            val doorComp = doorCx to doorCz
            val doorIdx = DungeonAPI.getDoorIdx(doorComp)

            if (DungeonAPI.getDoorAtIdx(doorIdx) != null) continue

            val nx = rx + dx
            val nz = rz + dz
            val blockBelow = WorldUtils.getBlockNumericId(nx, roofHeight, nz)
            val blockAbove = WorldUtils.getBlockNumericId(nx, roofHeight + 1, nz)

            if (room.type == RoomType.ENTRANCE && blockBelow != 0) continue
            if (blockBelow == 0 || blockAbove != 0) continue

            val neighborComp = x + cxOff to z + zOff
            val neighborIdx = DungeonAPI.getRoomIdx(neighborComp)
            if (neighborIdx !in rooms.indices) continue

            val neighborRoom = rooms[neighborIdx]

            when {
                neighborRoom == null -> {
                    room.addComponent(neighborComp)
                    rooms[neighborIdx] = room
                }
                neighborRoom != room && neighborRoom.type != RoomType.ENTRANCE -> {
                    DungeonAPI.mergeRooms(neighborRoom, room)
                }
            }
        }
    }

    fun checkPlayerState() {
        val world = KnitClient.world ?: return
        val networkHandler = KnitClient.client.connection

        for (player in DungeonAPI.players) {
            if (player == null) continue

            val entity = world.players().find { it.name.string == player.name }
            val ping = networkHandler?.getPlayerInfo(entity?.uuid)?.latency ?: -1

            player.inRender = ping != -1 && entity != null

            if (player.inRender && entity != null) {
                onPlayerMove(player, entity.x, entity.z, entity.yRot)
            }

            if (ping == -1) continue

            val currRoom = player.currRoom ?: continue
            if (currRoom == player.lastRoom) continue

            player.lastRoom?.players?.remove(player)
            currRoom.players.add(player)
            player.lastRoom = currRoom
        }
    }

    fun checkRoomState() {
        for (room in rooms) {
            if (room?.rotation == null) {
                room?.findRotation()
            }

            if (room != null && room.componentCenters.size < room.components.size) {
                room.findComponentCenters()
                room.findCenter()
            }
        }
    }

    fun checkDoorState() {
        for (door in DungeonAPI.uniqueDoors) {
            if (!door.opened) {
                door.check()
            }
        }
    }

    private fun onPlayerMove(player: DungeonPlayer, x: Double, z: Double, yaw: Float) {
        player.realX = x
        player.realZ = z
        player.yaw = yaw + 180f

        if (x !in DUNGEON_MIN_X..DUNGEON_MAX_X || z !in DUNGEON_MIN_Z..DUNGEON_MAX_Z) return

        val mapSize = ScanUtils.defaultMapSize
        player.iconX = clampMap(x, DUNGEON_MIN_X, DUNGEON_MAX_X, 0.0, mapSize.first.toDouble())
        player.iconZ = clampMap(z, DUNGEON_MIN_Z, DUNGEON_MAX_Z, 0.0, mapSize.second.toDouble())
        player.currRoom = DungeonAPI.getRoomAt(x.toInt(), z.toInt())
    }

    private fun clampMap(n: Double, inMin: Double, inMax: Double, outMin: Double, outMax: Double): Double {
        return when {
            n <= inMin -> outMin
            n >= inMax -> outMax
            else -> (n - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
        }
    }
}