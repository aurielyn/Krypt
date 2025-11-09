package xyz.meowing.krypt.mixins;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.meowing.krypt.events.EventBus;
import xyz.meowing.krypt.events.core.WorldEvent;

/**
 * Modified from Skytils
 * Under GPL 3.0 License
 */
@Mixin(WorldChunk.class)
public abstract class MixinChunk {
    @Shadow public abstract BlockState getBlockState(BlockPos pos);

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void onBlockChange(
            BlockPos pos,
            BlockState state,
            int flags,
            CallbackInfoReturnable<BlockState> cir
    ) {
        BlockState old = this.getBlockState(pos);
        if (old != state) {
            EventBus.INSTANCE.post(new WorldEvent.BlockStateChange(pos, old, state));
        }
    }
}