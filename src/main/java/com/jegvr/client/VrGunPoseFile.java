package com.jegvr.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jegvr.JegVrCompat;
import com.jegvr.client.VrGunConfig.Pose;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The format of {@code gun_poses.json} and {@code guns.json}, and how a gun's entry adds to the defaults. No game or
 * Forge state, so it can be tested; {@link VrGunConfig} owns the files and the layers.
 */
final class VrGunPoseFile {
    private static final Logger LOGGER = LogManager.getLogger(JegVrCompat.MOD_ID);
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static final int SCHEMA = 1;

    /** One file. */
    static final class Data {
        /** The format of the file; a later format brings its migration, and older files keep working. */
        int schema = SCHEMA;
        /** Null in the user layer until the user edits the defaults. */
        Pose defaults;
        Map<String, Pose> guns = new LinkedHashMap<>();
    }

    private VrGunPoseFile() {
    }

    /** Reads a file; null when it is empty. Throws on broken JSON. */
    static Data parse(Reader reader) {
        Data loaded = GSON.fromJson(reader, Data.class);
        if (loaded == null) {
            return null;
        }
        if (loaded.guns == null) {
            loaded.guns = new LinkedHashMap<>();
        }
        if (loaded.schema > SCHEMA) {
            LOGGER.warn("VR gun poses in format {} are from a newer version of the addon (this one reads {})",
                    loaded.schema, SCHEMA);
        }
        return loaded;
    }

    /** Effective pose: angles and offsets add, scales multiply, and the gun's magwell and grip replace the defaults'. */
    static Pose compose(Pose defaults, Pose gun) {
        Pose result = defaults.copy();
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
            if (gun.grip != null) {
                result.grip = gun.grip;
            }
        }
        return result;
    }

    /** Writes into {@code entry} what makes {@link #compose}{@code (defaults, entry)} give {@code effective}, rounded. */
    static void setEntry(Pose entry, Pose defaults, Pose effective) {
        entry.pitch = round(effective.pitch - defaults.pitch);
        entry.yaw = round(effective.yaw - defaults.yaw);
        entry.roll = round(effective.roll - defaults.roll);
        entry.tx = round(effective.tx - defaults.tx);
        entry.ty = round(effective.ty - defaults.ty);
        entry.tz = round(effective.tz - defaults.tz);
        entry.scale = defaults.scale == 0.0F ? 1.0F : round(effective.scale / defaults.scale);
    }

    static Pose rounded(Pose pose) {
        Pose p = new Pose();
        p.pitch = round(pose.pitch);
        p.yaw = round(pose.yaw);
        p.roll = round(pose.roll);
        p.tx = round(pose.tx);
        p.ty = round(pose.ty);
        p.tz = round(pose.tz);
        p.scale = round(pose.scale);
        p.magwell = pose.magwell;
        p.grip = pose.grip;
        return p;
    }

    /** Four decimals: tenths of a millimetre, and keeps the files free of float noise like 0.19000001. */
    static float round(float value) {
        return Math.round(value * 10000.0F) / 10000.0F;
    }
}
