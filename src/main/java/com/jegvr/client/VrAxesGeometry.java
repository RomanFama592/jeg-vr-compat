package com.jegvr.client;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The lines of the calibration guide of {@code /jegvr axes}, in the controller's axes and in metres (no game state:
 * it has tests). Three arrows for the axes {@code /jegvr move} uses, and one ring per angle of {@code /jegvr rot},
 * nested like a gimbal in the order the pose applies them (yaw, then pitch, then roll): the pitch ring turns with the
 * yaw and the roll ring with both, so each ring lies where its angle turns the gun now.
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

    /** One line. {@code axis} gives its colour; {@code needle} marks the lines that show the pose's current angles. */
    public record Segment(Vec3 from, Vec3 to, int axis, boolean needle) {
    }

    private VrAxesGeometry() {
    }

    public static List<Segment> build(float pitch, float yaw, float roll) {
        List<Segment> out = new ArrayList<>();
        axis(out, X, new Vec3(1, 0, 0), new Vec3(0, 1, 0));
        axis(out, Y, new Vec3(0, 1, 0), new Vec3(1, 0, 0));
        axis(out, Z, new Vec3(0, 0, 1), new Vec3(0, 1, 0));
        ring(out, Y, YAW_RADIUS, yaw, 0.0F, 0.0F, false);
        ring(out, X, PITCH_RADIUS, pitch, yaw, 0.0F, false);
        ring(out, Z, ROLL_RADIUS, roll, yaw, pitch, true);
        return out;
    }

    /** An arrow from the origin along the positive side of an axis. */
    private static void axis(List<Segment> out, int axis, Vec3 dir, Vec3 side) {
        Vec3 tip = dir.scale(AXIS_LENGTH);
        Vec3 back = dir.scale(AXIS_LENGTH - ARROW);
        out.add(new Segment(Vec3.ZERO, tip, axis, false));
        out.add(new Segment(tip, back.add(side.scale(ARROW * 0.5)), axis, false));
        out.add(new Segment(tip, back.subtract(side.scale(ARROW * 0.5)), axis, false));
    }

    /**
     * The ring of one angle: a circle with a long mark every 90 degrees and a short one every 45, a longer mark at zero,
     * an arrow head pointing the way the angle grows, and a needle at the current value.
     */
    private static void ring(List<Segment> out, int axis, double radius, float value, float yaw, float pitch,
                             boolean pitched) {
        for (int i = 0; i < RING_STEPS; i++) {
            out.add(new Segment(framed(onRing(axis, i * 360.0 / RING_STEPS, radius), yaw, pitch, pitched),
                    framed(onRing(axis, (i + 1) * 360.0 / RING_STEPS, radius), yaw, pitch, pitched), axis, false));
        }
        for (int degrees = 0; degrees < 360; degrees += 45) {
            double mark = degrees % 90 == 0 ? LONG_MARK : SHORT_MARK;
            double inner = degrees == 0 ? radius - LONG_MARK : radius;
            out.add(new Segment(framed(onRing(axis, degrees, inner), yaw, pitch, pitched),
                    framed(onRing(axis, degrees, radius + mark), yaw, pitch, pitched), axis, false));
        }
        Vec3 head = framed(onRing(axis, ARROW_AT, radius), yaw, pitch, pitched);
        out.add(new Segment(head, framed(onRing(axis, ARROW_AT - ARROW_SPAN, radius + SHORT_MARK), yaw, pitch, pitched),
                axis, false));
        out.add(new Segment(head, framed(onRing(axis, ARROW_AT - ARROW_SPAN, radius - SHORT_MARK), yaw, pitch, pitched),
                axis, false));
        out.add(new Segment(Vec3.ZERO, framed(onRing(axis, value, radius), yaw, pitch, pitched), axis, true));
    }

    /** The point of a ring at an angle, before the ring is turned by the angles applied ahead of its own. */
    static Vec3 onRing(int axis, double degrees, double radius) {
        return rotate(axis, degrees, zero(axis).scale(radius));
    }

    /** Where an angle of zero points: the barrel (-Z) for pitch and yaw, the top of the gun (+Y) for roll. */
    static Vec3 zero(int axis) {
        return axis == Z ? new Vec3(0, 1, 0) : new Vec3(0, 0, -1);
    }

    private static Vec3 framed(Vec3 point, float yaw, float pitch, boolean pitched) {
        Vec3 turned = pitched ? rotate(X, pitch, point) : point;
        return rotate(Y, yaw, turned);
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
