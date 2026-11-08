package com.jegvr.client;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The throwing gesture of one hand (F9.26, G.4), from its position in the play area once per tick: real metres, so
 * walking or turning with the stick does not count as throwing. No game state, so it can be tested.
 */
final class VrThrowMotion {
    static final int TICKS_PER_SECOND = 20;
    /** Slower than this at release (metres per second) is a gentle toss, not a throw. */
    static final double MIN_THROW_SPEED = 1.0;
    /** Hand speed (m/s) to projectile speed (blocks per tick): about 4 m/s gives JEG's own 1.0, 6 m/s the maximum. */
    static final double SPEED_FACTOR = 0.25;
    /** The release is noisy: the fastest of the last few ticks is the throw. */
    private static final int WINDOW = 3;

    private final Vec3[] positions = new Vec3[WINDOW + 1];
    private int count;

    /** Adds this tick's position; null (hand not tracked) forgets the motion. */
    void push(Vec3 roomPosition) {
        if (roomPosition == null) {
            count = 0;
            return;
        }
        System.arraycopy(positions, 0, positions, 1, positions.length - 1);
        positions[0] = roomPosition;
        count = Math.min(count + 1, positions.length);
    }

    void clear() {
        count = 0;
    }

    /** The fastest velocity of the last ticks, in metres per second in the play area; zero without two samples. */
    Vec3 peakVelocity() {
        Vec3 best = Vec3.ZERO;
        for (int i = 0; i + 1 < count; i++) {
            Vec3 v = positions[i].subtract(positions[i + 1]).scale(TICKS_PER_SECOND);
            if (v.lengthSqr() > best.lengthSqr()) {
                best = v;
            }
        }
        return best;
    }

    /** A play-area direction in the world: the play area is turned by {@code roomYaw} radians about Y. */
    static Vec3 toWorld(Vec3 roomVector, float roomYaw) {
        return roomVector.yRot(roomYaw);
    }

    /** True when the hand moved fast enough at release to count as a throw. */
    static boolean isThrow(Vec3 velocity) {
        return velocity.length() >= MIN_THROW_SPEED;
    }

    /** Projectile speed (blocks per tick) for a hand speed in metres per second. */
    static float projectileSpeed(double handSpeed, float min, float max) {
        return Mth.clamp((float) (handSpeed * SPEED_FACTOR), min, max);
    }
}
