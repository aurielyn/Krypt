package xyz.meowing.krypt.mixins;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.meowing.krypt.utils.EntityAccessor;

/**
 * Modified from SkyOcean's implementation
 * <p>
 * Original File: [GitHub](https://github.com/meowdding/SkyOcean/blob/main/src/common/main/java/me/owdding/skyocean/mixins/EntityMixin.java)
 * @author Meowdding
 */
@Mixin(Entity.class)
public class MixinEntity implements EntityAccessor {
    @Unique
    private boolean krypt$glowing = false;
    @Unique
    private int krypt$glowingColor = 0;
    @Unique
    private long krypt$glowTime = -1;
    @Unique
    private boolean krypt$glowingThisFrame = false;

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    public void getTeamColor(CallbackInfoReturnable<Integer> cir) {
        if (hasCustomGlow()) {
            cir.setReturnValue(krypt$glowingColor);
            this.krypt$glowingThisFrame = false;
        }
    }

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    public void isGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (hasCustomGlow()) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public void krypt$setGlowing(boolean glowing) {
        this.krypt$glowing = glowing;
    }

    @Override
    public void krypt$setGlowingColor(int color) {
        this.krypt$glowingColor = color;
    }

    @Override
    public void krypt$glowTime(long time) {
        this.krypt$glowTime = System.currentTimeMillis() + time;
        this.krypt$glowing = false;
    }

    @Override
    public void krypt$setGlowingThisFrame(boolean glowing) {
        this.krypt$glowingThisFrame = glowing;
    }

    @Unique
    private boolean hasCustomGlow() {
        if (this.krypt$glowingThisFrame) return true;
        if (this.krypt$glowTime > System.currentTimeMillis()) return true;
        this.krypt$glowTime = -1;
        return this.krypt$glowing;
    }
}