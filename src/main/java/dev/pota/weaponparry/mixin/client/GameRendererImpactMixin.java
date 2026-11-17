package dev.pota.weaponparry.mixin.client;

import dev.pota.weaponparry.client.ParryImpactFeedback;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Client-only, tiny successful-parry FOV pulse. */
@Mixin(GameRenderer.class)
public abstract class GameRendererImpactMixin {
    @Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)D",
            at = @At("RETURN"), cancellable = true)
    private void weaponparry$impactFov(Camera camera, float tickDelta, boolean changingFov,
                                      CallbackInfoReturnable<Double> cir) {
        double offset = ParryImpactFeedback.fovOffset(tickDelta);
        if (offset != 0.0) cir.setReturnValue(cir.getReturnValueD() + offset);
    }
}
