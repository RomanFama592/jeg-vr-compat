package com.jegvr.client;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The throwing gesture: how fast and where the hand was going when the trigger was released. */
class VrThrowMotionTest {
    private static final double EPS = 1.0E-6;

    @Test
    void velocityFromTickPositions() {
        VrThrowMotion motion = new VrThrowMotion();
        // 0.2 m per tick forward = 4 m/s.
        for (int i = 0; i < 4; i++) {
            motion.push(new Vec3(0, 1, -0.2 * i));
        }
        Vec3 v = motion.peakVelocity();
        assertEquals(0.0, v.x, EPS);
        assertEquals(-4.0, v.z, EPS);
    }

    @Test
    void theFastestRecentTickCounts() {
        VrThrowMotion motion = new VrThrowMotion();
        motion.push(new Vec3(0, 0, 0));
        motion.push(new Vec3(0, 0, -0.3));   // the swing
        motion.push(new Vec3(0, 0, -0.32));  // the hand already stopping as the trigger opens
        assertEquals(-6.0, motion.peakVelocity().z, EPS);
    }

    @Test
    void oldMotionIsForgotten() {
        VrThrowMotion motion = new VrThrowMotion();
        motion.push(new Vec3(0, 0, 0));
        motion.push(new Vec3(0, 0, -1));     // fast, but more than three ticks ago
        for (int i = 0; i < 4; i++) {
            motion.push(new Vec3(0, 0, -1));
        }
        assertEquals(0.0, motion.peakVelocity().length(), EPS);
    }

    @Test
    void lostTrackingResets() {
        VrThrowMotion motion = new VrThrowMotion();
        motion.push(new Vec3(0, 0, 0));
        motion.push(null);
        motion.push(new Vec3(5, 5, 5));
        assertEquals(0.0, motion.peakVelocity().length(), EPS, "one sample is no motion");
    }

    @Test
    void playAreaTurnedIntoTheWorld() {
        // Vivecraft turns the play area with JOML's rotateY, the same formula as Vec3.yRot.
        Vec3 forward = new Vec3(0, 0, -1);
        Vec3 world = VrThrowMotion.toWorld(forward, (float) Math.toRadians(90));
        assertEquals(-1.0, world.x, 1.0E-6);
        assertEquals(0.0, world.z, 1.0E-6);
        assertEquals(new Vec3(0, 2, 0).y, VrThrowMotion.toWorld(new Vec3(0, 2, 0), 1.3F).y, EPS);
    }

    @Test
    void slowReleaseIsNotAThrow() {
        assertFalse(VrThrowMotion.isThrow(new Vec3(0.5, 0, 0)));
        assertTrue(VrThrowMotion.isThrow(new Vec3(0, 0.8, -0.8)));
    }

    @Test
    void handSpeedToProjectileSpeed() {
        assertEquals(1.0F, VrThrowMotion.projectileSpeed(4.0, 0.3F, 1.5F), 1.0E-6F);
        assertEquals(0.3F, VrThrowMotion.projectileSpeed(0.2, 0.3F, 1.5F), 1.0E-6F);
        assertEquals(1.5F, VrThrowMotion.projectileSpeed(20.0, 0.3F, 1.5F), 1.0E-6F);
    }
}
