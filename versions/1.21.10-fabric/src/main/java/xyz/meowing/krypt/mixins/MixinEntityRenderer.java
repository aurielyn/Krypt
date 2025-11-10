package xyz.meowing.krypt.mixins;

import xyz.meowing.krypt.events.EventBus;
import xyz.meowing.krypt.events.core.RenderEvent;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity, S extends EntityRenderState> {

    @Unique
    private T krypt$currentEntity;

    @Inject(
        method = "getAndUpdateRenderState(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/entity/state/EntityRenderState;",
        at = @At("HEAD")
    )
    private void krypt$captureEntity(T entity, float tickProgress, CallbackInfoReturnable<S> cir) {
        this.krypt$currentEntity = entity;
    }

    @Inject(
        method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void krypt$onEntityRenderPre(
        S renderState,
        MatrixStack matrices,
        OrderedRenderCommandQueue queue,
        CameraRenderState cameraState,
        CallbackInfo callbackInfo
    ) {
        if (this.krypt$currentEntity == null) return;
        RenderEvent.Entity.Pre event = new RenderEvent.Entity.Pre(this.krypt$currentEntity, matrices, null, renderState.light);
        EventBus.INSTANCE.post(event);
        if (event.getCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(
        method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
        at = @At("TAIL")
    )
    private void krypt$onEntityRenderPost(
        S renderState,
        MatrixStack matrices,
        OrderedRenderCommandQueue queue,
        CameraRenderState cameraState,
        CallbackInfo callbackInfo
    ) {
        if (this.krypt$currentEntity == null) return;
        RenderEvent.Entity.Post event = new RenderEvent.Entity.Post(this.krypt$currentEntity, matrices, null, renderState.light);
        EventBus.INSTANCE.post(event);
    }
}