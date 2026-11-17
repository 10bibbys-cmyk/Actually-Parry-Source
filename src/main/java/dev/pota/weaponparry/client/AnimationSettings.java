package dev.pota.weaponparry.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client-only first-person animation profiles. Gameplay timing is server-owned;
 * these values affect Punchy's visual pose and playback duration exclusively.
 */
public final class AnimationSettings {
    private static final Logger LOG = LoggerFactory.getLogger("weaponparry");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "actually-parry-animations.json";
    public static AnimationSettings INSTANCE = defaults();

    public Profile oneHanded;
    public Profile twoHanded;
    /** Zero disables the optional first-person projection pulse. */
    public float impactPulseDegrees = 1.0F;

    public static final class Pose {
        public float[] rotation = new float[3]; // degrees X/Y/Z
        public float[] position = new float[3]; // Punchy coordinate units X/Y/Z

        public Pose() { }

        Pose(float rx, float ry, float rz, float px, float py, float pz) {
            rotation = new float[]{rx, ry, rz};
            position = new float[]{px, py, pz};
        }

        void validate() {
            if (rotation == null || rotation.length != 3 || position == null || position.length != 3)
                throw new IllegalArgumentException("Each pose requires three rotations and three positions");
            for (float value : rotation) {
                if (!Float.isFinite(value) || value < -180F || value > 180F)
                    throw new IllegalArgumentException("Rotation must be within -180..180 degrees");
            }
            for (float value : position) {
                if (!Float.isFinite(value) || value < -12F || value > 12F)
                    throw new IllegalArgumentException("Position must be within -12..12");
            }
        }
    }

    public static final class Profile {
        public int windupTicks = 3;
        public int holdTicks = 10;
        public int recoveryTicks = 6;
        public Pose mainArm = new Pose();
        public Pose offArm = new Pose();
        public Pose item = new Pose();
        public Pose grip = new Pose();

        public float totalSeconds() { return totalTicks() / 20.0F; }
        public int totalTicks() { return windupTicks + holdTicks + recoveryTicks; }

        void validate() {
            if (windupTicks < 1 || windupTicks > 40 || holdTicks < 1 || holdTicks > 60
                    || recoveryTicks < 1 || recoveryTicks > 40)
                throw new IllegalArgumentException("Timing must be 1..40, 1..60 and 1..40 ticks");
            if (mainArm == null || offArm == null || item == null || grip == null)
                throw new IllegalArgumentException("Missing bone transform");
            mainArm.validate();
            offArm.validate();
            item.validate();
            grip.validate();
        }
    }

    private void validate() {
        oneHanded.validate();
        twoHanded.validate();
        if (!Float.isFinite(impactPulseDegrees) || impactPulseDegrees < 0F || impactPulseDegrees > 3F)
            throw new IllegalArgumentException("Impact pulse must be between 0 and 3 degrees");
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE);
    }

    public Profile profile(boolean twoHanded) {
        return twoHanded ? this.twoHanded : this.oneHanded;
    }

    public static AnimationSettings defaults() {
        // Finalized production Punchy profiles. Visual timing is independent
        // of the server's 10-tick parry window.
        AnimationSettings settings = new AnimationSettings();
        settings.oneHanded = new Profile();
        settings.oneHanded.mainArm = new Pose(-48F, -70F, 48F, -3.8F, 1.8F, -0.4F);
        settings.oneHanded.item = new Pose(-180F, 180F, -180F, -11.4F, -0.3F, -0.5F);
        settings.oneHanded.grip = new Pose(0F, -90F, -6F, 0.5F, 0F, 0F);

        settings.twoHanded = new Profile();
        settings.twoHanded.mainArm = new Pose(-28F, -96F, 50F, -3.2F, 1.7F, -0.4F);
        settings.twoHanded.offArm = new Pose(13F, -8F, 20F, -3.3F, 3.8F, -2F);
        settings.twoHanded.item = new Pose(0F, 0F, -58F, -1F, -0.35F, -0.65F);
        settings.twoHanded.grip = new Pose(0F, -82F, -6F, 0F, 0F, 0F);
        return settings;
    }

    public static void load() {
        try {
            if (!Files.exists(file())) {
                INSTANCE = defaults();
                save();
                return;
            }
            AnimationSettings read = GSON.fromJson(Files.readString(file(), StandardCharsets.UTF_8), AnimationSettings.class);
            if (read == null || read.oneHanded == null || read.twoHanded == null)
                throw new IllegalArgumentException("Missing animation profiles");
            read.validate();
            INSTANCE = read;
        } catch (IOException | RuntimeException e) {
            LOG.warn("Invalid {}: using default animation profiles. Existing file was not overwritten.", FILE, e);
            INSTANCE = defaults();
        }
    }

    public static boolean save() {
        try {
            INSTANCE.validate();
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(INSTANCE) + "\n", StandardCharsets.UTF_8);
            return true;
        } catch (IOException | RuntimeException e) {
            LOG.error("Could not save {}.", FILE, e);
            return false;
        }
    }
}
