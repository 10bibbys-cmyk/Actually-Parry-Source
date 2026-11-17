package dev.pota.weaponparry.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** Small local hint for BCE's client-side stamina gate; server validates independently. */
public final class ParryClientState {
    private static long expires;
    private static boolean freeSwing;
    private static Identifier weapon;
    private static long guardUntil;
    private static Identifier guardWeapon;
    // Client-only render control; remains active slightly after the real guard ends.
    private static long parryVisualStart;
    private static long parryVisualEnd;
    private static boolean parryVisualTwoHanded;
    private static Identifier parryVisualWeapon;

    private ParryClientState() { }

    public static void sync(MinecraftClient client, int ticks, boolean available, Identifier item) {
        expires = client.world == null ? 0 : client.world.getTime() + ticks;
        freeSwing = available;
        weapon = item;
    }

    public static void startGuard(MinecraftClient client, int ticks) {
        if (client.world == null || client.player == null) return;
        guardUntil = client.world.getTime() + Math.max(1, ticks);
        guardWeapon = Registries.ITEM.getId(client.player.getMainHandStack().getItem());
    }
    public static void startParryVisual(MinecraftClient client, boolean twoHanded) {
        if (client.world == null || client.player == null) return;
        parryVisualStart = client.world.getTime();
        // Visual timing is locally editable; server gameplay guard stays unchanged.
        parryVisualEnd = parryVisualStart + AnimationSettings.INSTANCE.profile(twoHanded).totalTicks();
        parryVisualTwoHanded = twoHanded;
        parryVisualWeapon = Registries.ITEM.getId(client.player.getMainHandStack().getItem());
    }

    public static boolean parryVisualActive(MinecraftClient client) {
        if (client.player == null || client.world == null || client.player.isSpectator()
                || !client.player.isAlive() || client.getCameraEntity() != client.player
                || !client.options.getPerspective().isFirstPerson()
                || client.currentScreen != null || parryVisualWeapon == null) return false;
        if (client.world.getTime() >= parryVisualEnd) return false;
        if (!parryVisualWeapon.equals(Registries.ITEM.getId(client.player.getMainHandStack().getItem()))) {
            endParryVisual();
            return false;
        }
        return true;
    }

    public static float parryVisualTicks(MinecraftClient client, float tickDelta) {
        return client.world == null ? 0.0F : Math.max(0.0F, client.world.getTime() - parryVisualStart + tickDelta);
    }

    public static boolean parryVisualTwoHanded() { return parryVisualTwoHanded; }

    public static void endParryVisual() {
        parryVisualEnd = 0;
        parryVisualWeapon = null;
    }

    public static boolean guardBlocksAttack() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world != null && client.player != null
            && client.world.getTime() < guardUntil && guardWeapon != null
            && guardWeapon.equals(Registries.ITEM.getId(client.player.getMainHandStack().getItem()));
    }
    public static boolean freeBceAttackAvailable() {
        MinecraftClient client = MinecraftClient.getInstance();
        return freeSwing && client.world != null && client.player != null
                && client.world.getTime() < expires && weapon != null
                && weapon.equals(Registries.ITEM.getId(client.player.getMainHandStack().getItem()));
    }

    public static void clear() {
        expires = 0; freeSwing = false; weapon = null;
        guardUntil = 0; guardWeapon = null;
        endParryVisual();
    }
}
