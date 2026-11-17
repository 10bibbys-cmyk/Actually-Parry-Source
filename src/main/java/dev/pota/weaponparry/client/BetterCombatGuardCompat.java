package dev.pota.weaponparry.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

/**
 * On an accepted parry, cancel Better Combat's pending attack and fade out its
 * currently playing animation. The higher-priority guard layer handles the pose.
 * This integration is optional; no Better Combat classes link at class-load time.
 */
final class BetterCombatGuardCompat {
    private BetterCombatGuardCompat() { }

    static void cancelPendingUpswing(MinecraftClient client) {
        if (!FabricLoader.getInstance().isModLoaded("bettercombat")) return;
        try {
            Class<?> api = Class.forName("net.bettercombat.api.MinecraftClient_BetterCombat");
            if (api.isInstance(client)) api.getMethod("cancelUpswing").invoke(client);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // The authoritative server-side attack block still applies.
        }
        try {
            if (client.player == null) return;
            Class<?> animatable = Class.forName("net.bettercombat.client.animation.PlayerAttackAnimatable");
            if (animatable.isInstance(client.player)) {
                animatable.getMethod("stopAttackAnimation", float.class).invoke(client.player, 1.0F);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Existing guard animation takes priority even without this soft stop.
        }
    }
}
