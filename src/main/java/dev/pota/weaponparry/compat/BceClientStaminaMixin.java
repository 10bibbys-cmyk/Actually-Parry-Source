package dev.pota.weaponparry.compat;

import dev.pota.weaponparry.client.ParryClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** BCE client-side attack gate reads current stamina; a synced counter may bypass that one gate. */
@Pseudo
@Mixin(targets = "com.github.theredbrain.bettercombatextension.BetterCombatExtension", remap = false)
public abstract class BceClientStaminaMixin {
    @Inject(method = "getCurrentStamina", at = @At("RETURN"), cancellable = true, remap = false)
    private static void weaponparry$clientFreeCounter(LivingEntity entity, CallbackInfoReturnable<Float> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (entity == client.player && ParryClientState.freeBceAttackAvailable() && cir.getReturnValueF() <= 0) {
            cir.setReturnValue(1.0F);
        }
    }
}
