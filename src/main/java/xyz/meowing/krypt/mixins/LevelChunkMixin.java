package xyz.meowing.krypt.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.meowing.krypt.events.EventBus;
import xyz.meowing.krypt.events.core.WorldEvent;

/**
 * Yeeted from OdinFabric
 * Under GPL 3.0 license.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Shadow
    public abstract BlockState getBlockState(BlockPos pos);

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void onBlockChange(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        BlockState old = this.getBlockState(pos);
        if (old != state) EventBus.INSTANCE.post(new WorldEvent.BlockUpdate(pos, old, state));
    }
}