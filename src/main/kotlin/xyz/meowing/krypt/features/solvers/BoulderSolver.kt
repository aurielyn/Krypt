package xyz.meowing.krypt.features.solvers

import net.minecraft.block.Blocks
import net.minecraft.client.world.ClientWorld
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.world.EmptyBlockView
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.KnitPlayer.player
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.utils.WorldScanUtils
import xyz.meowing.krypt.api.dungeons.utils.WorldScanUtils.getRealCoord
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.NetworkUtils
import xyz.meowing.krypt.utils.WorldUtils
import xyz.meowing.krypt.utils.rendering.Render3D
import java.awt.Color

/**
 * Contains modified code from Noamm's boulder solver.
 *
 * Original File: [GitHub](https://github.com/Noamm9/NoammAddons/blob/master/src/main/kotlin/noammaddons/features/impl/dungeons/solvers/puzzles/BoulderSolver.kt)
 */
@Module
object BoulderSolver : Feature(
    "boulderSolver",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private data class BoulderBox(val box: BlockPos, val click: VoxelShape, val render: BlockPos, val clickPos: BlockPos)

    private val boulderSolutions = mutableMapOf<String, List<List<Double>>>()
    private var currentSolution = mutableListOf<BoulderBox>()
    private val bottomLeftBox = BlockPos(-9, 65, -9)

    private var inBoulder = false
    private var roomCenter = BlockPos(-1, -1, -1)
    private var rotation = 0

    private var startTime: Long? = null

    private val boxColor by ConfigDelegate<Color>("boulderSolver.boxColor")
    private val clickColor by ConfigDelegate<Color>("boulderSolver.clickColor")
    private val showAll by ConfigDelegate<Boolean>("boulderSolver.showAll")

    init {
        NetworkUtils.fetchJson<Map<String, List<List<Double>>>>(
            url = "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/BoulderSolutions.json",
            onSuccess = { boulderSolutions.putAll(it) },
            onError = { }
        )
    }

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Boulder solver",
                "Shows which boulders to click in order",
                "Solvers",
                ConfigElement(
                    "boulderSolver",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Box color",
                ConfigElement(
                    "boulderSolver.boxColor",
                    ElementType.ColorPicker(Color(255, 0, 0, 127))
                )
            )
            .addFeatureOption(
                "Click color",
                ConfigElement(
                    "boulderSolver.clickColor",
                    ElementType.ColorPicker(Color(0, 255, 0, 255))
                )
            )
            .addFeatureOption(
                "Show all boxes",
                ConfigElement(
                    "boulderSolver.showAll",
                    ElementType.Switch(false)
                )
            )
    }

    override fun initialize() {
        register<DungeonEvent.Room.Change> { event ->
            if (event.new.name != "Boulder") return@register

            inBoulder = true
            rotation = 360 - (event.new.rotation ?: 0) + 180

            player?.let { p ->
                val (centerX, centerZ) = WorldScanUtils.getRoomCenter(p.x.toInt(), p.z.toInt())
                roomCenter = BlockPos(centerX, 0, centerZ)
            }

            solve()
        }

        register<DungeonEvent.Room.Change> { event ->
            if (inBoulder && event.new.name != "Boulder") reset()
        }

        register<LocationEvent.WorldChange> { reset() }

        register<RenderEvent.World.Last> { event ->
            if (!inBoulder || currentSolution.isEmpty()) return@register

            val boxes = if (showAll) currentSolution else listOf(currentSolution.first())

            boxes.forEach { box ->
                val boxArea = Box(
                    box.box.x - 1.0, box.box.y - 1.0, box.box.z - 1.0,
                    box.box.x + 2.0, box.box.y + 2.0, box.box.z + 2.0
                )

                Render3D.drawSpecialBB(
                    boxArea,
                    boxColor,
                    event.context.consumers(),
                    event.context.matrixStack(),
                    phase = true
                )

                Render3D.drawFilledShapeVoxel(
                    box.click.offset(box.clickPos),
                    clickColor,
                    event.context.consumers(),
                    event.context.matrixStack(),
                )
            }
        }

        register<PacketEvent.Sent> { event ->
            if (!inBoulder) return@register

            val packet = event.packet as? PlayerInteractBlockC2SPacket ?: return@register
            val blockPos = packet.blockHitResult.blockPos
            val block = client.world?.getBlockState(blockPos)?.block ?: return@register

            when (block) {
                Blocks.OAK_WALL_SIGN, Blocks.STONE_BUTTON -> currentSolution.find { it.click == blockPos }?.let { currentSolution.remove(it) }
                Blocks.CHEST -> reset()
            }
        }
    }

    private fun solve() {
        val world = KnitClient.world ?: return
        val (sx, sy, sz) = bottomLeftBox.let { Triple(it.x, it.y, it.z) }
        var pattern = ""

        for (z in 0..5) {
            for (x in 0..6) {
                val pos = getRealCoord(BlockPos(sx + x * 3, sy, sz + z * 3), roomCenter, rotation)
                val block = WorldUtils.getBlockStateAt(pos.x, pos.y, pos.z)?.block
                pattern += if (block == Blocks.AIR) "0" else "1"
            }
        }

        currentSolution = boulderSolutions[pattern]?.map { sol ->
            val box = getRealCoord(BlockPos(sol[0].toInt(), sy, sol[1].toInt()), roomCenter, rotation)
            val clickPos = getRealCoord(BlockPos(sol[2].toInt(), sy, sol[3].toInt()), roomCenter, rotation)
            val click = getVoxelShape(clickPos, world)
            val render = getRealCoord(BlockPos(sol[4].toInt(), sy, sol[5].toInt()), roomCenter, rotation)
            BoulderBox(box, click, render, clickPos)
        }?.toMutableList() ?: mutableListOf()
    }

    private fun reset() {
        inBoulder = false
        roomCenter = BlockPos(-1, -1, -1)
        rotation = 0
        currentSolution.clear()
        startTime = null
    }

    private fun getVoxelShape(pos: BlockPos, world: ClientWorld): VoxelShape {
        return world.getBlockState(pos).getOutlineShape(
            EmptyBlockView.INSTANCE,
            pos
        )
    }
}