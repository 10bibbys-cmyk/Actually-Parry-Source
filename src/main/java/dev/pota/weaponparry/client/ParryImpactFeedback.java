package dev.pota.weaponparry.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

/**
 * Visual-only, defender-side successful-parry feedback. A tiny 4-tick FOV
 * compression gives an impact sensation without moving the player's camera,
 * modifying input, or running a per-tick handler.
 */
public final class ParryImpactFeedback {
    private static ClientWorld triggeredWorld;
    private static long startTick = Long.MIN_VALUE;

    private ParryImpactFeedback() { }

    public static void trigger(MinecraftClient client) {
        if (client.world == null || client.player == null || client.player.isSpectator()) return;
        if (AnimationSettings.INSTANCE.impactPulseDegrees <= 0.0F) return;
        triggeredWorld = client.world;
        startTick = client.world.getTime();
    }

    /** Called from the existing first-person projection calculation. */
    public static double fovOffset(float tickDelta) {
        // Called from the renderer at frame rate. Almost all frames are idle.
        if (startTick == Long.MIN_VALUE) return 0.0;
        MinecraftClient client = MinecraftClient.getInstance();
        // Never retain an old ClientWorld across dimension/world changes.
        if (client.world == null || client.world != triggeredWorld) {
            clear();
            return 0.0;
        }

        double elapsed = client.world.getTime() - startTick + tickDelta;
        if (elapsed >= 4.0) {
            clear();
            return 0.0;
        }
        if (elapsed < 0.0 || client.player == null
                || client.getCameraEntity() != client.player
                || !client.options.getPerspective().isFirstPerson()) return 0.0;

        float strength = AnimationSettings.INSTANCE.impactPulseDegrees;
        return strength <= 0.0F ? 0.0 : -strength * Math.sin(Math.PI * elapsed / 4.0);
    }

    public static void clear() {
        triggeredWorld = null;
        startTick = Long.MIN_VALUE;
    }
}
