package com.jegvr.client;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * F10.4: where the free hand's controller holds a two-handed gun, relative to the gun's controller. No game state, so it
 * can be tested. Stored for a gun held in the right hand (as JEG models are made); a gun in the left hand uses it
 * mirrored across the controller's X axis, like the gun itself ({@link VrGunMirror}). Distances are metres in the room:
 * Vivecraft's world scale, which the controller frames carry, is taken out and put back.
 */
final class VrGripPose {
    private static final Matrix4f MIRROR = new Matrix4f().scaling(-1.0F, 1.0F, 1.0F);

    private VrGripPose() {
    }

    /** The free controller's frame in the gun controller's: position (room metres) and rotation, as stored. */
    static VrGunConfig.Grip capture(Matrix4f gunController, Matrix4f freeController, boolean mirrored) {
        Matrix4f local = new Matrix4f(gunController).invert().mul(freeController);
        if (mirrored) {
            local = new Matrix4f(MIRROR).mul(local).mul(MIRROR);
        }
        Vector3f position = local.getTranslation(new Vector3f());
        Quaternionf rotation = local.getNormalizedRotation(new Quaternionf());
        VrGunConfig.Grip grip = new VrGunConfig.Grip();
        grip.x = VrGunPoseFile.round(position.x);
        grip.y = VrGunPoseFile.round(position.y);
        grip.z = VrGunPoseFile.round(position.z);
        grip.qx = VrGunPoseFile.round(rotation.x);
        grip.qy = VrGunPoseFile.round(rotation.y);
        grip.qz = VrGunPoseFile.round(rotation.z);
        grip.qw = VrGunPoseFile.round(rotation.w);
        return grip;
    }

    /** Where the free controller goes for {@code grip}, in the frame {@code gunController} lives in (scale kept). */
    static Matrix4f apply(Matrix4f gunController, VrGunConfig.Grip grip, boolean mirrored) {
        Quaternionf rotation = new Quaternionf(grip.qx, grip.qy, grip.qz, grip.qw).normalize();
        Matrix4f local = new Matrix4f().translationRotate(grip.x, grip.y, grip.z, rotation);
        if (mirrored) {
            local = new Matrix4f(MIRROR).mul(local).mul(MIRROR);
        }
        return new Matrix4f(gunController).mul(local);
    }
}
