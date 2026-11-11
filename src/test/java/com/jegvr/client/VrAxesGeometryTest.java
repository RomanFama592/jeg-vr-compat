package com.jegvr.client;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VrAxesGeometryTest {
    private static void assertNear(Vec3 expected, Vec3 actual) {
        assertTrue(expected.distanceTo(actual) < 1.0E-9, expected + " vs " + actual);
    }

    @Test
    void positiveAnglesTurnLikeThePose() {
        // Yaw turns the barrel (-Z) to the left, pitch lifts it, roll tips the top of the gun (+Y) to the left.
        assertNear(new Vec3(-1, 0, 0), VrAxesGeometry.onRing(VrAxesGeometry.Y, 90, 1));
        assertNear(new Vec3(0, 1, 0), VrAxesGeometry.onRing(VrAxesGeometry.X, 90, 1));
        assertNear(new Vec3(-1, 0, 0), VrAxesGeometry.onRing(VrAxesGeometry.Z, 90, 1));
    }

    @Test
    void theRollNeedleIsTheTopOfTheGun() {
        List<VrAxesGeometry.Segment> lines = VrAxesGeometry.build(90.0F, 90.0F, 0.0F);
        List<VrAxesGeometry.Segment> needles = lines.stream().filter(VrAxesGeometry.Segment::needle).toList();
        assertEquals(3, needles.size());
        // Yaw 90 then pitch 90: the barrel points up and the top of the gun points to the right (+X).
        VrAxesGeometry.Segment pitch = needles.stream().filter(s -> s.axis() == VrAxesGeometry.X).findFirst().orElseThrow();
        VrAxesGeometry.Segment roll = needles.stream().filter(s -> s.axis() == VrAxesGeometry.Z).findFirst().orElseThrow();
        assertNear(new Vec3(0, VrAxesGeometry.PITCH_RADIUS, 0), pitch.to());
        assertNear(new Vec3(VrAxesGeometry.ROLL_RADIUS, 0, 0), roll.to());
    }

    @Test
    void everyRingHasEightMarksAndStaysOnItsRadius() {
        List<VrAxesGeometry.Segment> lines = VrAxesGeometry.build(20.0F, -35.0F, 70.0F);
        // 3 arrows of 3 lines; per ring: 48 of the circle, 8 marks, 2 of the arrow head, 1 needle.
        assertEquals(9 + 3 * (48 + 8 + 2 + 1), lines.size());
        for (VrAxesGeometry.Segment s : lines) {
            assertTrue(s.to().length() <= VrAxesGeometry.AXIS_LENGTH + 1.0E-9);
        }
    }
}
