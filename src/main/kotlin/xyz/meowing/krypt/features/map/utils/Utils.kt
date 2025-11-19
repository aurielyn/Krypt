package xyz.meowing.krypt.features.map.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.krypt.Krypt
import xyz.meowing.krypt.utils.rendering.Render2D
import xyz.meowing.krypt.utils.rendering.Render2D.pushPop
import xyz.meowing.krypt.utils.rendering.Render2D.width
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

    val defaultMap: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "krypt/default_map")
    val markerSelf: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "krypt/marker_self")
    val markerOther: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "krypt/marker_other")

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
            val id = ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "dungeons/imagedata.json")
            val optional = resourceManager.getResource(id)
            val resource = optional.orElse(null) ?: return

            val reader = InputStreamReader(resource.open())
            val type = object : TypeToken<Map<String, List<BossMapData>>>() {}.type
            val parsed = gson.fromJson<Map<String, List<BossMapData>>>(reader, type)

            bossMaps.putAll(parsed)
        }

        fun getBossMap(floor: Int, playerPos: Vec3): BossMapData? {
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

    fun renderNametag(context: GuiGraphics, name: String, scale: Float) {
        val matrix = context.pose()
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
}