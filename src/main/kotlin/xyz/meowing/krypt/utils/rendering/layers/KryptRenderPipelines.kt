package xyz.meowing.krypt.utils.rendering.layers

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderPipelines
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.resources.ResourceLocation
import xyz.meowing.krypt.Krypt

/**
 * Modified from Devonian's RenderPipelines.
 * Original File: [GitHub](https://github.com/Synnerz/devonian/blob/1ad3ce3a40d4f6409eaa5407d1b180ba293edb43/src/main/kotlin/com/github/synnerz/devonian/utils/render/DPipelines.kt
 */
object KryptRenderPipelines {
    val LINES_THROUGH_WALLS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "lines_through_walls"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    )

    val FILLED_THROUGH_WALLS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "filled_through_walls"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    )

    val BEACON_BEAM_OPAQUE: RenderPipeline = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation("krypt/beacon_beam_opaque")
        .build()

    val BEACON_BEAM_OPAQUE_THROUGH_WALLS: RenderPipeline = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation("krypt/beacon_beam_opaque_through_walls")
        .withDepthWrite(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .build()

    val BEACON_BEAM_TRANSLUCENT: RenderPipeline = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation("krypt/beacon_beam_translucent")
        .withDepthWrite(false)
        .withBlend(BlendFunction.TRANSLUCENT)
        .build()

    val BEACON_BEAM_TRANSLUCENT_THROUGH_WALLS: RenderPipeline = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation("krypt/beacon_beam_translucent_through_walls")
        .withDepthWrite(false)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .build()

    fun shaderPath(name: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, name)
}