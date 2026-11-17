package dev.pota.weaponparry;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;

public final class WeaponParry implements ModInitializer {
    public static final Identifier PARRY_REQUEST = new Identifier("weaponparry", "request");
    public static final Identifier PARRY_POSE = new Identifier("weaponparry", "pose");
    public static final Identifier COUNTER_SYNC = new Identifier("weaponparry", "counter_sync");
    public static final Identifier PARRY_IMPACT = new Identifier("weaponparry", "impact");

    @Override
    public void onInitialize() {
        ParryConfig.load();
        ParryCombat.initialize();
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ParryCombat.shutdown());
        ServerPlayNetworking.registerGlobalReceiver(PARRY_REQUEST, (server, player, handler, buf, sender) ->
                server.execute(() -> ParryCombat.activate(player)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ParryCombat.disconnect(handler.getPlayer()));
    }
}
