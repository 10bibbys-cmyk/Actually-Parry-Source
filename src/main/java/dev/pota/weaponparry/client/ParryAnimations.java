package dev.pota.weaponparry.client;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Arm;

/** Existing 1H/2H guard poses, layered above Better Combat swings while active. */
final class ParryAnimations {
    private static final Identifier LAYER = new Identifier("weaponparry", "guard_layer");
    private static final Identifier ONE_HAND = new Identifier("weaponparry", "parry_one");
    private static final Identifier TWO_HAND = new Identifier("weaponparry", "parry_two");
    private static final Identifier ONE_HAND_LEFT = new Identifier("weaponparry", "parry_one_left");
    private static final Identifier TWO_HAND_LEFT = new Identifier("weaponparry", "parry_two_left");

    private ParryAnimations() { }

    static void register() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 2500,
                player -> new ModifierLayer<IAnimation>());
    }

    @SuppressWarnings("unchecked")
    static void play(MinecraftClient client, int id, boolean twoHanded) {
        if (client.world == null || !(client.world.getEntityById(id) instanceof AbstractClientPlayerEntity player)) return;
        boolean left = player.getMainArm() == Arm.LEFT;
        var animation = PlayerAnimationRegistry.getAnimation(twoHanded
                ? (left ? TWO_HAND_LEFT : TWO_HAND) : (left ? ONE_HAND_LEFT : ONE_HAND));
        var layer = PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER);
        if (animation != null && layer instanceof ModifierLayer<?> modifier) {
            ((ModifierLayer<IAnimation>)modifier).setAnimation(new KeyframeAnimationPlayer(animation));
        }
    }
}
