package xyz.meowing.krypt.mixins;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.meowing.krypt.events.EventBus;
import xyz.meowing.krypt.events.core.PacketEvent;

//#if MC >= 1.21.8
//$$ import io.netty.channel.ChannelFutureListener;
//#endif

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

    @Inject(method = "sendImmediately", at = @At("HEAD"), cancellable = true)
    //#if MC >= 1.21.8
    //$$ private void krypt$onPacketSend(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean flush, CallbackInfo ci) {
    //#else
    private void krypt$onPacketSend(Packet<?> packet, PacketCallbacks callbacks, boolean flush, CallbackInfo ci) {
        if (EventBus.INSTANCE.post(new PacketEvent.Sent(packet))) ci.cancel();
    }
}