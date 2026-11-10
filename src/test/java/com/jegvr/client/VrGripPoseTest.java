package com.jegvr.client;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VrGripPoseTest {
    private static final float EPS = 2.0E-3F;

    /** A controller frame as Vivecraft builds it: position, rotation, then the world scale. */
    private static Matrix4f controller(float x, float y, float z, float yawDeg, float pitchDeg, float scale) {
        return new Matrix4f().translation(x, y, z).rotateY((float) Math.toRadians(yawDeg))
                .rotateX((float) Math.toRadians(pitchDeg)).scale(scale);
    }

    @Test
    void capturedGripPutsTheFreeControllerBack() {
        for (float scale : new float[]{1.0F, 2.5F}) {
            for (boolean mirrored : new boolean[]{false, true}) {
                Matrix4f gun = controller(1.0F, 1.2F, -0.4F, 30.0F, -10.0F, scale);
                Matrix4f free = controller(1.1F, 1.15F, -0.75F, 25.0F, 5.0F, scale);
                VrGunConfig.Grip grip = VrGripPose.capture(gun, free, mirrored);
                assertMatrix(free, VrGripPose.apply(gun, grip, mirrored));
            }
        }
    }

    @Test
    void gripIsInRoomMetresWhateverTheWorldScale() {
        // 25 cm ahead of the gun controller (its -Z), with the world four times bigger.
        Matrix4f gun = controller(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 4.0F);
        Matrix4f free = new Matrix4f(gun).translate(0.0F, 0.0F, -0.25F);
        VrGunConfig.Grip grip = VrGripPose.capture(gun, free, false);
        assertEquals(-0.25F, grip.z, EPS);
        assertEquals(0.0F, grip.x, EPS);
    }

    @Test
    void mirroredGripSwapsSides() {
        Matrix4f gun = controller(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        Matrix4f free = new Matrix4f(gun).translate(0.05F, -0.03F, -0.25F);
        VrGunConfig.Grip grip = VrGripPose.capture(gun, free, true);
        assertEquals(-0.05F, grip.x, EPS);
        assertEquals(-0.03F, grip.y, EPS);
        Vector3f back = VrGripPose.apply(gun, grip, true).getTranslation(new Vector3f());
        assertEquals(0.05F, back.x, EPS);
    }

    private static void assertMatrix(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), EPS, "m" + column + row);
            }
        }
    }
}
