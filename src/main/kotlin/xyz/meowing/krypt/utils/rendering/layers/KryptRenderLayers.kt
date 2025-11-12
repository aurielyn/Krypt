package xyz.meowing.krypt.utils.rendering.layers

import it.unimi.dsi.fastutil.doubles.Double2ObjectMap
import it.unimi.dsi.fastutil.doubles.Double2ObjectOpenHashMap
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters
import net.minecraft.client.render.RenderPhase
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer
import net.minecraft.util.TriState
import java.util.OptionalDouble
import java.util.function.DoubleFunction

/**
 * Modified from Devonian's RenderLayers.
 * Original File: [GitHub](https://github.com/Synnerz/devonian/blob/1ad3ce3a40d4f6409eaa5407d1b180ba293edb43/src/main/kotlin/com/github/synnerz/devonian/utils/render/DLayers.kt)
 */
object KryptRenderLayers {
    private val linesThroughWallsLayers: Double2ObjectMap<RenderLayer.MultiPhase> = Double2ObjectOpenHashMap()
    private val linesLayers: Double2ObjectMap<RenderLayer.MultiPhase> = Double2ObjectOpenHashMap()

    private val LINES_THROUGH_WALLS = DoubleFunction { width ->
        RenderLayer.of(
            "lines_through_walls",
            RenderLayer.DEFAULT_BUFFER_SIZE, false, false,
            KryptRenderPipelines.LINES_THROUGH_WALLS,
            MultiPhaseParameters.builder()
                .lineWidth(RenderPhase.LineWidth(OptionalDouble.of(width)))
                .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                .build(false)
        )
    }

    private val LINES = DoubleFunction { width ->
        RenderLayer.of(
            "lines",
            RenderLayer.DEFAULT_BUFFER_SIZE, false, false,
            RenderPipelines.LINES,
            MultiPhaseParameters.builder()
                .lineWidth(RenderPhase.LineWidth(OptionalDouble.of(width)))
                .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                .build(false)
        )
    }

    val FILLED: RenderLayer.MultiPhase = RenderLayer.of(
        "filled", RenderLayer.DEFAULT_BUFFER_SIZE, false, true,
        RenderPipelines.DEBUG_FILLED_BOX,
        MultiPhaseParameters.builder()
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
            .build(false)
    )


    val FILLED_THROUGH_WALLS: RenderLayer.MultiPhase = RenderLayer.of(
        "filled_through_walls", RenderLayer.DEFAULT_BUFFER_SIZE, false, true,
        KryptRenderPipelines.FILLED_THROUGH_WALLS,
        MultiPhaseParameters.builder()
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
            .build(false)
    )

    val BEACON_BEAM_OPAQUE: RenderLayer.MultiPhase = RenderLayer.of(
        "beacon_beam_opaque", 1536, false, true,
        KryptRenderPipelines.BEACON_BEAM_OPAQUE,
        MultiPhaseParameters
            .builder()
            .texture(
                RenderPhase.Texture(
                    BeaconBlockEntityRenderer.BEAM_TEXTURE,
                    //#if MC < 1.21.7
                    TriState.FALSE,
                    //#endif
                    false
                )
            )
            .build(false)
    )

    val BEACON_BEAM_OPAQUE_THROUGH_WALLS: RenderLayer.MultiPhase = RenderLayer.of(
        "beacon_beam_opaque_through_walls", 1536, false, true,
        KryptRenderPipelines.BEACON_BEAM_OPAQUE_THROUGH_WALLS,
        MultiPhaseParameters
            .builder()
            .texture(
                RenderPhase.Texture(
                    BeaconBlockEntityRenderer.BEAM_TEXTURE,
                    //#if MC < 1.21.7
                    TriState.FALSE,
                    //#endif
                    false
                )
            )
            .build(false)
    )

    val BEACON_BEAM_TRANSLUCENT: RenderLayer.MultiPhase = RenderLayer.of(
        "beacon_beam_translucent", 1536, false, true,
        KryptRenderPipelines.BEACON_BEAM_TRANSLUCENT,
        MultiPhaseParameters
            .builder()
            .texture(
                RenderPhase.Texture(
                    BeaconBlockEntityRenderer.BEAM_TEXTURE,
                    //#if MC < 1.21.7
                    TriState.FALSE,
                    //#endif
                    false
                )
            )
            .build(false)
    )

    val BEACON_BEAM_TRANSLUCENT_THROUGH_WALLS: RenderLayer.MultiPhase = RenderLayer.of(
        "devonian_beacon_beam_translucent_esp",1536,false,true,
        KryptRenderPipelines.BEACON_BEAM_TRANSLUCENT_THROUGH_WALLS,
        MultiPhaseParameters
            .builder()
            .texture(
                RenderPhase.Texture(
                    BeaconBlockEntityRenderer.BEAM_TEXTURE,
                    //#if MC < 1.21.7
                    TriState.FALSE,
                    //#endif
                    false
                )
            )
            .build(false)
    )

    fun getLinesThroughWalls(width: Double): RenderLayer.MultiPhase =
        linesThroughWallsLayers.computeIfAbsent(width, LINES_THROUGH_WALLS)

    fun getLines(width: Double): RenderLayer.MultiPhase =
        linesLayers.computeIfAbsent(width, LINES)
}