package dev.pota.weaponparry.client;

import dev.pota.weaponparry.WeaponParry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class WeaponParryClient implements ClientModInitializer {
    private static KeyBinding key;

    @Override
    public void onInitializeClient() {
        AnimationSettings.load();
        ParryAnimations.register();
        // Only reconciles a Punchy clip while the accepted parry visual is active.
        ClientTickEvents.END_CLIENT_TICK.register(PunchyParryCompat::tick);
        key = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.weaponparry.parry",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.weaponparry"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (key.wasPressed()) {
                if (client.player != null && client.currentScreen == null && client.getNetworkHandler() != null) {
                    ClientPlayNetworking.send(WeaponParry.PARRY_REQUEST, PacketByteBufs.create());
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(WeaponParry.PARRY_POSE, (client, handler, buf, responseSender) -> {
            int entityId = buf.readVarInt();
            boolean twoHanded = buf.readBoolean();
            int windowTicks = buf.readVarInt();
            client.execute(() -> {
                ParryAnimations.play(client, entityId, twoHanded);
                if (client.player != null && client.player.getId() == entityId) {
                    ParryClientState.startGuard(client, windowTicks);
                    ParryClientState.startParryVisual(client, twoHanded);
                    BetterCombatGuardCompat.cancelPendingUpswing(client);
                    PunchyParryCompat.play(client, entityId, twoHanded);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(WeaponParry.COUNTER_SYNC, (client, handler, buf, responseSender) -> {
            int remaining = buf.readVarInt();
            boolean freeSwing = buf.readBoolean();
            Identifier weapon = buf.readIdentifier();
            client.execute(() -> ParryClientState.sync(client, remaining, freeSwing, weapon));
        });

        ClientPlayNetworking.registerGlobalReceiver(WeaponParry.PARRY_IMPACT, (client, handler, buf, sender) ->
                client.execute(() -> ParryImpactFeedback.trigger(client)));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            PunchyParryCompat.clear();
            ParryImpactFeedback.clear();
            ParryClientState.clear();
        });
    }
}
