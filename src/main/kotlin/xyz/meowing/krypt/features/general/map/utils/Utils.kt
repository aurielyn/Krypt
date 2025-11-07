package xyz.meowing.krypt.features.general.map.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.minecraft.client.gui.DrawContext
import net.minecraft.resource.ResourceManager
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.krypt.Krypt
import xyz.meowing.krypt.api.dungeons.utils.Checkmark
import xyz.meowing.krypt.api.dungeons.utils.DoorType
import xyz.meowing.krypt.api.dungeons.utils.RoomType
import xyz.meowing.krypt.features.general.map.DungeonMap
import xyz.meowing.krypt.utils.Render2D
import xyz.meowing.krypt.utils.Render2D.pushPop
import xyz.meowing.krypt.utils.Render2D.width
import java.awt.Color
import java.io.InputStreamReader

object Utils {
    fun scale(floor: Int?): Float {
        if (floor == null) return 1f
        return when (floor) {
            0 -> 6f / 4f
            in 1..3 -> 6f / 5f
            else -> 1f
        }
    }

    val defaultMap: Identifier = Identifier.of(Krypt.NAMESPACE, "krypt/default_map")
    val markerSelf: Identifier = Identifier.of(Krypt.NAMESPACE, "krypt/marker_self")
    val markerOther: Identifier = Identifier.of(Krypt.NAMESPACE, "krypt/marker_other")

    val greenCheck: Identifier = Identifier.of(Krypt.NAMESPACE, "krypt/clear/green_check")
    val whiteCheck: Identifier = Identifier.of(Krypt.NAMESPACE, "krypt/clear/white_check")
    val failedRoom: Identifier = Identifier.of(Krypt.NAMESPACE, "krypt/clear/failed_room")
    val questionMark: Identifier = Identifier.of(Krypt.NAMESPACE, "krypt/clear/question_mark")

    fun getCheckmarks(checkmark: Checkmark): Identifier? = when (checkmark) {
        Checkmark.GREEN -> greenCheck
        Checkmark.WHITE -> whiteCheck
        Checkmark.FAILED -> failedRoom
        Checkmark.UNEXPLORED -> questionMark
        else -> null
    }

    fun getTextColor(check: Checkmark?): String = when (check) {
        null -> "§7"
        Checkmark.WHITE -> "§f"
        Checkmark.GREEN -> "§a"
        Checkmark.FAILED -> "§c"
        else -> "§7"
    }

    val roomTypes = mapOf(
        63 to "Normal",
        30 to "Entrance",
        74 to "Yellow",
        18 to "Blood",
        66 to "Puzzle",
        62 to "Trap"
    )

    fun getClassColor(dClass: String?): Color = when (dClass) {
        "Healer" -> DungeonMap.healerColor
        "Mage" -> DungeonMap.mageColor
        "Berserk" -> DungeonMap.berserkColor
        "Archer" -> DungeonMap.archerColor
        "Tank" -> DungeonMap.tankColor
        else -> Color(0, 0, 0, 255)
    }

    val roomTypeColors: Map<RoomType, Color>
        get() = mapOf(
            RoomType.NORMAL to DungeonMap.normalRoomColor,
            RoomType.PUZZLE to DungeonMap.puzzleRoomColor,
            RoomType.TRAP to DungeonMap.trapRoomColor,
            RoomType.YELLOW to DungeonMap.yellowRoomColor,
            RoomType.BLOOD to DungeonMap.bloodRoomColor,
            RoomType.FAIRY to DungeonMap.fairyRoomColor,
            RoomType.ENTRANCE to DungeonMap.entranceRoomColor,
        )

    val doorTypeColors: Map<DoorType, Color>
        get() = mapOf(
            DoorType.NORMAL to DungeonMap.normalDoorColor,
            DoorType.WITHER to DungeonMap.witherDoorColor,
            DoorType.BLOOD to DungeonMap.bloodDoorColor,
            DoorType.ENTRANCE to DungeonMap.entranceDoorColor
        )

    data class BossMapData(
        val image: String,
        val bounds: List<List<Double>>,
        val widthInWorld: Int,
        val heightInWorld: Int,
        val topLeftLocation: List<Int>,
        val renderSize: Int? = null
    )

    object BossMapRegistry {
        private val gson = Gson()
        private val bossMaps = mutableMapOf<String, List<BossMapData>>()

        init {
            val resourceManager = KnitClient.client.resourceManager
            load(resourceManager)
        }

        fun load(resourceManager: ResourceManager) {
            val id = Identifier.of(Krypt.NAMESPACE, "dungeons/imagedata.json")
            val optional = resourceManager.getResource(id)
            val resource = optional.orElse(null) ?: return

            val reader = InputStreamReader(resource.inputStream)
            val type = object : TypeToken<Map<String, List<BossMapData>>>() {}.type
            val parsed = gson.fromJson<Map<String, List<BossMapData>>>(reader, type)

            bossMaps.putAll(parsed)
        }

        fun getBossMap(floor: Int, playerPos: Vec3d): BossMapData? {
            val maps = bossMaps[floor.toString()] ?: return null
            return maps.firstOrNull { map ->
                (0..2).all { axis ->
                    val min = map.bounds[0][axis]
                    val max = map.bounds[1][axis]
                    val p = listOf(playerPos.x, playerPos.y, playerPos.z)[axis]
                    p in min..max
                }
            }
        }

        fun getAll(): Map<String, List<BossMapData>> = bossMaps
    }

    fun renderNametag(context: DrawContext, name: String, scale: Float) {
        val matrix = context.matrices
        val width = name.width().toFloat()
        val drawX = (-width / 2).toInt()
        val drawY = 0

        val offsets = listOf(
            scale to 0f, -scale to 0f,
            0f to scale, 0f to -scale
        )

        context.pushPop {
            //#if MC >= 1.21.7
            //$$ matrix.scale(scale, scale)
            //$$ matrix.translate(0f, 12f)
            //#else
            matrix.scale(scale, scale, 1f)
            matrix.translate(0f, 12f, 0f)
            //#endif

            for ((dx, dy) in offsets) {
                context.pushPop {
                    //#if MC >= 1.21.7
                    //$$ matrix.translate(dx, dy)
                    //#else
                    matrix.translate(dx, dy, 0f)
                    //#endif
                    Render2D.renderString(context, "§0$name", drawX.toFloat(), drawY.toFloat(), 1f)
                }
            }


            Render2D.renderString(context, name, drawX.toFloat(), drawY.toFloat(), 1f)
        }
    }

    fun typeToColor(type: RoomType): String = when (type) {
        RoomType.NORMAL   -> "7"
        RoomType.PUZZLE   -> "d"
        RoomType.TRAP     -> "6"
        RoomType.YELLOW   -> "e"
        RoomType.BLOOD    -> "c"
        RoomType.FAIRY    -> "d"
        RoomType.RARE     -> "b"
        RoomType.ENTRANCE -> "a"
        RoomType.UNKNOWN  -> "f"
    }

    fun typeToName(type: RoomType): String = when (type) {
        RoomType.NORMAL   -> "Normal"
        RoomType.PUZZLE   -> "Puzzle"
        RoomType.TRAP     -> "Trap"
        RoomType.YELLOW   -> "Yellow"
        RoomType.BLOOD    -> "Blood"
        RoomType.FAIRY    -> "Fairy"
        RoomType.RARE     -> "Rare"
        RoomType.ENTRANCE -> "Entrance"
        RoomType.UNKNOWN  -> "Unknown"
    }
}