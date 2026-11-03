package com.jegvr.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jegvr.JegVrCompat;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-gun VR pose offsets stored in {@code config/jeg_vr_compat/guns.json}. The "default" entry applies to every
 * gun; a per-gun entry (registry id) is added on top. The file is re-read when it changes on disk.
 */
public final class VrGunConfig {
    private static final Logger LOGGER = LogManager.getLogger(JegVrCompat.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long RELOAD_CHECK_MS = 1000L;

    /** Rotation in degrees, translation in blocks (model space, applied before the gun is drawn). */
    public static final class Pose {
        public float pitch;
        public float yaw;
        public float roll;
        public float tx;
        public float ty;
        public float tz;
        public float scale = 1.0F;
        /** Where the free hand drops a magazine (used by grab-to-reload); optional. */
        public Magwell magwell;

        public Pose copy() {
            Pose p = new Pose();
            p.set(this);
            p.magwell = magwell;
            return p;
        }

        public void set(Pose o) {
            pitch = o.pitch;
            yaw = o.yaw;
            roll = o.roll;
            tx = o.tx;
            ty = o.ty;
            tz = o.tz;
            scale = o.scale;
        }
    }

    public static final class Magwell {
        public float x;
        public float y;
        public float z;
    }

    private static final class Data {
        Pose defaults = factoryDefaults();
        Map<String, Pose> guns = new LinkedHashMap<>();
    }

    /** No rotation or offset: the gun uses the bare controller frame (see VivecraftItemRenderingVRMixin). */
    public static Pose factoryDefaults() {
        Pose pose = new Pose();
        pose.scale = 0.6F;
        return pose;
    }

    /** Built-in per-item entries, used while guns.json has no entry of its own for that item. */
    private static final Map<String, Pose> BUILT_IN = new LinkedHashMap<>();

    static {
        // F9.11: the trumpet's baked model is small next to the guns (6 px long); JEG draws it 1.5x in flat too.
        // Its valves sit higher than a gun's grip, so it goes up a few centimetres into the hand.
        Pose trumpet = new Pose();
        trumpet.scale = 1.5F;
        trumpet.ty = 0.04F;
        BUILT_IN.put("jeg:trumpet", trumpet);
    }

    private static Data data = new Data();
    private static long lastModified = -1L;
    private static long lastCheck;

    private VrGunConfig() {
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(JegVrCompat.MOD_ID).resolve("guns.json");
    }

    /** Effective pose for a gun: global default plus the per-gun entry. */
    public static Pose poseFor(String gunId) {
        reloadIfChanged();
        Pose base = data.defaults;
        Pose gun = data.guns.getOrDefault(gunId, BUILT_IN.get(gunId));
        Pose result = base.copy();
        if (gun != null) {
            result.pitch += gun.pitch;
            result.yaw += gun.yaw;
            result.roll += gun.roll;
            result.tx += gun.tx;
            result.ty += gun.ty;
            result.tz += gun.tz;
            result.scale *= gun.scale;
            if (gun.magwell != null) {
                result.magwell = gun.magwell;
            }
        }
        return result;
    }

    /** Stores {@code effective} as the final pose of a gun (per-gun entry = effective minus global default). */
    public static void setEffective(String gunId, Pose effective) {
        reloadIfChanged();
        if (gunId == null) {
            data.defaults.set(effective);
            return;
        }
        Pose base = data.defaults;
        Pose entry = data.guns.computeIfAbsent(gunId, VrGunConfig::newEntry);
        entry.pitch = effective.pitch - base.pitch;
        entry.yaw = effective.yaw - base.yaw;
        entry.roll = effective.roll - base.roll;
        entry.tx = effective.tx - base.tx;
        entry.ty = effective.ty - base.ty;
        entry.tz = effective.tz - base.tz;
        entry.scale = base.scale == 0.0F ? 1.0F : effective.scale / base.scale;
    }

    /** Back to the factory pose: the global default, or a gun's built-in entry (none for most guns). */
    public static void reset(String gunId) {
        reloadIfChanged();
        if (gunId == null) {
            data.defaults = factoryDefaults();
        } else {
            data.guns.remove(gunId);
        }
    }

    /** The raw entry being edited: the per-gun one, or the global default when {@code gunId} is null. */
    public static Pose editable(String gunId) {
        reloadIfChanged();
        return gunId == null ? data.defaults : data.guns.computeIfAbsent(gunId, VrGunConfig::newEntry);
    }

    /** A new per-gun entry, starting from the built-in one when there is one. */
    private static Pose newEntry(String gunId) {
        Pose builtIn = BUILT_IN.get(gunId);
        return builtIn == null ? new Pose() : builtIn.copy();
    }

    public static void save() {
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(data, writer);
            }
            lastModified = Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            LOGGER.error("Could not save {}", path, e);
        }
    }

    private static void reloadIfChanged() {
        long now = System.currentTimeMillis();
        if (now - lastCheck < RELOAD_CHECK_MS) {
            return;
        }
        lastCheck = now;
        Path path = file();
        try {
            if (!Files.exists(path)) {
                if (lastModified == -1L) {
                    save();
                }
                return;
            }
            long modified = Files.getLastModifiedTime(path).toMillis();
            if (modified == lastModified) {
                return;
            }
            try (Reader reader = Files.newBufferedReader(path)) {
                Data loaded = GSON.fromJson(reader, Data.class);
                if (loaded != null) {
                    if (loaded.defaults == null) {
                        loaded.defaults = factoryDefaults();
                    }
                    if (loaded.guns == null) {
                        loaded.guns = new LinkedHashMap<>();
                    }
                    data = loaded;
                }
            }
            lastModified = modified;
            LOGGER.info("Loaded VR gun poses from {}", path);
        } catch (Exception e) {
            lastModified = -2L;
            LOGGER.error("Could not read {}, keeping previous values", path, e);
        }
    }
}
