package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.config.VrClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * F9.17: JEG guns are modelled for the right hand (bolt, ejection port and sights on its right side). A gun in the
 * physical left controller is drawn mirrored across its X axis, animation included (the mirror applies to the whole
 * GeckoLib model), so it looks made for that hand.
 * <p>
 * {@link VrGunPose} mirrors the controller frame and then applies the right-hand pose, which places the gun exactly
 * where the left-hand pose put it. A mirror also turns every face inside out (its vertices now run clockwise), so the
 * gun is drawn through {@link #wrap}, which emits each quad's vertices in reverse order; culling then keeps the outer
 * faces. The gun is drawn with the right-hand display context, like the main gun of a right-handed player.
 */
public final class VrGunMirror {
    /** Set by {@link VrGunPose} for the gun Vivecraft is about to draw; cleared once it is drawn. */
    private static boolean pending;

    private VrGunMirror() {
    }

    /** Whether the gun in this hand is held in the physical left controller and must be mirrored. */
    public static boolean mirrors(boolean mainHand) {
        return VR.active() && VrClientConfig.mirrorLeftHand() && mainHand == VR.leftHanded();
    }

    /** Mirrors the pose across X, normals included ({@code PoseStack.scale} mishandles a single negative axis). */
    public static void mirror(PoseStack poseStack) {
        PoseStack.Pose pose = poseStack.last();
        pose.pose().scale(-1.0F, 1.0F, 1.0F);
        pose.normal().scale(-1.0F, 1.0F, 1.0F);
        pending = true;
    }

    /** True between {@link #mirror} and the end of the next {@code ItemInHandRenderer.renderItem}. */
    public static boolean pending() {
        return pending;
    }

    public static void drawn() {
        pending = false;
    }

    /** The same buffers, with every quad and triangle wound the other way. */
    public static MultiBufferSource wrap(MultiBufferSource source) {
        return type -> {
            VertexConsumer buffer = source.getBuffer(type);
            VertexFormat.Mode mode = type.mode();
            if (mode == VertexFormat.Mode.QUADS) {
                return new ReversedWinding(buffer, 4);
            }
            return mode == VertexFormat.Mode.TRIANGLES ? new ReversedWinding(buffer, 3) : buffer;
        };
    }

    /**
     * Holds the vertices of one primitive and replays them as 0, n-1, ..., 1. Each vertex's element calls are replayed in
     * their own order, which must match the buffer's vertex format. It runs for every vertex of a model, for each eye, so
     * it records into arrays it reuses and allocates nothing per vertex.
     */
    static final class ReversedWinding implements VertexConsumer {
        private static final int MAX_ELEMENTS = 6;
        private static final int VALUES_PER_ELEMENT = 4;
        private static final byte POSITION = 0;
        private static final byte COLOR = 1;
        private static final byte UV = 2;
        private static final byte OVERLAY = 3;
        private static final byte LIGHT = 4;
        private static final byte NORMAL = 5;

        private final VertexConsumer delegate;
        private final int size;
        private final byte[][] kinds;
        private final int[] counts;
        private final double[][] values;
        private int current;

        ReversedWinding(VertexConsumer delegate, int size) {
            this.delegate = delegate;
            this.size = size;
            this.kinds = new byte[size][MAX_ELEMENTS];
            this.counts = new int[size];
            this.values = new double[size][MAX_ELEMENTS * VALUES_PER_ELEMENT];
        }

        private ReversedWinding record(byte kind, double a, double b, double c, double d) {
            int count = counts[current];
            kinds[current][count] = kind;
            double[] vertex = values[current];
            int at = count * VALUES_PER_ELEMENT;
            vertex[at] = a;
            vertex[at + 1] = b;
            vertex[at + 2] = c;
            vertex[at + 3] = d;
            counts[current] = count + 1;
            return this;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            return record(POSITION, x, y, z, 0.0);
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            return record(COLOR, r, g, b, a);
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return record(UV, u, v, 0.0, 0.0);
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return record(OVERLAY, u, v, 0.0, 0.0);
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return record(LIGHT, u, v, 0.0, 0.0);
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return record(NORMAL, x, y, z, 0.0);
        }

        @Override
        public void endVertex() {
            current++;
            if (current == size) {
                emit(0);
                for (int i = size - 1; i > 0; i--) {
                    emit(i);
                }
                current = 0;
                java.util.Arrays.fill(counts, 0);
            }
        }

        private void emit(int index) {
            double[] vertex = values[index];
            for (int element = 0; element < counts[index]; element++) {
                int at = element * VALUES_PER_ELEMENT;
                double a = vertex[at];
                double b = vertex[at + 1];
                double c = vertex[at + 2];
                switch (kinds[index][element]) {
                    case POSITION -> delegate.vertex(a, b, c);
                    case COLOR -> delegate.color((int) a, (int) b, (int) c, (int) vertex[at + 3]);
                    case UV -> delegate.uv((float) a, (float) b);
                    case OVERLAY -> delegate.overlayCoords((int) a, (int) b);
                    case LIGHT -> delegate.uv2((int) a, (int) b);
                    default -> delegate.normal((float) a, (float) b, (float) c);
                }
            }
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            delegate.defaultColor(r, g, b, a);
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }
    }
}
