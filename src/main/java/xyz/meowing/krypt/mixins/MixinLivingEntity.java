package xyz.meowing.krypt.mixins;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.meowing.krypt.events.EventBus;
import xyz.meowing.krypt.events.core.EntityEvent;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {
    public MixinLivingEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setPose(Lnet/minecraft/world/entity/Pose;)V"))
    private void krypt$onDeath(DamageSource damageSource, CallbackInfo ci) {
        EventBus.INSTANCE.post(new EntityEvent.Death(this));
    }
}
