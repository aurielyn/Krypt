package xyz.meowing.krypt.features.map.render

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.state.MapRenderState
import net.minecraft.core.component.DataComponents
import net.minecraft.world.level.saveddata.maps.MapId
import net.minecraft.world.item.MapItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.KnitPlayer

object ScoreMapRenderer {
    var cachedRenderState = MapRenderState()

    fun getCurrentMap(): ItemStack? {
        val stack = KnitPlayer.player?.inventory?.getItem(8) ?: return null
        if (stack.item !is MapItem) return null
        return stack
    }

    fun getCurrentMapId(stack: ItemStack?): MapId? {
        return stack?.get(DataComponents.MAP_ID)
    }

    fun getCurrentMapState(id: MapId?): MapItemSavedData? {
        if (id == null) return null
        return MapItem.getSavedData(id, KnitClient.world)
    }

    fun getCurrentMapRender(): MapRenderState? {
        val renderState = MapRenderState()

        if (KnitPlayer.player == null || KnitClient.world == null) return null

        val map = getCurrentMap()
        val id = getCurrentMapId(map) ?: return null
        val state = getCurrentMapState(id) ?: return null

        KnitClient.client.mapRenderer.extractRenderState(id,state, renderState)
        cachedRenderState = renderState
        return renderState
    }

    fun renderScoreMap(context: GuiGraphics) {
        val matrix = context.pose()
        val renderState = getCurrentMapRender() ?: cachedRenderState.takeIf { it.texture != null } ?: return

        //#if MC >= 1.21.7
        //$$ matrix.pushMatrix()
        //$$ matrix.translate(5f, 5f)
        //$$ context.submitMapRenderState(renderState)
        //$$ matrix.popMatrix()
        //#else
        val consumer = KnitClient.client.renderBuffers().bufferSource()

        matrix.pushPose()
        matrix.translate(5f, 5f, 5f)
        KnitClient.client.mapRenderer.render(renderState, matrix, consumer, true, LightTexture.FULL_BRIGHT)
        matrix.popPose()
        //#endif
    }
}