package dev.pota.weaponparry.client;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Constructs an in-memory Punchy 3.2a AnimationClip, using the same
 * AnimationClip/BoneTrack/Keyframe types as Punchy's resource loader.
 * Reflection keeps Punchy optional on dedicated servers and clients.
 */
final class PunchyClipBuilder {
    private static Class<?> clipClass;
    private static Class<?> trackClass;
    private static Class<?> frameClass;
    private static Constructor<?> clipCtor;
    private static Constructor<?> trackCtor;
    private static Constructor<?> frameCtor;
    private static Method addTrack;
    private static Method addFrame;
    private static Method addTimeline;
    // Two immutable visual profiles for the lifetime of the client. Constructing
    // a clip creates up to 32 keyframes and their JOML transforms; reuse avoids
    // that allocation and reflection work on every accepted parry.
    private static Object oneHandedClip;
    private static Object twoHandedClip;

    private PunchyClipBuilder() { }

    private static void init() throws ReflectiveOperationException {
        if (clipCtor != null) return;
        clipClass = Class.forName("punchy.client.animation.data.AnimationClip");
        trackClass = Class.forName("punchy.client.animation.data.BoneTrack");
        frameClass = Class.forName("punchy.client.animation.data.Keyframe");
        clipCtor = clipClass.getConstructor(String.class, float.class, boolean.class);
        trackCtor = trackClass.getConstructor(String.class);
        frameCtor = frameClass.getConstructor(float.class, Vector3f.class, Vector3f.class,
                Quaternionf.class, Vector3f.class, String.class);
        addTrack = clipClass.getMethod("addTrack", trackClass);
        addFrame = trackClass.getMethod("addKeyframe", frameClass);
        addTimeline = clipClass.getMethod("addTimelineEffect", float.class, String.class);
    }

    static Object create(boolean twoHanded, AnimationSettings.Profile profile)
            throws ReflectiveOperationException {
        // AnimationSettings is loaded once during client initialization. There
        // is no live editor, so the authored profile does not change mid-session.
        Object cached = twoHanded ? twoHandedClip : oneHandedClip;
        if (cached != null) return cached;
        init();
        float length = profile.totalSeconds();
        Object clip = clipCtor.newInstance(twoHanded ? "actually_parry_two" : "actually_parry_one",
                length, false);

        addBone(clip, "right_arm", profile.mainArm, profile);
        if (twoHanded) addBone(clip, "left_arm", profile.offArm, profile);
        addBone(clip, "item_right", profile.item, profile);
        addBone(clip, "itemgrip_right", profile.grip, profile);

        // Preserve native Punchy 3.2a playback settings and dual-hand ownership.
        addTimeline.invoke(clip, 0.0F, "anim_speed_1");
        addTimeline.invoke(clip, 0.0F, "disable_tuning");
        if (twoHanded) addTimeline.invoke(clip, 0.0F, "dual_handed");
        // Cache only fully constructed clips. Failed reflection retries cleanly.
        if (twoHanded) twoHandedClip = clip;
        else oneHandedClip = clip;
        return clip;
    }

    private static void addBone(Object clip, String boneName, AnimationSettings.Pose pose,
                                AnimationSettings.Profile profile) throws ReflectiveOperationException {
        Object track = trackCtor.newInstance(boneName);
        float up = profile.windupTicks / 20.0F;
        float holdEnd = (profile.windupTicks + profile.holdTicks) / 20.0F;
        float down = profile.recoveryTicks / 20.0F;
        // Ease in/out using two intermediate samples in each transition.
        float[] times = new float[]{
                0F, up * 0.30F, up * 0.70F, up, holdEnd,
                holdEnd + down * 0.30F, holdEnd + down * 0.70F, holdEnd + down
        };
        float[] weights = {0F, 0.10F, 0.90F, 1F, 1F, 0.90F, 0.10F, 0F};
        for (int i = 0; i < times.length; i++) {
            float w = weights[i];
            Vector3f pos = new Vector3f(pose.position[0] * w,
                    pose.position[1] * w, pose.position[2] * w);
            Vector3f deg = new Vector3f(pose.rotation[0] * w,
                    pose.rotation[1] * w, pose.rotation[2] * w);
            // Match PunchyAnimationLoader's Euler-to-quaternion ZYX order.
            Quaternionf q = new Quaternionf().rotationZYX(
                    (float) Math.toRadians(deg.z),
                    (float) Math.toRadians(deg.y),
                    (float) Math.toRadians(deg.x));
            Object frame = frameCtor.newInstance(times[i], pos, deg, q,
                    new Vector3f(1F, 1F, 1F), "linear");
            addFrame.invoke(track, frame);
        }
        addTrack.invoke(clip, track);
    }
}
