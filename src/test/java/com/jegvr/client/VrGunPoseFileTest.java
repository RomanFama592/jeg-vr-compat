package com.jegvr.client;

import com.google.gson.JsonSyntaxException;
import com.jegvr.client.VrGunConfig.Pose;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The format of gun_poses.json / guns.json and how a gun's entry combines with the defaults. */
class VrGunPoseFileTest {
    private static final float EPS = 1.0E-4F;

    @Test
    void entryAddsAnglesAndOffsetsAndMultipliesScale() {
        Pose defaults = pose(1, 2, 3, 0.1F, 0.2F, 0.3F, 0.6F);
        Pose gun = pose(10, 20, 30, 0.01F, 0.02F, 0.03F, 1.5F);
        Pose result = VrGunPoseFile.compose(defaults, gun);
        assertPose(pose(11, 22, 33, 0.11F, 0.22F, 0.33F, 0.9F), result);
    }

    @Test
    void withoutEntryTheDefaultsAreACopy() {
        Pose defaults = pose(1, 2, 3, 0.1F, 0.2F, 0.3F, 0.6F);
        Pose result = VrGunPoseFile.compose(defaults, null);
        assertPose(defaults, result);
        assertNotSame(defaults, result);
        result.pitch = 99;
        assertEquals(1, defaults.pitch, EPS);
    }

    @Test
    void gunMagwellReplacesTheDefaultOne() {
        Pose defaults = new Pose();
        defaults.magwell = new VrGunConfig.Magwell();
        Pose gun = new Pose();
        assertSame(defaults.magwell, VrGunPoseFile.compose(defaults, gun).magwell);
        gun.magwell = new VrGunConfig.Magwell();
        assertSame(gun.magwell, VrGunPoseFile.compose(defaults, gun).magwell);
    }

    @Test
    void gunGripReplacesTheDefaultOneAndSurvivesRounding() {
        Pose defaults = new Pose();
        Pose gun = new Pose();
        assertSame(null, VrGunPoseFile.compose(defaults, gun).grip);
        gun.grip = new VrGunConfig.Grip();
        assertSame(gun.grip, VrGunPoseFile.compose(defaults, gun).grip);
        assertSame(gun.grip, VrGunPoseFile.rounded(gun).grip);
        assertSame(gun.grip, gun.copy().grip);
    }

    /** Saving a calibrated pose stores the difference from the defaults; reading it back gives the same pose. */
    @Test
    void savedEntryGivesBackTheCalibratedPose() {
        Pose defaults = pose(0, 0, 0, 0, -0.08F, -0.06F, 0.6F);
        Pose effective = pose(-12.5F, 3, 90, 0.015F, 0.11F, -0.2F, 0.9F);
        Pose entry = new Pose();
        VrGunPoseFile.setEntry(entry, defaults, effective);
        assertPose(effective, VrGunPoseFile.compose(defaults, entry));
        assertEquals(1.5F, entry.scale, EPS);
    }

    @Test
    void zeroDefaultScaleLeavesTheGunScaleAtOne() {
        Pose entry = new Pose();
        VrGunPoseFile.setEntry(entry, pose(0, 0, 0, 0, 0, 0, 0), pose(0, 0, 0, 0, 0, 0, 2));
        assertEquals(1.0F, entry.scale, EPS);
    }

    @Test
    void roundsToFourDecimals() {
        assertEquals(0.19F, VrGunPoseFile.round(0.19000001F));
        assertEquals(0.1235F, VrGunPoseFile.round(0.12345678F));
        assertEquals(-0.0001F, VrGunPoseFile.round(-0.00012F));
    }

    @Test
    void readsTheBuiltInFile() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("assets/jeg_vr_compat/gun_poses.json")) {
            assertNotNull(in, "gun_poses.json on the classpath");
            VrGunPoseFile.Data data = VrGunPoseFile.parse(new InputStreamReader(in, StandardCharsets.UTF_8));
            assertNotNull(data);
            assertEquals(VrGunPoseFile.SCHEMA, data.schema);
            assertNotNull(data.defaults);
            assertTrue(data.defaults.scale > 0);
            assertTrue(data.guns.keySet().stream().allMatch(id -> id.contains(":")), "item ids: " + data.guns.keySet());
            data.guns.values().forEach(gun -> assertTrue(gun.scale > 0));
        }
    }

    @Test
    void oldFileWithoutSchemaOrGuns() {
        VrGunPoseFile.Data data = VrGunPoseFile.parse(new StringReader("{\"defaults\": {\"scale\": 0.5}}"));
        assertEquals(1, data.schema);
        assertNotNull(data.guns);
        assertTrue(data.guns.isEmpty());
        assertEquals(0.5F, data.defaults.scale, EPS);
    }

    @Test
    void gunWithoutScaleKeepsScaleOne() {
        VrGunPoseFile.Data data = VrGunPoseFile.parse(new StringReader("{\"guns\": {\"jeg:pistol\": {\"yaw\": 5}}}"));
        assertEquals(1.0F, data.guns.get("jeg:pistol").scale, EPS);
        assertNull(data.defaults);
    }

    @Test
    void newerSchemaIsStillRead() {
        VrGunPoseFile.Data data = VrGunPoseFile.parse(new StringReader("{\"schema\": 9, \"guns\": {}}"));
        assertEquals(9, data.schema);
    }

    @Test
    void emptyFileIsNothingAndBrokenFileThrows() {
        assertNull(VrGunPoseFile.parse(new StringReader("")));
        assertThrows(JsonSyntaxException.class, () -> VrGunPoseFile.parse(new StringReader("{\"guns\": {")));
    }

    private static Pose pose(float pitch, float yaw, float roll, float tx, float ty, float tz, float scale) {
        Pose p = new Pose();
        p.pitch = pitch;
        p.yaw = yaw;
        p.roll = roll;
        p.tx = tx;
        p.ty = ty;
        p.tz = tz;
        p.scale = scale;
        return p;
    }

    private static void assertPose(Pose expected, Pose actual) {
        assertEquals(expected.pitch, actual.pitch, EPS, "pitch");
        assertEquals(expected.yaw, actual.yaw, EPS, "yaw");
        assertEquals(expected.roll, actual.roll, EPS, "roll");
        assertEquals(expected.tx, actual.tx, EPS, "tx");
        assertEquals(expected.ty, actual.ty, EPS, "ty");
        assertEquals(expected.tz, actual.tz, EPS, "tz");
        assertEquals(expected.scale, actual.scale, EPS, "scale");
    }
}
