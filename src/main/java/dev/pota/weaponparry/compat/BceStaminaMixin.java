package dev.pota.weaponparry.compat;

import dev.pota.weaponparry.ParryCombat;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** BCE funnels attack stamina through these helpers on Fabric 1.20.1.
 * A single free swing is consumed when BCE would actually deduct stamina. */
@Pseudo
@Mixin(targets = "com.github.theredbrain.bettercombatextension.BetterCombatExtension", remap = false)
public abstract class BceStaminaMixin {
    @Inject(method = "getCurrentStamina", at = @At("RETURN"), cancellable = true, remap = false)
    private static void weaponparry$serverGate(LivingEntity entity, CallbackInfoReturnable<Float> cir) {
        if (entity instanceof ServerPlayerEntity player && ParryCombat.freeBceSwingReady(player) && cir.getReturnValueF() <= 0) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "addStamina", at = @At("HEAD"), cancellable = true, remap = false)
    private static void weaponparry$freeStamina(LivingEntity entity, float amount, CallbackInfo ci) {
        if (amount < 0 && entity instanceof ServerPlayerEntity player && ParryCombat.consumeFreeBceSwing(player)) {
            ci.cancel();
        }
    }
}
