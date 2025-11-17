package xyz.meowing.krypt.mixins;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.meowing.krypt.events.EventBus;
import xyz.meowing.krypt.events.core.GuiEvent;

@Mixin(AbstractContainerScreen.class)
public class MixinAbstractContainerScreen {
    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    @Inject(
            method = "keyPressed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;onClose()V",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true
    )
    //#if MC >= 1.21.9
    //$$ private void krypt$closeWindowPressed(net.minecraft.client.input.KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
    //#else
    private void krypt$closeWindowPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
    //#endif
        if (EventBus.INSTANCE.post(new GuiEvent.Close((AbstractContainerScreen) (Object) this, this.menu))) {
            cir.setReturnValue(true);
        }
    }
}
