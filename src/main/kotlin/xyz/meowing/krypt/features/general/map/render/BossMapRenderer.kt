package xyz.meowing.krypt.features.general.map.render

import net.minecraft.client.gui.DrawContext
import net.minecraft.util.math.RotationAxis
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.players.DungeonPlayer
import xyz.meowing.krypt.features.general.map.DungeonMap
import xyz.meowing.krypt.features.general.map.utils.Utils
import xyz.meowing.krypt.utils.Render2D
import xyz.meowing.krypt.utils.Render2D.pushPop
import java.util.UUID
import kotlin.math.PI
import kotlin.math.min

object BossMapRenderer {
    private const val MAP_SIZE = 128

    fun renderBossMap(context: DrawContext) {
        val matrix = context.matrices
        val playerPos = KnitPlayer.player?.pos ?: return
        val bossMap = Utils.BossMapRegistry.getBossMap(DungeonAPI.floor?.floorNumber ?: return, playerPos) ?: return
        val texture = bossMap.image

        val textureWidth = 256.0
        val textureHeight = 256.0

        val sizeInWorld = min(bossMap.widthInWorld, bossMap.heightInWorld)
        val renderSize = bossMap.renderSize ?: bossMap.widthInWorld
        val scale = (MAP_SIZE / (textureWidth / bossMap.widthInWorld * renderSize))

        var offsetX = ((playerPos.x - bossMap.topLeftLocation[0]) / sizeInWorld) * MAP_SIZE - MAP_SIZE / 2
        var offsetY = ((playerPos.z - bossMap.topLeftLocation[1]) / sizeInWorld) * MAP_SIZE - MAP_SIZE / 2

        offsetX = offsetX.coerceIn(0.0, maxOf(0.0, textureWidth * scale - MAP_SIZE))
        offsetY = offsetY.coerceIn(0.0, maxOf(0.0, textureHeight * scale - MAP_SIZE))

        context.pushPop {
            //#if MC >= 1.21.7
            //$$ matrix.translate(5f, 5f)
            //#else
            matrix.translate(5f, 5f, 0f)
            //#endif
            context.enableScissor(0, 0, MAP_SIZE, MAP_SIZE)

            Render2D.drawImage(context, net.minecraft.util.Identifier.of("krypt", "krypt/boss/$texture"), (-offsetX).toInt(), (-offsetY).toInt(), (textureWidth * scale).toInt(), (textureHeight * scale).toInt())

            renderBossPlayers(context, bossMap, offsetX, offsetY, sizeInWorld)

            context.disableScissor()
        }
    }

    private fun renderBossPlayers(context: DrawContext, bossMap: Utils.BossMapData, offsetX: Double, offsetY: Double, sizeInWorld: Int) {
        val matrix = context.matrices

        for (player in DungeonAPI.players) {
            if (player == null || (player.dead && player.name != KnitPlayer.name)) continue

            val realX = player.iconX ?: continue
            val realY = player.iconZ ?: continue
            val rotation = player.yaw ?: continue

            val x = ((realX - bossMap.topLeftLocation[0]) / sizeInWorld) * MAP_SIZE - offsetX
            val y = ((realY - bossMap.topLeftLocation[1]) / sizeInWorld) * MAP_SIZE - offsetY

            val ownName = !DungeonMap.showOwnPlayer && player.name == KnitPlayer.name

            if (DungeonAPI.holdingLeaps && DungeonMap.showPlayerNametags && !ownName) {
                context.pushPop {
                    //#if MC >= 1.21.7
                    //$$ matrix.translate(x.toFloat(), y.toFloat())
                    //#else
                    matrix.translate(x.toFloat(), y.toFloat(), 0f)
                    //#endif
                    Utils.renderNametag(context, player.name, 1f / 1.3f)
                }
            }

            renderBossPlayerIcon(context, player, x, y, rotation)
        }
    }

    private fun renderBossPlayerIcon(context: DrawContext, player: DungeonPlayer, x: Double, y: Double, rotation: Float) {
        context.pushPop {
            val matrix = context.matrices

            //#if MC >= 1.21.7
            //$$ matrix.translate(x.toFloat(), y.toFloat())
            //$$ matrix.rotate((rotation * (PI / 180)).toFloat())
            //$$ matrix.scale(1f, 1f)
            //#else
            matrix.translate(x.toFloat(), y.toFloat(), 0f)
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation.toFloat()))
            matrix.scale(1f, 1f, 1f)
            //#endif

            if (DungeonMap.showPlayerHead) {
                val borderColor = if (DungeonMap.iconClassColors) Utils.getClassColor(player.dungeonClass?.displayName) else DungeonMap.playerIconBorderColor

                Render2D.drawRect(context, -6, -6, 12, 12, borderColor)

                val borderSize = DungeonMap.playerIconBorderSize.toFloat()
                //#if MC >= 1.21.7
                //$$ matrix.scale(1f - borderSize, 1f - borderSize)
                //#else
                matrix.scale(1f - borderSize, 1f - borderSize, 1f)
                //#endif
                Render2D.drawPlayerHead(context, -6, -6, 12, player.uuid ?: UUID(0, 0))
            } else {
                val head = if (player.name == KnitPlayer.name) Utils.markerSelf else Utils.markerOther
                Render2D.drawImage(context, head, -4, -5, 7, 10)
            }
        }
    }
}