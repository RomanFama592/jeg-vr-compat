package com.jegvr.client;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VrAxesGeometryTest {
    private static void assertNear(Vec3 expected, Vec3 actual) {
        assertTrue(expected.distanceTo(actual) < 1.0E-6, expected + " vs " + actual);
    }

    @Test
    void positiveAnglesTurnLikeThePose() {
        // Yaw turns the barrel (-Z) to the left, pitch lifts it, roll tips the top of the gun (+Y) to the left.
        assertNear(new Vec3(-1, 0, 0), VrAxesGeometry.onRing(VrAxesGeometry.Y, 90, 1));
        assertNear(new Vec3(0, 1, 0), VrAxesGeometry.onRing(VrAxesGeometry.X, 90, 1));
        assertNear(new Vec3(-1, 0, 0), VrAxesGeometry.onRing(VrAxesGeometry.Z, 90, 1));
    }

    @Test
    void theGuideFollowsThePose() {
        // Yaw 90 then pitch 90: the barrel (-Z) points up and the top of the gun (+Y) to the right; then the offset.
        assertNear(new Vec3(0.1, 1.2, 0.3), VrAxesGeometry.posed(new Vec3(0, 0, -1), 90, 90, 0, 0.1F, 0.2F, 0.3F));
        assertNear(new Vec3(1.1, 0.2, 0.3), VrAxesGeometry.posed(new Vec3(0, 1, 0), 90, 90, 0, 0.1F, 0.2F, 0.3F));
        // Roll turns about the barrel, wherever the barrel points: it stays put.
        assertNear(new Vec3(0, 1, 0), VrAxesGeometry.posed(new Vec3(0, 0, -1), 90, 90, 57, 0, 0, 0));
    }

    @Test
    void everyRingHasEightMarksAndStaysOnItsRadius() {
        List<VrAxesGeometry.Segment> lines = VrAxesGeometry.build();
        // 3 arrows of 3 lines; per ring: 48 of the circle, 8 marks, 2 of the arrow head.
        assertEquals(9 + 3 * (48 + 8 + 2), lines.size());
        for (VrAxesGeometry.Segment s : lines) {
            assertTrue(s.to().length() <= VrAxesGeometry.AXIS_LENGTH + 1.0E-9);
        }
    }
}
