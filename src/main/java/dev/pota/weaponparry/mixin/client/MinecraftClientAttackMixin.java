package dev.pota.weaponparry.mixin.client;

import dev.pota.weaponparry.client.ParryClientState;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Input-side guard: prevents vanilla and Better Combat upswing initiation. */
@Mixin(value = MinecraftClient.class, priority = 1500)
public abstract class MinecraftClientAttackMixin {
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void weaponparry$stopAttack(CallbackInfoReturnable<Boolean> cir) {
        if (ParryClientState.guardBlocksAttack()) {
            cir.setReturnValue(false);
        } else {
            // A counterattack after the 10-tick guard releases the lingering
            // first-person pose so Punchy can immediately animate the swing.
            ParryClientState.endParryVisual();
        }
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void weaponparry$stopHeldAttack(boolean breaking, CallbackInfo ci) {
        if (ParryClientState.guardBlocksAttack()) {
            ci.cancel();
        } else if (MinecraftClient.getInstance().options.attackKey.isPressed()) {
            ParryClientState.endParryVisual();
        }
    }
}
