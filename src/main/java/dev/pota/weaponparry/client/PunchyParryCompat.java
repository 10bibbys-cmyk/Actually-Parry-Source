package dev.pota.weaponparry.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Method;

/**
 * Punchy 3.2a first-person parry ownership.
 *
 * Punchy owns the actual first-person arm/item renderer. During a server-accepted
 * parry we play its native custom clip and reconcile interruptions only if needed.
 * Other animations take over normally once the parry visual ends or a counter
 * starts. All calls are optional and resolved reflectively to avoid a hard dep.
 */
final class PunchyParryCompat {
    private static final Logger LOG = LoggerFactory.getLogger("weaponparry");

    private static boolean apiChecked;
    private static boolean apiAvailable;
    private static boolean missingClipWarned;
    private static float ownedClipSeconds;
    private static boolean initLogged;
    private static boolean interruptionLogged;
    private static Object handler;
    private static Object ownedClip;
    private static Method resolveCustomClip, setSourceHand, isModEnabled, isBlacklisted,
            speedSetting, setActiveArm, setIgnoreDualHanded, setTimelineScale,
            setBlendIn, resetLoop, playAtTime, getCurrentClip, getCurrentTime,
            isPlaying, stopWithBlendOut;

    private PunchyParryCompat() { }

    private static boolean setup() {
        if (apiChecked) return apiAvailable;
        apiChecked = true;
        if (!FabricLoader.getInstance().isModLoaded("punchy")) return false;
        try {
            Class<?> manager = Class.forName("punchy.client.animation.PunchyAnimationManager");
            Class<?> config = Class.forName("punchy.config.PunchyConfig");
            Class<?> clip = Class.forName("punchy.client.animation.data.AnimationClip");
            Class<?> pose = Class.forName("punchy.client.animation.PoseHandler");
            handler = manager.getField("POSE_HANDLER").get(null);
            resolveCustomClip = manager.getMethod("resolveCustomClip", String.class);
            setSourceHand = manager.getMethod("setSourceHand", MinecraftClient.class, Hand.class);
            isBlacklisted = manager.getMethod("isAnyHandBlacklisted");
            isModEnabled = config.getMethod("isModEnabled");
            speedSetting = config.getMethod("getAnimationSpeed");
            setActiveArm = pose.getMethod("setActiveArm", net.minecraft.util.Arm.class);
            setIgnoreDualHanded = pose.getMethod("setIgnoreDualHanded", boolean.class);
            setTimelineScale = pose.getMethod("setTimelineSpeedScale", float.class);
            setBlendIn = pose.getMethod("setBlendInDurationNext", float.class);
            resetLoop = pose.getMethod("resetLoopSpeedParams");
            playAtTime = pose.getMethod("playAtTime", clip, float.class,
                    boolean.class, boolean.class, float.class);
            getCurrentClip = pose.getMethod("getCurrentClip");
            getCurrentTime = pose.getMethod("getCurrentTime");
            isPlaying = pose.getMethod("isPlaying");
            stopWithBlendOut = pose.getMethod("stopWithBlendOut");
            apiAvailable = true;
        } catch (ReflectiveOperationException | LinkageError ex) {
            LOG.warn("Punchy 3.2a API unavailable; only third-person parry animations will play.", ex);
        }
        return apiAvailable;
    }

    static void play(MinecraftClient client, int entityId, boolean twoHanded) {
        release();
        if (!setup() || client.world == null || client.player == null
                || client.player.getId() != entityId
                || client.getCameraEntity() != client.player
                || !client.options.getPerspective().isFirstPerson()) return;
        try {
            if (!Boolean.TRUE.equals(isModEnabled.invoke(null)) ||
                    Boolean.TRUE.equals(isBlacklisted.invoke(null))) return;
            AnimationSettings.Profile profile = AnimationSettings.INSTANCE.profile(twoHanded);
            ownedClipSeconds = profile.totalSeconds();
            ownedClip = PunchyClipBuilder.create(twoHanded, profile);
            beginAt(client, 0.0F);
            if (!initLogged) {
                initLogged = true;
                LOG.info("Actually Parry loaded the first-person Punchy parry profiles.");
            }
        } catch (ReflectiveOperationException | IllegalArgumentException | LinkageError ex) {
            LOG.warn("Unable to start Punchy parry clip. Combat remains functional.", ex);
            ownedClip = null;
        }
    }

    /** Runs only on a client with a pending first-person parry pose. */
    static void tick(MinecraftClient client) {
        if (ownedClip == null) return;
        if (!ParryClientState.parryVisualActive(client) || !setup()) {
            release();
            return;
        }
        try {
            if (!Boolean.TRUE.equals(isModEnabled.invoke(null))) {
                release();
                return;
            }
            float elapsed = Math.min(ownedClipSeconds - 0.02F,
                    ParryClientState.parryVisualTicks(client, 0) / 20.0F);
            Object current = getCurrentClip.invoke(handler);
            boolean playing = Boolean.TRUE.equals(isPlaying.invoke(handler));
            float currentSeconds = ((Number) getCurrentTime.invoke(handler)).floatValue();

            if (current != ownedClip || !playing ||
                    !Float.isFinite(currentSeconds) ||
                    Math.abs(currentSeconds - elapsed) > 0.14F) {
                // A Punchy movement, equip, or swing clip took over, or playback
                // drifted due to configuration speed. Rejoin at the ORIGINAL
                // elapsed point: never restart the 0.95s sequence at frame zero.
                if (!interruptionLogged) {
                    interruptionLogged = true;
                    LOG.info("Reclaiming Punchy first-person pose during active parry.");
                }
                beginAt(client, elapsed);
            }
        } catch (ReflectiveOperationException | IllegalArgumentException | LinkageError ex) {
            LOG.warn("Punchy parry playback failed; restoring Punchy's normal animation control.", ex);
            release();
        }
    }

    private static void beginAt(MinecraftClient client, float positionSeconds)
            throws ReflectiveOperationException {
        setSourceHand.invoke(null, client, Hand.MAIN_HAND);
        setActiveArm.invoke(handler, client.player.getMainArm());
        setIgnoreDualHanded.invoke(handler, false);
        resetLoop.invoke(handler);
        setBlendIn.invoke(handler, 0.05F);
        playAtTime.invoke(handler, ownedClip, 1.0F, false, false, positionSeconds);
        // Verified Punchy 3.2a applies getAnimationSpeed()/5.5 to anim_speed_*
        // timeline effects. Compensate only this handler while the guard runs.
        float global = ((Number) speedSetting.invoke(null)).floatValue();
        if (Float.isFinite(global) && global > 0.05F) {
            setTimelineScale.invoke(handler, 5.5F / global);
        }
    }

    static void clear() {
        release();
        interruptionLogged = false;
    }

    private static void release() {
        if (ownedClip == null) return;
        if (apiAvailable) {
            try {
                if (getCurrentClip.invoke(handler) == ownedClip) {
                    setTimelineScale.invoke(handler, 1.0F);
                    stopWithBlendOut.invoke(handler);
                }
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Optional client-only renderer must never break combat.
            }
        }
        ownedClip = null;
        ownedClipSeconds = 0.0F;
    }
}
