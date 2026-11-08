package com.jegvr.client;

import com.jegvr.compat.VR;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Where the hand-reload and precision boxes are, from the poses alone: no Vivecraft or game state, so it can be
 * tested. Every size is in room-scale metres and is multiplied by Vivecraft's world scale ({@code scale}).
 */
final class VrZones {
    /** From the headset: down, to the side, forward. */
    static final Vec3 HIP = new Vec3(0.65, 0.2, 0.05);
    /** Seated: in front of the belly, clear of the chair. */
    static final Vec3 FRONT = new Vec3(0.45, 0.15, 0.3);
    static final Vec3 POUCH_HALF = new Vec3(0.17, 0.22, 0.17);
    static final double MAGWELL_FORWARD = 0.1;
    static final double MAGWELL_DOWN = 0.08;
    static final Vec3 MAGWELL_HALF = new Vec3(0.1, 0.1, 0.1);
    static final Vec3 MAGAZINE_HALF = new Vec3(0.04, 0.07, 0.04);

    /** The foregrip box's nearest place along the gun's barrel. */
    static final double FOREGRIP_MIN_DEPTH = 0.05;
    static final double FOREGRIP_DOWN = 0.02;
    static final Vec3 FOREGRIP_HALF = new Vec3(0.08, 0.08, 0.08);
    /** With both hands on the gun, the hands may be this far apart (between the grip and the end of the barrel). */
    static final double MIN_HAND_GAP = FOREGRIP_MIN_DEPTH;
    static final double MAX_HAND_GAP = 0.7;
    /** The line between the hands may turn this far from the gun controller before the free hand lets go. */
    static final double MAX_TWIST_COS = Math.cos(Math.toRadians(50.0));

    private static final double MIN_DIR_SQR = 1.0E-6;
    private static final double MIN_LEVEL_DIR_SQR = 1.0E-4;

    private VrZones() {
    }

    /**
     * The pouch on the free hand's side, following the headset's yaw: at the hip, or in front of the belly when
     * {@code front}. Null when the headset looks straight up or down (no yaw).
     *
     * @param freeHandOnLeft the free hand is the player's left one
     */
    static AABB pouch(VR.Aim hmd, boolean front, boolean freeHandOnLeft, double scale) {
        Vec3 forward = new Vec3(hmd.dir().x, 0.0, hmd.dir().z);
        if (forward.lengthSqr() < MIN_LEVEL_DIR_SQR) {
            return null;
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 offset = front ? FRONT : HIP;
        double side = freeHandOnLeft ? -1.0 : 1.0;
        Vec3 center = hmd.pos()
                .add(0.0, -offset.x * scale, 0.0)
                .add(right.scale(offset.y * side * scale))
                .add(forward.scale(offset.z * scale));
        return box(center, POUCH_HALF, scale);
    }

    /** Just below and ahead of the gun hand, where the magazine goes in; null without a direction. */
    static AABB magwell(VR.Aim gun, double scale) {
        if (gun.dir().lengthSqr() < MIN_DIR_SQR) {
            return null;
        }
        Vec3 center = gun.pos().add(gun.dir().normalize().scale(MAGWELL_FORWARD * scale))
                .add(0.0, -MAGWELL_DOWN * scale, 0.0);
        return box(center, MAGWELL_HALF, scale);
    }

    /** The magazine held in the free hand. */
    static AABB magazine(Vec3 freeHand, double scale) {
        return box(freeHand, MAGAZINE_HALF, scale);
    }

    /**
     * Centre of the foregrip box: slides along the barrel to stay level with the free hand (or at its nearest place
     * without one), between {@link #FOREGRIP_MIN_DEPTH} and {@link #MAX_HAND_GAP}. Null without a direction.
     */
    static Vec3 foregrip(VR.Aim gun, Vec3 freeHand, double scale) {
        if (gun.dir().lengthSqr() < MIN_DIR_SQR) {
            return null;
        }
        Vec3 barrel = gun.dir().normalize();
        double depth = freeHand == null ? FOREGRIP_MIN_DEPTH * scale : freeHand.subtract(gun.pos()).dot(barrel);
        depth = Math.max(FOREGRIP_MIN_DEPTH * scale, Math.min(MAX_HAND_GAP * scale, depth));
        return gun.pos().add(barrel.scale(depth)).add(0.0, -FOREGRIP_DOWN * scale, 0.0);
    }

    /** The foregrip box around {@code center} (or around the free hand while it holds the gun). */
    static AABB foregripBox(Vec3 center, double scale) {
        return box(center, FOREGRIP_HALF, scale);
    }

    /**
     * Unit vector from the gun hand to the free hand, or null when they are not holding one gun together: too close,
     * too far, or the line turned too far from the gun.
     */
    static Vec3 twoHandDir(VR.Aim gun, Vec3 freeHand, double scale) {
        if (gun.dir().lengthSqr() < MIN_DIR_SQR) {
            return null;
        }
        Vec3 line = freeHand.subtract(gun.pos());
        double gap = line.length();
        if (gap < MIN_HAND_GAP * scale || gap > MAX_HAND_GAP * scale) {
            return null;
        }
        Vec3 dir = line.scale(1.0 / gap);
        return dir.dot(gun.dir().normalize()) < MAX_TWIST_COS ? null : dir;
    }

    static AABB box(Vec3 center, Vec3 half, double scale) {
        Vec3 h = half.scale(scale);
        return new AABB(center.subtract(h), center.add(h));
    }
}
