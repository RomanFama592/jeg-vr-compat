package com.jegvr.client;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The angles sent in JEG's shoot message must point where the controller points, as Minecraft reads them. */
class VrAimTest {
    private static final float EPS = 1.0E-3F;

    @Test
    void cardinalDirections() {
        assertAngles(0.0F, 0.0F, VrAim.rotation(new Vec3(0, 0, 1), 0, 0));      // south
        assertAngles(-90.0F, 0.0F, VrAim.rotation(new Vec3(1, 0, 0), 0, 0));    // east
        assertAngles(90.0F, 0.0F, VrAim.rotation(new Vec3(-1, 0, 0), 0, 0));    // west
        assertAngles(0.0F, -90.0F, VrAim.rotation(new Vec3(0, 1, 0), 0, 0));    // up
        assertAngles(0.0F, 90.0F, VrAim.rotation(new Vec3(0, -1, 0), 0, 0));    // down
        assertEquals(180.0F, Math.abs(VrAim.rotation(new Vec3(0, 0, -1), 0, 0)[0]), EPS); // north
    }

    /** Minecraft turns the angles back into a direction with Vec3.directionFromRotation; it must be the barrel's. */
    @Test
    void roundTripsThroughMinecraft() {
        double[][] dirs = {{0.3, 0.2, 0.9}, {-0.7, -0.4, 0.1}, {0.0, 0.99, -0.1}, {0.5, -0.5, -0.5}, {-1, 0.001, 0}};
        for (double[] d : dirs) {
            Vec3 dir = new Vec3(d[0], d[1], d[2]).normalize();
            float[] rot = VrAim.rotation(dir, 0, 0);
            Vec3 back = Vec3.directionFromRotation(rot[1], rot[0]);
            // Mth.sin/cos use a lookup table: about 1e-4 of error.
            assertEquals(dir.x, back.x, 2.0E-3, "x of " + dir);
            assertEquals(dir.y, back.y, 2.0E-3, "y of " + dir);
            assertEquals(dir.z, back.z, 2.0E-3, "z of " + dir);
        }
    }

    @Test
    void recoilDriftsAndClimbs() {
        float[] rot = VrAim.rotation(new Vec3(0, 0, 1), 2.5F, 4.0F);
        assertAngles(2.5F, -4.0F, rot);
    }

    @Test
    void climbNeverPassesStraightUp() {
        float[] rot = VrAim.rotation(new Vec3(0, 1, 0.01).normalize(), 0, 30.0F);
        assertEquals(-90.0F, rot[1], EPS);
    }

    @Test
    void notQuiteUnitDirectionsStillWork() {
        // A direction slightly longer than 1 (float noise) must not produce NaN from asin.
        float[] rot = VrAim.rotation(new Vec3(0, 1.0000001, 0), 0, 0);
        assertEquals(-90.0F, rot[1], EPS);
    }

    private static void assertAngles(float yaw, float pitch, float[] rot) {
        assertEquals(yaw, rot[0], EPS, "yaw");
        assertEquals(pitch, rot[1], EPS, "pitch");
    }
}
