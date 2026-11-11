package com.jegvr.client;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The lines of the calibration guide of {@code /jegvr axes}, in the gun's own axes and in metres (no game state: it has
 * tests): three arrows for X, Y and Z, and one ring around each (pitch around X, yaw around Y, roll around Z).
 * {@link #posed} takes a point of the guide to the controller's axes with the gun's pose, so the guide sits on the gun
 * and turns with it.
 */
public final class VrAxesGeometry {
    public static final int X = 0;
    public static final int Y = 1;
    public static final int Z = 2;

    static final double AXIS_LENGTH = 0.2;
    static final double YAW_RADIUS = 0.14;
    static final double PITCH_RADIUS = 0.12;
    static final double ROLL_RADIUS = 0.1;
    private static final double ARROW = 0.02;
    private static final double LONG_MARK = 0.03;
    private static final double SHORT_MARK = 0.012;
    private static final int RING_STEPS = 48;
    /** Where on a ring the arrow of its positive direction sits, between the zero mark and the first small one. */
    private static final double ARROW_AT = 30.0;
    private static final double ARROW_SPAN = 9.0;

    /** One line; {@code axis} gives its colour. */
    public record Segment(Vec3 from, Vec3 to, int axis) {
    }

    private VrAxesGeometry() {
    }

    public static List<Segment> build() {
        List<Segment> out = new ArrayList<>();
        axis(out, X, new Vec3(1, 0, 0), new Vec3(0, 1, 0));
        axis(out, Y, new Vec3(0, 1, 0), new Vec3(1, 0, 0));
        axis(out, Z, new Vec3(0, 0, 1), new Vec3(0, 1, 0));
        ring(out, Y, YAW_RADIUS);
        ring(out, X, PITCH_RADIUS);
        ring(out, Z, ROLL_RADIUS);
        return out;
    }

    /**
     * A point of the gun's axes in the controller's: the same translation and turns, in the same order, as
     * {@code VrGunPose.apply} (the guide keeps its size: the pose's scale is left out).
     */
    public static Vec3 posed(Vec3 point, float pitch, float yaw, float roll, float tx, float ty, float tz) {
        return rotate(Y, yaw, rotate(X, pitch, rotate(Z, roll, point))).add(tx, ty, tz);
    }

    /** An arrow from the origin along the positive side of an axis. */
    private static void axis(List<Segment> out, int axis, Vec3 dir, Vec3 side) {
        Vec3 tip = dir.scale(AXIS_LENGTH);
        Vec3 back = dir.scale(AXIS_LENGTH - ARROW);
        out.add(new Segment(Vec3.ZERO, tip, axis));
        out.add(new Segment(tip, back.add(side.scale(ARROW * 0.5)), axis));
        out.add(new Segment(tip, back.subtract(side.scale(ARROW * 0.5)), axis));
    }

    /**
     * The ring of one angle: a circle with a long mark every 90 degrees and a short one every 45, a longer mark at zero,
     * and an arrow head pointing the way the angle grows.
     */
    private static void ring(List<Segment> out, int axis, double radius) {
        for (int i = 0; i < RING_STEPS; i++) {
            out.add(new Segment(onRing(axis, i * 360.0 / RING_STEPS, radius),
                    onRing(axis, (i + 1) * 360.0 / RING_STEPS, radius), axis));
        }
        for (int degrees = 0; degrees < 360; degrees += 45) {
            double mark = degrees % 90 == 0 ? LONG_MARK : SHORT_MARK;
            double inner = degrees == 0 ? radius - LONG_MARK : radius;
            out.add(new Segment(onRing(axis, degrees, inner), onRing(axis, degrees, radius + mark), axis));
        }
        Vec3 head = onRing(axis, ARROW_AT, radius);
        out.add(new Segment(head, onRing(axis, ARROW_AT - ARROW_SPAN, radius + SHORT_MARK), axis));
        out.add(new Segment(head, onRing(axis, ARROW_AT - ARROW_SPAN, radius - SHORT_MARK), axis));
    }

    /** The point of a ring at an angle. */
    static Vec3 onRing(int axis, double degrees, double radius) {
        return rotate(axis, degrees, zero(axis).scale(radius));
    }

    /** Where an angle of zero points: the barrel (-Z) for pitch and yaw, the top of the gun (+Y) for roll. */
    static Vec3 zero(int axis) {
        return axis == Z ? new Vec3(0, 1, 0) : new Vec3(0, 0, -1);
    }

    /** A right-handed turn about a positive axis, as {@code Axis.XP/YP/ZP.rotationDegrees} does. */
    static Vec3 rotate(int axis, double degrees, Vec3 v) {
        double a = Math.toRadians(degrees);
        double c = Math.cos(a);
        double s = Math.sin(a);
        return switch (axis) {
            case X -> new Vec3(v.x, v.y * c - v.z * s, v.y * s + v.z * c);
            case Y -> new Vec3(v.x * c + v.z * s, v.y, -v.x * s + v.z * c);
            default -> new Vec3(v.x * c - v.y * s, v.x * s + v.y * c, v.z);
        };
    }
}
