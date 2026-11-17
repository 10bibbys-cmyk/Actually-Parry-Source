package dev.pota.weaponparry.mixin;

import dev.pota.weaponparry.ParryCombat;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingDamageMixin {
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float weaponparry$penalty(float amount, DamageSource source, float suppliedAmount) {
        return (Object)this instanceof ServerPlayerEntity player
                ? ParryCombat.penalizeIncoming(player, source, amount) : amount;
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void weaponparry$penaltyConsume(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof ServerPlayerEntity player) {
            ParryCombat.incomingApplied(player, source, cir.getReturnValueZ());
        }
    }
}
