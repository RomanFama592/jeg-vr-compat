package com.jegvr.client;

import com.jegvr.JegVrCompat;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Per-gun VR pose offsets, in two layers:
 * <ul>
 * <li>Built-in: {@code assets/jeg_vr_compat/gun_poses.json}, shipped in the jar. No pose lives in code.</li>
 * <li>User: {@code config/jeg_vr_compat/guns.json}, on top of the built-in one (a user entry replaces the built-in
 * entry of that gun; a user "defaults" replaces the built-in defaults).</li>
 * </ul>
 * The calibration commands edit the user layer. In the development environment (the project's sources next to the
 * game directory) they edit the built-in file in {@code src/main/resources} instead, so calibrations ship with the
 * next build, and the user layer is ignored. Each layer holds a "defaults" pose for every gun and per-gun entries
 * (registry id) added on top. Files are re-read when they change on disk.
 */
public final class VrGunConfig {
    private static final Logger LOGGER = LogManager.getLogger(JegVrCompat.MOD_ID);
    private static final long RELOAD_CHECK_MS = 1000L;
    private static final String RESOURCE = "assets/" + JegVrCompat.MOD_ID + "/gun_poses.json";

    /** Rotation in degrees, translation in blocks (controller frame, applied before the gun is drawn). */
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
        /**
         * F10.4: where the free hand's controller holds the gun (two-handed grip), relative to the gun's controller;
         * optional (without it, the gun model's support arm decides). Taken with {@code /jegvr grip capture}.
         */
        public Grip grip;

