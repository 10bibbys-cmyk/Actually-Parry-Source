package dev.pota.weaponparry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** One server-owned config. Clients may have different local files; the server always decides. */
public final class ParryConfig {
    private static final Logger LOG = LoggerFactory.getLogger("weaponparry");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public List<String> weaponWhitelist = new ArrayList<>();
    public int parryWindowTicks = 10;
    public int cooldownTicks = 40;
    public int counterDurationTicks = 20;
    public float counterBonusDamage = 1.0F;
    public boolean enableCounter = true;
    public float failedParryBonusDamage = 1.0F;
    public boolean enableFailedParryPenalty = true;
    public float frontalAngleDegrees = 120.0F;
    public int sparkCount = 8;
    public float oneHandedVolume = 0.45F;
    public float twoHandedVolume = 0.50F;
    public float oneHandedPitch = 1.05F;
    public float twoHandedPitch = 0.75F;
    public boolean freeBceCounterattackStamina = true;

    // Resolved once on configuration load. Registry tags themselves remain
    // dynamic, so datapack tag reloads still update weapon eligibility.
    private transient List<Identifier> allowedItemIds = List.of();
    private transient List<TagKey<Item>> allowedItemTags = List.of();
    private transient double frontalCosine = Math.cos(Math.toRadians(120.0D * 0.5D));

    public double frontalCosine() { return frontalCosine; }

    public static ParryConfig INSTANCE = new ParryConfig();

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("weaponparry.json");
        try {
            if (Files.exists(path)) {
                ParryConfig value = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), ParryConfig.class);
                if (value == null) throw new JsonParseException("Null config");
                value.validate();
                INSTANCE = value;
            } else {
                INSTANCE = new ParryConfig();
                Files.createDirectories(path.getParent());
                Files.writeString(path, GSON.toJson(INSTANCE), StandardCharsets.UTF_8);
            }
        } catch (IOException | RuntimeException exception) {
            LOG.error("Invalid weaponparry.json; refusing to enable any parries until fixed", exception);
            INSTANCE = new ParryConfig();
        }
    }

    private void validate() {
        if (weaponWhitelist == null) throw new IllegalArgumentException("weaponWhitelist missing");
        if (parryWindowTicks < 1 || parryWindowTicks > 200) throw new IllegalArgumentException("parryWindowTicks 1..200");
        if (cooldownTicks < 0 || cooldownTicks > 12000) throw new IllegalArgumentException("cooldownTicks 0..12000");
        if (counterDurationTicks < 1 || counterDurationTicks > 12000) throw new IllegalArgumentException("counterDurationTicks 1..12000");
        if (counterBonusDamage < 0 || counterBonusDamage > 100 || !Float.isFinite(counterBonusDamage)) throw new IllegalArgumentException("invalid counter bonus");
        if (failedParryBonusDamage < 0 || failedParryBonusDamage > 100 || !Float.isFinite(failedParryBonusDamage)) throw new IllegalArgumentException("invalid penalty bonus");
        if (frontalAngleDegrees <= 0 || frontalAngleDegrees > 180 || !Float.isFinite(frontalAngleDegrees)) throw new IllegalArgumentException("frontalAngleDegrees 0..180");
        if (sparkCount < 0 || sparkCount > 64) throw new IllegalArgumentException("sparkCount 0..64");
        if (oneHandedVolume < 0 || oneHandedVolume > 2 || twoHandedVolume < 0 || twoHandedVolume > 2) throw new IllegalArgumentException("volume 0..2");
        if (oneHandedPitch < 0.5F || oneHandedPitch > 2 || twoHandedPitch < 0.5F || twoHandedPitch > 2) throw new IllegalArgumentException("pitch 0.5..2");
        List<Identifier> ids = new ArrayList<>();
        List<TagKey<Item>> tags = new ArrayList<>();
        for (String entry : weaponWhitelist) {
            if (entry == null || entry.isBlank())
                throw new IllegalArgumentException("Invalid weapon whitelist selector: " + entry);
            boolean isTag = entry.startsWith("#");
            Identifier id = Identifier.tryParse(isTag ? entry.substring(1) : entry);
            if (id == null)
                throw new IllegalArgumentException("Invalid weapon whitelist selector: " + entry);
            if (isTag) tags.add(TagKey.of(RegistryKeys.ITEM, id));
            else ids.add(id);
        }
        allowedItemIds = List.copyOf(ids);
        allowedItemTags = List.copyOf(tags);
        frontalCosine = Math.cos(Math.toRadians(frontalAngleDegrees * 0.5D));
    }

    public boolean allows(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem
                || stack.getItem() instanceof ShieldItem || stack.getItem() instanceof PickaxeItem
                || stack.getItem() instanceof ShovelItem || stack.getItem() instanceof HoeItem
                || stack.getItem() instanceof ShearsItem || stack.getItem() instanceof FishingRodItem
                || stack.getItem() instanceof FlintAndSteelItem) return false;
        // Vanilla axes are utility tools. Explicitly whitelisted modded battleaxes remain eligible.
        Identifier actualId = Registries.ITEM.getId(stack.getItem());
        if (actualId.getNamespace().equals("minecraft") && actualId.getPath().endsWith("_axe")) return false;
        for (TagKey<Item> tag : allowedItemTags) {
            if (stack.isIn(tag)) return true;
        }
        for (Identifier id : allowedItemIds) {
            if (actualId.equals(id)) return true;
        }
        return false;
    }
}
