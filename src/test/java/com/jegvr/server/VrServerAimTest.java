package com.jegvr.server;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the server believes of the muzzle position a VR client sends. */
class VrServerAimTest {
    @Test
    void recordLastsOneTick() {
        assertTrue(VrServerAim.fresh(100, 100));
        assertTrue(VrServerAim.fresh(100, 101));
        assertFalse(VrServerAim.fresh(100, 102));
    }

    @Test
    void muzzleMustBeNearTheEye() {
        Vec3 eye = new Vec3(10, 65, 10);
        assertTrue(VrServerAim.inReach(eye, eye.add(0.5, -0.3, 0.4)));
        assertTrue(VrServerAim.inReach(eye, eye.add(2.5, 0, 0)));
        assertFalse(VrServerAim.inReach(eye, eye.add(2.6, 0, 0)));
        assertFalse(VrServerAim.inReach(eye, eye.add(0, 0, 40)));
    }

    @Test
    void muzzleBehindAWallStaysOnTheEyeSide() {
        Vec3 eye = new Vec3(0, 0, 0);
        Vec3 muzzle = new Vec3(0, 0, 1);
        Vec3 wall = new Vec3(0, 0, 0.6);
        Vec3 origin = VrServerAim.beforeWall(eye, muzzle, wall);
        assertEquals(0.55, origin.z, 1.0E-9);
        assertEquals(0.0, origin.x, 1.0E-9);
        assertEquals(0.0, origin.y, 1.0E-9);
    }

    @Test
    void rejectsNonFiniteVectors() {
        assertTrue(VrServerAim.isFinite(new Vec3(1, 2, 3)));
        assertFalse(VrServerAim.isFinite(new Vec3(Double.NaN, 0, 0)));
        assertFalse(VrServerAim.isFinite(new Vec3(0, Double.POSITIVE_INFINITY, 0)));
    }
}
