package com.jegvr.server;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** F9.8: where a bullet meets its shooter's head or hand, and how much it hurts. */
class VrSelfDamageTest {
    private static final double EPS = 1.0E-9;
    private static final Vec3 O = Vec3.ZERO;

    @Test
    void bodyShotKeepsJegDamage() {
        assertEquals(7.5F, VrSelfDamage.damage(7.5F, false));
        assertEquals(0.0F, VrSelfDamage.damage(0.0F, false));
    }

    @Test
    void headshotIsLethal() {
        assertEquals(VrSelfDamage.LETHAL_DAMAGE, VrSelfDamage.damage(7.5F, true));
        assertTrue(VrSelfDamage.damage(7.5F, true) >= 1000.0F);
        // A gun that already does more keeps its damage.
        assertEquals(5000.0F, VrSelfDamage.damage(5000.0F, true));
    }

    @Test
    void headCentreIsBehindTheEyes() {
        Vec3 centre = VrSelfDamage.headCentre(new Vec3(0, 1.6, 0), new Vec3(0, 0, 2), 1.0);
        assertEquals(new Vec3(0, 1.6, -VrSelfDamage.HEAD_BACK), centre);
        // Scaled with the world scale; no direction keeps the eyes.
        assertEquals(-VrSelfDamage.HEAD_BACK * 2, VrSelfDamage.headCentre(O, new Vec3(0, 0, 1), 2.0).z, EPS);
        assertEquals(O, VrSelfDamage.headCentre(O, O, 1.0));
    }

    @Test
    void segmentEntersTheSphere() {
        // From 1 m away towards the centre, radius 0.1: enters 0.9 m along a 2 m segment.
        assertEquals(0.45, VrSelfDamage.entry(new Vec3(0, 0, -1), new Vec3(0, 0, 1), O, 0.1, false), EPS);
        // Passing beside it, or stopping short of it.
        assertEquals(-1.0, VrSelfDamage.entry(new Vec3(0.2, 0, -1), new Vec3(0.2, 0, 1), O, 0.1, false));
        assertEquals(-1.0, VrSelfDamage.entry(new Vec3(0, 0, -1), new Vec3(0, 0, -0.5), O, 0.1, false));
        // Moving away from it, from outside.
        assertEquals(-1.0, VrSelfDamage.entry(new Vec3(0, 0, -1), new Vec3(0, 0, -3), O, 0.1, false));
        // No movement.
        assertEquals(-1.0, VrSelfDamage.entry(new Vec3(0, 0, -1), new Vec3(0, 0, -1), O, 0.1, true));
    }

    @Test
    void startInsideCountsOnlyTowardsTheCentreWhenAllowed() {
        Vec3 inside = new Vec3(0, 0, -0.05);
        // Gun pressed to the head, pointing in: hit at once.
        assertEquals(0.0, VrSelfDamage.entry(inside, new Vec3(0, 0, 5), O, 0.1, true));
        // Pointing away: no hit.
        assertEquals(-1.0, VrSelfDamage.entry(inside, new Vec3(0, 0, -5), O, 0.1, true));
        // Hands never count from inside (a muzzle next to the supporting hand).
        assertEquals(-1.0, VrSelfDamage.entry(inside, new Vec3(0, 0, 5), O, 0.1, false));
    }
}
