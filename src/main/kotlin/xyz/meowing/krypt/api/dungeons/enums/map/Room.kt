package xyz.meowing.krypt.api.dungeons.enums.map

import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import xyz.meowing.krypt.api.dungeons.enums.DungeonPlayer
import xyz.meowing.krypt.api.dungeons.handlers.RoomRegistry
import xyz.meowing.krypt.api.dungeons.utils.ScanUtils
import xyz.meowing.krypt.api.dungeons.utils.WorldScanUtils
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.utils.WorldUtils
import kotlin.properties.Delegates

class Room(
    initialComponent: Pair<Int, Int>,
    var height: Int? = null
) {
    val components = mutableListOf<Pair<Int, Int>>()
    val realComponents = mutableListOf<Pair<Int, Int>>()
    val cores = mutableListOf<Int>()

    var roomData: RoomMetadata? = null
    var shape: RoomShape = RoomShape.UNKNOWN
    var explored = false

    var checkmark by Delegates.observable(Checkmark.UNDISCOVERED) { _, oldValue, newValue ->
        if (oldValue == newValue) return@observable
        if (name == "Unknown") return@observable

        val roomPlayers = players.toList()

        EventBus.post(DungeonEvent.Room.StateChange(this, oldValue, newValue, roomPlayers))
    }

    var players: MutableSet<DungeonPlayer> = mutableSetOf()

    var name: String? = null
    var corner: Triple<Int, Int, Int>? = null
    var center: Triple<Double, Double, Double>? = null
    var componentCenters: List<Triple<Double, Double, Double>> = emptyList()
    var rotation: RoomRotations = RoomRotations.NONE
    var type: RoomType = RoomType.UNKNOWN
    var secrets: Int = 0
    var secretsFound: Int = 0
    var crypts: Int = 0

    var clearTime = 0L

    init {
        addComponents(listOf(initialComponent))
    }

    fun addComponent(comp: Pair<Int, Int>, update: Boolean = true): Room {
        if (!components.contains(comp)) components += comp
        if (update) update()
        return this
    }

    fun addComponents(comps: List<Pair<Int, Int>>): Room {
        comps.forEach { addComponent(it, update = false) }
        update()
        return this
    }

    fun hasComponent(x: Int, z: Int): Boolean {
        return components.any { it.first == x && it.second == z }
    }

    fun update() {
        components.sortWith(compareBy({ it.first }, { it.second }))
        realComponents.clear()
        realComponents += components.map { WorldScanUtils.componentToRealCoord(it.first, it.second) }
        scan()
        shape = RoomShape.fromComponents(components)
        corner = null
        rotation = RoomRotations.NONE
    }

    fun scan(): Room {
        for ((x, z) in realComponents) {
            if (height == null) height = WorldScanUtils.getHighestY(x, z)
            val core = WorldScanUtils.getCore(x, z)
            cores += core
            loadFromCore(core)
        }
        return this
    }

    private fun loadFromCore(core: Int): Boolean {
        val data = RoomRegistry.getByCore(core) ?: return false
        loadFromData(data)
        return true
    }

    fun loadFromData(data: RoomMetadata) {
        roomData = data
        name = data.name
        type = RoomType.fromRoomData(data) ?: RoomType.NORMAL
        secrets = data.secrets
        crypts = data.crypts
    }

    fun loadFromMapColor(color: Byte): Room {
        type = RoomType.fromMapColor(color.toInt()) ?: RoomType.UNKNOWN
        when (type) {
            RoomType.BLOOD -> RoomRegistry.getAll().find { it.type == "Blood" }?.let { loadFromData(it) }
            RoomType.ENTRANCE -> RoomRegistry.getAll().find { it.type == "Entrance" }?.let { loadFromData(it) }
            else -> {}
        }
        return this
    }

    fun findRotation(): Room {
        val currentHeight = height ?: return this

        if (type == RoomType.FAIRY) {
            rotation = RoomRotations.SOUTH
            val (x, z) = realComponents.first()
            corner = Triple(x - ScanUtils.halfRoomSize, height!!, z - ScanUtils.halfRoomSize)
            return this
        }

        val horizontals = Direction.entries.filter { it.axis.isHorizontal }

        rotation = RoomRotations.entries.dropLast(1).find { rot ->
            realComponents.any { (rx, rz) ->
                val checkX = rx + rot.x
                val checkZ = rz + rot.z

                if (!WorldScanUtils.isChunkLoaded(checkX, checkZ)) return@any false

                val state = WorldUtils.getBlockStateAt(checkX, currentHeight, checkZ) ?: return@any false

                state.`is`(Blocks.BLUE_TERRACOTTA) && horizontals.all { facing ->
                    val offsetX = if (facing.axis == Direction.Axis.X) facing.stepX else 0
                    val offsetZ = if (facing.axis == Direction.Axis.Z) facing.stepZ else 0
                    val neighborState = WorldUtils.getBlockStateAt(checkX + offsetX, currentHeight, checkZ + offsetZ)
                    neighborState?.block in setOf(Blocks.AIR, Blocks.BLUE_TERRACOTTA, null)
                }.also { isValid ->
                    if (isValid) {
                        corner = Triple(checkX, currentHeight, checkZ)
                    }
                }
            }
        } ?: RoomRotations.NONE
        return this
    }

    fun findCenter(): Room {
        if (realComponents.isEmpty() || height == null) return this

        val minX = realComponents.minOf { it.first }
        val maxX = realComponents.maxOf { it.first }
        val minZ = realComponents.minOf { it.second }
        val maxZ = realComponents.maxOf { it.second }

        center = Triple(
            (minX + maxX) / 2.0,
            height!!.toDouble(),
            (minZ + maxZ) / 2.0
        )
        return this
    }

    fun findComponentCenters(): Room {
        val currentHeight = height ?: return this

        componentCenters = realComponents.map { (x, z) ->
            Triple(
                x.toDouble(),
                currentHeight.toDouble(),
                z.toDouble(),
            )
        }
        return this
    }

    fun fromWorldPos(pos: Triple<Double, Double, Double>): Triple<Int, Int, Int>? {
        if (corner == null || rotation == RoomRotations.NONE) return null
        val rel = Triple(
            (pos.first - corner!!.first).toInt(),
            (pos.second - corner!!.second).toInt(),
            (pos.third - corner!!.third).toInt()
        )
        return WorldScanUtils.rotateCoord(rel, rotation.degrees)
    }

    fun toWorldPos(local: Triple<Int, Int, Int>): Triple<Int, Int, Int>? {
        if (corner == null || rotation == RoomRotations.NONE) return null
        val rotated = WorldScanUtils.rotateCoord(local, 360 - rotation.degrees)
        return Triple(
            rotated.first + corner!!.first,
            rotated.second,
            rotated.third + corner!!.third
        )
    }
}