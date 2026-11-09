package com.jegvr.client;

import com.jegvr.compat.VR;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Where the hand-reload and precision boxes go, for given headset and controller poses. */
class VrZonesTest {
    private static final double EPS = 1.0E-9;
    /** Headset at the origin looking south (+Z). Facing south, east (+X) is on the player's left. */
    private static final VR.Aim LOOKING_SOUTH = new VR.Aim(Vec3.ZERO, new Vec3(0, 0, 1));

    @Test
    void hipPouchOnTheFreeHandsSide() {
        AABB left = VrZones.pouch(LOOKING_SOUTH, false, true, 1.0);
        assertCenter(new Vec3(0.2, -0.65, 0.05), left);
        AABB right = VrZones.pouch(LOOKING_SOUTH, false, false, 1.0);
        assertCenter(new Vec3(-0.2, -0.65, 0.05), right);
        assertEquals(0.34, left.getXsize(), EPS);
        assertEquals(0.44, left.getYsize(), EPS);
    }

    @Test
    void seatedPouchInFrontOfTheBelly() {
        assertCenter(new Vec3(0.15, -0.45, 0.3), VrZones.pouch(LOOKING_SOUTH, true, true, 1.0));
    }

    @Test
    void pouchFollowsYawButNotPitch() {
        // Looking east and 45 degrees down: same height under the head, and "forward" is east.
        VR.Aim eastDown = new VR.Aim(Vec3.ZERO, new Vec3(1, -1, 0).normalize());
        // Facing east, north (-Z) is on the left.
        assertCenter(new Vec3(0.05, -0.65, -0.2), VrZones.pouch(eastDown, false, true, 1.0));
    }

    @Test
    void noPouchLookingStraightDown() {
        assertNull(VrZones.pouch(new VR.Aim(Vec3.ZERO, new Vec3(0, -1, 0)), false, true, 1.0));
    }

    @Test
    void worldScaleScalesOffsetsAndSizes() {
        AABB big = VrZones.pouch(LOOKING_SOUTH, false, true, 2.0);
        assertCenter(new Vec3(0.4, -1.3, 0.1), big);
        assertEquals(0.68, big.getXsize(), EPS);
    }

    @Test
    void magwellBelowAndAheadOfTheGunHand() {
        VR.Aim gun = new VR.Aim(new Vec3(1, 1, 1), new Vec3(0, 0, 2));
        assertCenter(new Vec3(1, 0.92, 1.1), VrZones.magwell(gun, 1.0));
        assertNull(VrZones.magwell(new VR.Aim(Vec3.ZERO, Vec3.ZERO), 1.0));
    }

    @Test
    void magazineTouchesMagwellWhenHeldUnderTheGun() {
        VR.Aim gun = new VR.Aim(Vec3.ZERO, new Vec3(0, 0, 1));
        AABB magwell = VrZones.magwell(gun, 1.0);
        assertTrue(VrZones.magazine(new Vec3(0, -0.1, 0.12), 1.0).intersects(magwell));
        assertFalse(VrZones.magazine(new Vec3(0, -0.5, 0.12), 1.0).intersects(magwell));
    }

    @Test
    void foregripSlidesAlongTheBarrelWithinLimits() {
        VR.Aim gun = new VR.Aim(Vec3.ZERO, new Vec3(0, 0, 1));
        // No free hand: nearest place.
        assertVec(new Vec3(0, -0.02, VrZones.FOREGRIP_MIN_DEPTH), VrZones.foregrip(gun, null, 1.0));
        // Level with the free hand, whatever its sideways offset.
        assertVec(new Vec3(0, -0.02, 0.3), VrZones.foregrip(gun, new Vec3(0.1, 0.05, 0.3), 1.0));
        // Never behind the grip, never past the reach of two hands.
        assertVec(new Vec3(0, -0.02, VrZones.FOREGRIP_MIN_DEPTH), VrZones.foregrip(gun, new Vec3(0, 0, -0.4), 1.0));
        assertVec(new Vec3(0, -0.02, 0.7), VrZones.foregrip(gun, new Vec3(0, 0, 3), 1.0));
    }

    @Test
    void twoHandDirectionPointsFromGunHandToFreeHand() {
        VR.Aim gun = new VR.Aim(Vec3.ZERO, new Vec3(0, 0, 1));
        Vec3 dir = VrZones.twoHandDir(gun, new Vec3(0, 0.1, 0.3), 1.0);
        assertNotNull(dir);
        assertEquals(1.0, dir.length(), EPS);
        assertTrue(dir.y > 0 && dir.z > 0);
    }

    @Test
    void twoHandDirectionNeedsBothHandsOnOneGun() {
        VR.Aim gun = new VR.Aim(Vec3.ZERO, new Vec3(0, 0, 1));
        assertNull(VrZones.twoHandDir(gun, new Vec3(0, 0, 0.02), 1.0), "hands together");
        assertNull(VrZones.twoHandDir(gun, new Vec3(0, 0, 0.9), 1.0), "hands too far apart");
        double far = Math.toRadians(60);
        assertNull(VrZones.twoHandDir(gun, new Vec3(Math.sin(far), 0, Math.cos(far)).scale(0.3), 1.0), "60 degrees off");
        double near = Math.toRadians(40);
        assertNotNull(VrZones.twoHandDir(gun, new Vec3(Math.sin(near), 0, Math.cos(near)).scale(0.3), 1.0), "40 degrees off");
    }

    private static void assertCenter(Vec3 expected, AABB box) {
        assertNotNull(box);
        assertVec(expected, box.getCenter());
    }

    private static void assertVec(Vec3 expected, Vec3 actual) {
        assertNotNull(actual);
        assertEquals(expected.x, actual.x, EPS, "x");
        assertEquals(expected.y, actual.y, EPS, "y");
        assertEquals(expected.z, actual.z, EPS, "z");
    }
}
