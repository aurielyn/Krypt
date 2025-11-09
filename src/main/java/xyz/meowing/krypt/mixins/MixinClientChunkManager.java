package xyz.meowing.krypt.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.meowing.krypt.events.EventBus;
import xyz.meowing.krypt.events.core.WorldEvent;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Modified from Skytils
 * Under GPL 3.0 License
 */
@Mixin(ClientChunkManager.class)
public abstract class MixinClientChunkManager extends ChunkManager {
    @Inject(method = "loadChunkFromPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;resetChunkColor(Lnet/minecraft/util/math/ChunkPos;)V"))
    private void onLoadChunkFromPacket(
            int x,
            int z,
            PacketByteBuf buf,
            Map<Heightmap.Type, long[]> heightmaps,
            Consumer<ChunkData.BlockEntityVisitor> consumer,
            CallbackInfoReturnable<WorldChunk> cir,
            @Local WorldChunk worldChunk
    ) {
        EventBus.INSTANCE.post(new WorldEvent.ChunkLoad(worldChunk));
    }
}