        public Pose copy() {
            Pose p = new Pose();
            p.set(this);
            p.magwell = magwell;
            p.grip = grip;
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

    /** Position in room metres and rotation (quaternion), in the gun controller's frame, for a right-hand gun. */
    public static final class Grip {
        public float x;
        public float y;
        public float z;
        public float qx;
        public float qy;
        public float qz;
        public float qw = 1.0F;
    }

    public static final class Magwell {
        public float x;
        public float y;
        public float z;
    }

    /** One file of poses and when it was last read. */
    private static final class Layer {
        VrGunPoseFile.Data data = new VrGunPoseFile.Data();
        long lastModified = -1L;
    }

    /** Dev only: the built-in file in the project's sources; null in a built jar. */
    private static final Path SOURCE = sourceFile();
    private static final Layer BUILT_IN = new Layer();
    private static final Layer USER = new Layer();
    private static long lastCheck;

    private VrGunConfig() {
    }

    private static Path sourceFile() {
        if (FMLLoader.isProduction()) {
            return null;
        }
        Path file = FMLPaths.GAMEDIR.get().toAbsolutePath().getParent().resolve("src/main/resources").resolve(RESOURCE);
        return Files.exists(file) ? file : null;
    }

    private static Path userFile() {
        return FMLPaths.CONFIGDIR.get().resolve(JegVrCompat.MOD_ID).resolve("guns.json");
    }

    /** Reads the files now instead of on the first gun drawn. */
    public static void load() {
        lastCheck = 0L;
        reloadIfChanged();
        LOGGER.info("VR gun poses: built-in {}, calibration commands edit {}", SOURCE != null ? SOURCE : RESOURCE,
                SOURCE != null ? "the built-in file (development)" : userFile());
    }

    /** True when the commands edit the built-in file (development environment). */
    public static boolean editsBuiltIn() {
        return SOURCE != null;
    }

    /** The disk is only looked at here, between frames: {@link #poseFor} runs in the middle of drawing each eye. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            reloadIfChanged();
        }
    }

    /** Effective pose for a gun: defaults plus the gun's entry. */
    public static Pose poseFor(String gunId) {
        return VrGunPoseFile.compose(defaults(), entry(gunId));
    }

    /** Stores {@code effective} as the final pose of a gun (its entry = effective minus defaults). */
    public static void setEffective(String gunId, Pose effective) {
        reloadIfChanged();
        VrGunPoseFile.Data target = editable().data;
        if (gunId == null) {
            target.defaults = VrGunPoseFile.rounded(effective);
            return;
        }
        Pose entry = target.guns.computeIfAbsent(gunId, VrGunConfig::newEntry);
        VrGunPoseFile.setEntry(entry, defaults(), effective);
    }

    /** F10.4: stores (or, with null, removes) the two-handed grip of a gun in the layer the commands edit. */
    public static void setGrip(String gunId, Grip grip) {
        reloadIfChanged();
        VrGunPoseFile.Data target = editable().data;
        if (grip == null) {
            Pose entry = target.guns.get(gunId);
            if (entry != null) {
                entry.grip = null;
            }
            return;
        }
        target.guns.computeIfAbsent(gunId, VrGunConfig::newEntry).grip = grip;
    }

    /**
     * Drops the edits of a gun (or of the defaults when {@code gunId} is null). For the user layer that is back to the
     * built-in pose; for the built-in file (dev) a gun loses its entry and the defaults go back to the saved file.
     */
    public static void reset(String gunId) {
        reloadIfChanged();
        if (gunId != null) {
            editable().data.guns.remove(gunId);
        } else if (editsBuiltIn()) {
            BUILT_IN.lastModified = -1L;
            lastCheck = 0L;
            reloadIfChanged();
        } else {
            USER.data.defaults = null;
        }
    }

    /** Writes the layer the commands edit; returns its path, or null on failure. */
    public static Path save() {
        Path path = editsBuiltIn() ? SOURCE : userFile();
        Layer layer = editable();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                VrGunPoseFile.GSON.toJson(layer.data, writer);
                writer.write(System.lineSeparator());
            }
            layer.lastModified = Files.getLastModifiedTime(path).toMillis();
            return path;
        } catch (IOException e) {
            LOGGER.error("Could not save {}", path, e);
            return null;
        }
    }

    private static Layer editable() {
        return editsBuiltIn() ? BUILT_IN : USER;
    }

    private static Pose defaults() {
        if (!editsBuiltIn() && USER.data.defaults != null) {
            return USER.data.defaults;
        }
        return BUILT_IN.data.defaults != null ? BUILT_IN.data.defaults : new Pose();
    }

    private static Pose entry(String gunId) {
        Pose user = editsBuiltIn() ? null : USER.data.guns.get(gunId);
        return user != null ? user : BUILT_IN.data.guns.get(gunId);
    }

    /** A new user entry starts from the built-in one, so calibrating a gun starts where it is. */
    private static Pose newEntry(String gunId) {
        Pose builtIn = editsBuiltIn() ? null : BUILT_IN.data.guns.get(gunId);
        return builtIn == null ? new Pose() : builtIn.copy();
    }

    private static void reloadIfChanged() {
        long now = System.currentTimeMillis();
        if (now - lastCheck < RELOAD_CHECK_MS) {
            return;
        }
        lastCheck = now;
        if (SOURCE != null) {
            reloadFile(BUILT_IN, SOURCE);
            return;
        }
        if (BUILT_IN.lastModified == -1L) {
            loadResource();
        }
        Path user = userFile();
        if (!Files.exists(user)) {
            if (USER.lastModified == -1L) {
                save();
            }
            return;
        }
        reloadFile(USER, user);
    }

    /** The built-in file inside the jar; read once. */
    private static void loadResource() {
        BUILT_IN.lastModified = 0L;
        try (InputStream in = VrGunConfig.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                LOGGER.error("Built-in VR gun poses {} not found in the jar", RESOURCE);
                return;
            }
            VrGunPoseFile.Data loaded = VrGunPoseFile.parse(new InputStreamReader(in, StandardCharsets.UTF_8));
            if (loaded != null) {
                BUILT_IN.data = loaded;
            }
        } catch (Exception e) {
            LOGGER.error("Could not read built-in VR gun poses {}", RESOURCE, e);
        }
    }

    private static void reloadFile(Layer layer, Path path) {
        try {
            long modified = Files.getLastModifiedTime(path).toMillis();
            if (modified == layer.lastModified) {
                return;
            }
            // Remembered first, so a broken file is reported once and tried again only when it changes.
            layer.lastModified = modified;
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                VrGunPoseFile.Data loaded = VrGunPoseFile.parse(reader);
                if (loaded != null) {
                    layer.data = loaded;
                }
            }
            LOGGER.info("Loaded VR gun poses from {}", path);
        } catch (Exception e) {
            LOGGER.error("Could not read {}, keeping previous values", path, e);
        }
    }
}
