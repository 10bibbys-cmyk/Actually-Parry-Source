package dev.pota.weaponparry;

import dev.pota.weaponparry.core.ParryTimeline;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.network.PacketByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

/** Entire combat decision path runs on the logical server. */
public final class ParryCombat {
    public static final TagKey<Item> TWO_HANDED = TagKey.of(RegistryKeys.ITEM, new Identifier("weaponparry", "two_handed"));
    private static final TagKey<DamageType> MELEE = TagKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier("weaponparry", "melee"));
    private static final TagKey<EntityType<?>> PHYSICAL_PROJECTILE = TagKey.of(RegistryKeys.ENTITY_TYPE, new Identifier("weaponparry", "physical_projectiles"));
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final class Session {
        final ParryTimeline timeline = new ParryTimeline();
        volatile Item weapon;  // must remain the same item throughout the guard and counterattack
        volatile boolean twoHanded;
        long lastFeedbackTick = -1;
    }

    private ParryCombat() { }

    public static void initialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            Session state = SESSIONS.get(player.getUuid());
            if (state == null) return true;
            long tick = tick(player);
            if (!state.timeline.guardActive(tick) || !validWeapon(player, state)) return true;
            if (!parryable(source) || !inFront(player, source)) return true;
            if (!state.timeline.parryHit(tick, ParryConfig.INSTANCE.counterDurationTicks)) return true;
            feedback(player, state, source);
            syncCounter(player, state);
            return false;
        });
    }

    public static void activate(ServerPlayerEntity player) {
        ParryConfig config = ParryConfig.INSTANCE;
        ItemStack main = player.getMainHandStack();
        if (!player.isAlive() || player.isSpectator() || player.isUsingItem() || !config.allows(main)) return;
        Session state = SESSIONS.computeIfAbsent(player.getUuid(), key -> new Session());
        long tick = tick(player);
        if (!state.timeline.activate(tick, config.parryWindowTicks, config.cooldownTicks)) return;
        state.weapon = main.getItem();
        state.twoHanded = main.isIn(TWO_HANDED);
        PacketByteBuf packet = PacketByteBufs.create();
        packet.writeVarInt(player.getId());
        packet.writeBoolean(state.twoHanded);
        packet.writeVarInt(config.parryWindowTicks);
        for (ServerPlayerEntity watcher : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(watcher, WeaponParry.PARRY_POSE, copyPose(player.getId(), state.twoHanded, config.parryWindowTicks));
        }
        ServerPlayNetworking.send(player, WeaponParry.PARRY_POSE, packet);
    }

    private static PacketByteBuf copyPose(int id, boolean twoHanded, int windowTicks) {
        PacketByteBuf b = PacketByteBufs.create();
        b.writeVarInt(id);
        b.writeBoolean(twoHanded);
        b.writeVarInt(windowTicks);
        return b;
    }

    public static void disconnect(ServerPlayerEntity player) { SESSIONS.remove(player.getUuid()); }

    public static void shutdown() { SESSIONS.clear(); }

    private static long tick(ServerPlayerEntity player) { return player.getServer().getTicks(); }

    private static boolean validWeapon(ServerPlayerEntity player, Session state) {
        return state.weapon != null && player.getMainHandStack().getItem() == state.weapon
                && ParryConfig.INSTANCE.allows(player.getMainHandStack()) && !player.isUsingItem();
    }

    public static boolean parryable(DamageSource source) {
        if (source.isIn(DamageTypeTags.IS_EXPLOSION)
                || source.isOf(DamageTypes.MAGIC)
                || source.isOf(DamageTypes.INDIRECT_MAGIC)
                || source.isOf(DamageTypes.SONIC_BOOM)
                || source.isOf(DamageTypes.DRAGON_BREATH)
                || source.isOf(DamageTypes.WITHER_SKULL)
                || source.isOf(DamageTypes.FIREBALL)
                || source.isOf(DamageTypes.UNATTRIBUTED_FIREBALL)) return false;
        // Physical melee, including modded attacks explicitly added to the melee damage type tag.
        if (source.isIn(MELEE)) {
            return source.getSource() instanceof LivingEntity;
        }
        // Physical projectiles only. Merely having a projectile damage type is not enough;
        // magical bolts and fireballs must not become parryable.
        if (!source.isIn(DamageTypeTags.IS_PROJECTILE)) return false;
        Entity direct = source.getSource();
        return direct instanceof ProjectileEntity
                && direct.getType().isIn(PHYSICAL_PROJECTILE);
    }

    public static boolean inFront(ServerPlayerEntity player, DamageSource source) {
        Entity direct = source.getSource();
        if (direct == null) return false;
        Vec3d towardThreat;
        if (direct instanceof ProjectileEntity projectile && projectile.getVelocity().horizontalLengthSquared() > 1.0e-6) {
            // Against projectiles, face the direction they are coming from, not the shooter.
            towardThreat = projectile.getVelocity().multiply(-1);
        } else {
            towardThreat = direct.getPos().subtract(player.getPos());
        }
        Vec3d look = player.getRotationVec(1.0F);
        double dot = look.x * towardThreat.x + look.z * towardThreat.z;
        double lengthSquared = (look.x * look.x + look.z * look.z) * (towardThreat.x * towardThreat.x + towardThreat.z * towardThreat.z);
        return lengthSquared > 1.0e-6 && dot / Math.sqrt(lengthSquared) >= ParryConfig.INSTANCE.frontalCosine();
    }

    /** Called before damage is applied; this does not consume the penalty until damage succeeds. */
    public static float penalizeIncoming(ServerPlayerEntity player, DamageSource source, float amount) {
        Session state = SESSIONS.get(player.getUuid());
        if (state == null || amount <= 0 || !ParryConfig.INSTANCE.enableFailedParryPenalty || !parryable(source)) return amount;
        long tick = tick(player);
        if (state.timeline.guardActive(tick) && validWeapon(player, state) && !inFront(player, source)) {
            state.timeline.missedGuard(tick);
        }
        return state.timeline.penaltyReady(tick) ? amount + ParryConfig.INSTANCE.failedParryBonusDamage : amount;
    }

    public static void incomingApplied(ServerPlayerEntity player, DamageSource source, boolean dealtDamage) {
        Session state = SESSIONS.get(player.getUuid());
        if (state != null && dealtDamage && ParryConfig.INSTANCE.enableFailedParryPenalty && parryable(source)) {
            state.timeline.consumePenalty();
        }
    }

    /** Enforced on the logical server, including Better Combat attack packets. */
    public static boolean blocksAttack(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity p)) return false;
        Session state = SESSIONS.get(p.getUuid());
        return state != null && validWeapon(p, state) && state.timeline.guardActive(tick(p));
    }
    public static void attackStarted(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        Session state = SESSIONS.get(serverPlayer.getUuid());
        if (state != null) state.timeline.cancelGuard();
    }

    /** Called by the player attack damage wrapper; does not touch spell or ranged damage. */
    public static float counterBonus(PlayerEntity player, DamageSource source, Entity target, float base) {
        if (!(player instanceof ServerPlayerEntity serverPlayer) || !(target instanceof LivingEntity)
                || base <= 0 || !source.isOf(DamageTypes.PLAYER_ATTACK)) return 0;
        Session state = SESSIONS.get(serverPlayer.getUuid());
        if (state == null || !validWeapon(serverPlayer, state) || !state.timeline.counterReady(tick(serverPlayer))) return 0;
        return ParryConfig.INSTANCE.enableCounter ? ParryConfig.INSTANCE.counterBonusDamage : 0;
    }

    public static void attackApplied(PlayerEntity player, boolean dealtDamage) {
        if (!dealtDamage || !(player instanceof ServerPlayerEntity serverPlayer)) return;
        Session state = SESSIONS.get(serverPlayer.getUuid());
        if (state != null) {
            state.timeline.cancelGuard();
            state.timeline.consumeCounter();
            syncCounter(serverPlayer, state);
        }
    }

    public static boolean freeBceSwingReady(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return false;
        if (!ParryConfig.INSTANCE.freeBceCounterattackStamina) return false;
        Session state = SESSIONS.get(serverPlayer.getUuid());
        return state != null && validWeapon(serverPlayer, state) && state.timeline.freeSwingReady(tick(serverPlayer));
    }

    public static boolean consumeFreeBceSwing(ServerPlayerEntity player) {
        if (!freeBceSwingReady(player)) return false;
        Session state = SESSIONS.get(player.getUuid());
        boolean consumed = state.timeline.consumeFreeSwing(tick(player));
        if (consumed) syncCounter(player, state);
        return consumed;
    }

    private static void syncCounter(ServerPlayerEntity player, Session state) {
        PacketByteBuf b = PacketByteBufs.create();
        b.writeVarInt(state.timeline.remainingCounter(tick(player)));
        b.writeBoolean(ParryConfig.INSTANCE.freeBceCounterattackStamina && state.timeline.freeSwingReady(tick(player)) && validWeapon(player, state));
        b.writeIdentifier(Registries.ITEM.getId(state.weapon));
        ServerPlayNetworking.send(player, WeaponParry.COUNTER_SYNC, b);
    }

    private static void feedback(ServerPlayerEntity player, Session state, DamageSource source) {
        // A player can parry multiple hits per guard. Coalesce the impact
        // presentation to one burst per server tick.
        long now = tick(player);
        if (state.lastFeedbackTick == now) return;
        state.lastFeedbackTick = now;

        ParryConfig config = ParryConfig.INSTANCE;
        ServerWorld world = player.getServerWorld();

        Entity direct = source.getSource();
        Vec3d towardContact = direct == null
                ? player.getRotationVec(1.0F)
                : direct.getPos().subtract(player.getPos());
        if (direct instanceof ProjectileEntity projectile
                && projectile.getVelocity().horizontalLengthSquared() > 1.0e-6) {
            towardContact = projectile.getVelocity().multiply(-1.0);
        }
        Vec3d front = new Vec3d(towardContact.x, 0.0, towardContact.z);
        if (front.lengthSquared() < 1.0e-6) {
            Vec3d look = player.getRotationVec(1.0F);
            front = new Vec3d(look.x, 0.0, look.z);
        }
        front = front.normalize();
        Vec3d across = new Vec3d(-front.z, 0.0, front.x);

        // Small, bright crossing spark sprays, not an opaque AoE cloud.
        double x = player.getX() + front.x * 0.70;
        double y = player.getY() + player.getHeight() * 0.69;
        double z = player.getZ() + front.z * 0.70;
        if (config.sparkCount > 0) {
            // Stronger than 1.1.1 even for pre-existing config files.
            // Bound all emitted particles regardless of the configured count.
            int sparks = Math.min(48, config.sparkCount * 2);
            int left = sparks / 2;
            int right = sparks - left;
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    x + across.x * 0.09, y + 0.035, z + across.z * 0.09,
                    left, 0.14, 0.12, 0.14, 0.20);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    x - across.x * 0.09, y - 0.035, z - across.z * 0.09,
                    right, 0.14, 0.12, 0.14, 0.20);
            world.spawnParticles(ParticleTypes.CRIT, x, y, z,
                    Math.max(2, Math.min(5, sparks / 7)), 0.10, 0.09, 0.10, 0.11);
        }

        // Restore the original sound which worked well in-game.
        // Slightly softer than 1.1.2; existing per-hand config values remain usable.
        float volume = (state.twoHanded ? config.twoHandedVolume : config.oneHandedVolume) * 0.85F;
        float pitch = state.twoHanded ? config.twoHandedPitch : config.oneHandedPitch;
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ANVIL_PLACE,
                SoundCategory.PLAYERS, volume, pitch);

        // Visual-only defender feedback. No player movement or camera rotation.
        ServerPlayNetworking.send(player, WeaponParry.PARRY_IMPACT, PacketByteBufs.create());

    }
}
