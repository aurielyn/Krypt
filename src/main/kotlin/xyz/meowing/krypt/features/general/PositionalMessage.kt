package xyz.meowing.krypt.features.general

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.util.CommonColors
import net.minecraft.world.phys.Vec3
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.command.Commodore
import xyz.meowing.knit.api.command.utils.GreedyString
import xyz.meowing.krypt.annotations.Command
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.data.StoredFile
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.rendering.Render3D
import xyz.meowing.knit.api.KnitPlayer.player
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.events.core.TickEvent

@Module
object PositionalMessage : Feature(
    "posMsg",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    override fun addConfig() {
        ConfigManager.addFeature(
            "Positional message",
            "use \"/krypt pos help\" for more info",
            "General",
            ConfigElement("posMsg", ElementType.Switch(false))
        )
    }

    private data class PosMsg(val pos: Vec3, val radius: Float, val message: String, var sentMessage: Boolean = false, var radiusSquared: Float = radius * radius)

    private val configCache: HashMap<String, MutableList<PosMsg>> = HashMap<String, MutableList<PosMsg>>()
    private var posMsgList: MutableList<PosMsg> = mutableListOf<PosMsg>()
    private val posMsgData = StoredFile("features/PosMsg")
    var config by posMsgData.jsonObject("posMsgData")
    private var currentConfig: String = "default"

    override fun initialize() {
        if(!config.has("config")) config.addProperty("config", "default")
        switchConfig(config.get("config").asString)
        register<RenderEvent.World.Last> { event ->
            if (!DungeonAPI.inBoss) return@register

            posMsgList.forEach { data ->
                Render3D.drawFilledCircle(
                    consumers = event.context.consumers(),
                    matrixStack = event.context.matrixStack(),
                    center = data.pos,
                    radius = data.radius,
                    segments = 256,
                    borderColor = CommonColors.WHITE,
                    fillColor = CommonColors.RED
                )
            }
        }
        register<TickEvent.Client> { _ ->
            if(!DungeonAPI.inBoss) return@register
            val playerPos = player?.position() ?: return@register

            for (entry in posMsgList) {
                if (isInsideCircle(playerPos, entry.pos, entry.radiusSquared)) {
                    if (!entry.sentMessage) {
                        KnitChat.sendMessage("/pc " + entry.message)
                        entry.sentMessage = true
                    }
                } else {
                    entry.sentMessage = false
                }
            }
        }
    }

    fun addPos(pos: Vec3, radius: Float, message: String) {
        KnitChat.fakeMessage("Added a new position!")
        posMsgList.add(PosMsg(pos.round(), radius, message))
    }

    fun addPos(radius: Float, message: String) {
        val player = player ?: return
        addPos(player.position(), radius, message)
    }

    fun removePos(index: Int) {
        if(posMsgList.size < index) {
            KnitChat.fakeMessage("Index exceeds current list size: ${posMsgList.size}")
            return
        }

        KnitChat.fakeMessage("Removed the $index entry")
        posMsgList.removeAt(index)
    }

    fun save() {
        KnitChat.fakeMessage("Saved config!")
        posMsgList.forEach { element ->
            config[currentConfig].asJsonArray.add(createJsonObject(element))
        }

        config.addProperty("config", currentConfig)
        posMsgData.forceSave()
    }

    fun saveConfig() {
        config.addProperty("config", currentConfig)
        posMsgData.forceSave()
    }

    fun switchConfig(newConfig: String) {
        currentConfig = newConfig
        saveConfig()
        KnitChat.fakeMessage("Switched to $newConfig!")

        configCache[newConfig]?.let {
            posMsgList = it
            return
        }

        posMsgList = mutableListOf()

        if (!config.has(newConfig)) {
            config.add(newConfig, JsonArray())
            configCache[newConfig] = posMsgList
            return
        }

        config.get(newConfig).asJsonArray.forEach { element ->
            posMsgList.add(getDataClass(element.asJsonObject))
        }

        configCache[newConfig] = posMsgList
    }

    fun listEntries() {
        for(i in 0 until posMsgList.size) {
            KnitChat.fakeMessage("$i: x: ${posMsgList[i].pos.x} " +
                    "y: ${posMsgList[i].pos.y} " +
                    "z: ${posMsgList[i].pos.z} " +
                    "radius: ${posMsgList[i].radius} " +
                    "message: ${posMsgList[i].message} "
            )
        }
    }

    fun listConfigs() {
        config.entrySet().forEach { entry ->
            KnitChat.fakeMessage(entry.key)
        }
    }

    private fun createJsonObject(data: PosMsg): JsonObject {
        val jsonObject = JsonObject()

        jsonObject.addProperty(PosMsgProperties.X.toString(), data.pos.x)
        jsonObject.addProperty(PosMsgProperties.Y.toString(), data.pos.y)
        jsonObject.addProperty(PosMsgProperties.Z.toString(), data.pos.z)
        jsonObject.addProperty(PosMsgProperties.RADIUS.toString(), data.radius)
        jsonObject.addProperty(PosMsgProperties.MESSAGE.toString(), data.message)

        return jsonObject
    }

    private fun getDataClass(data: JsonObject): PosMsg {
        return PosMsg(
            Vec3(
                data.get((PosMsgProperties.X.toString())).asDouble,
                data.get(PosMsgProperties.Y.toString()).asDouble,
                data.get(PosMsgProperties.Z.toString()).asDouble),
            data.get(PosMsgProperties.RADIUS.toString()).asFloat,
            data.get(PosMsgProperties.MESSAGE.toString()).asString
        )
    }

    private fun Double.round(): Double = String.format("%.3f", this).toDouble()
    private fun Vec3.round(): Vec3 = Vec3(x.round(), y.round(), z.round())

    private fun isInsideCircle(playerPos: Vec3, center: Vec3, radiusSquared: Float): Boolean {
        val x = playerPos.x - center.x
        val y = playerPos.y - center.y
        val z = playerPos.z - center.z
        return x * x + y * y + z * z <= radiusSquared
    }
}

@Command
object CommandHandler : Commodore("krypt") {
    init {
        literal("pos") {

            literal("help") {
                runs {
                    KnitChat.fakeMessage("List of commands: \n" +
                            "at: x y z radius message | adds a new point at x y z\n" +
                            "remove: index\n" +
                            "list: current or configs\n" +
                            "save\n" +
                            "add: radius message | adds a new point at current position\n" +
                            "switch: new config\n"
                    )
                }
            }

            literal("at") {
                runs { x: Double, y: Double, z: Double, radius: Float, message: GreedyString ->
                    PositionalMessage.addPos(Vec3(x, y, z), radius, message.string)
                }
            }

            literal("remove") {
                runs { index: Int ->
                    PositionalMessage.removePos(index)
                }
            }

            literal("list") {
                literal("current") {
                    runs {
                        PositionalMessage.listEntries()
                    }
                }

                literal("configs") {
                    runs {
                        PositionalMessage.listConfigs()
                    }
                }
            }

            literal("save") {
                runs {
                    PositionalMessage.save()
                }
            }

            literal("add") {
                runs { radius: Float, message: GreedyString ->
                    PositionalMessage.addPos(radius, message.string)
                }
            }

            literal("switch") {
                runs { newConfig: String ->
                    PositionalMessage.switchConfig(newConfig)
                }
            }
        }
    }
}

private enum class PosMsgProperties(private val s: String) {
    X("x"),
    Y("y"),
    Z("z"),
    RADIUS("radius"),
    MESSAGE("message");

    override fun toString() = s
}