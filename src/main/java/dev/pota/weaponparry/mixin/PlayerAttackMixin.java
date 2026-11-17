package dev.pota.weaponparry.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.pota.weaponparry.ParryCombat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bonus is part of the original hit; no extra damage call, invulnerability bypass or double proc. */
@Mixin(PlayerEntity.class)
public abstract class PlayerAttackMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void weaponparry$attackBegins(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity)(Object)this;
        if (ParryCombat.blocksAttack(player)) {
            ci.cancel();
            return;
        }
        ParryCombat.attackStarted(player);
    }

    @WrapOperation(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private boolean weaponparry$counter(Entity target, DamageSource source, float damage, Operation<Boolean> original) {
        PlayerEntity player = (PlayerEntity)(Object)this;
        float bonus = ParryCombat.counterBonus(player, source, target, damage);
        boolean dealt = original.call(target, source, damage + bonus);
        if (bonus > 0 && dealt) ParryCombat.attackApplied(player, true);
        return dealt;
    }
}
