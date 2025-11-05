package xyz.meowing.krypt.mixins;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.meowing.krypt.events.EventBus;
import xyz.meowing.krypt.events.core.PacketEvent;

@Mixin(ClientConnection.class)
public class MixinClientConnection {
    @Inject(method = "channelRead0*", at = @At("HEAD"), cancellable = true)
    private void krypt$onReceivePacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (EventBus.INSTANCE.onPacketReceived(packet)) ci.cancel();
    }

    @Inject(method = "channelRead0*", at = @At("TAIL"))
    private void krypt$onReceivePacketPost(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        EventBus.INSTANCE.post(new PacketEvent.ReceivedPost(packet));
    }
}