package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.config.VrClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
     * Holds the vertices of one primitive and replays them as 0, n-1, ..., 1. Each vertex's element calls are replayed
     * in their own order, which must match the buffer's vertex format.
     */
    private static final class ReversedWinding implements VertexConsumer {
        private final VertexConsumer delegate;
        private final int size;
        private final List<List<Consumer<VertexConsumer>>> vertices = new ArrayList<>(4);
        private List<Consumer<VertexConsumer>> current = new ArrayList<>(7);

        private ReversedWinding(VertexConsumer delegate, int size) {
            this.delegate = delegate;
            this.size = size;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            current.add(c -> c.vertex(x, y, z));
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            current.add(c -> c.color(r, g, b, a));
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            current.add(c -> c.uv(u, v));
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            current.add(c -> c.overlayCoords(u, v));
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            current.add(c -> c.uv2(u, v));
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            current.add(c -> c.normal(x, y, z));
            return this;
        }

        @Override
        public void endVertex() {
            vertices.add(current);
            current = new ArrayList<>(7);
            if (vertices.size() == size) {
                emit(0);
                for (int i = size - 1; i > 0; i--) {
                    emit(i);
                }
                vertices.clear();
            }
        }

        private void emit(int index) {
            for (Consumer<VertexConsumer> element : vertices.get(index)) {
                element.accept(delegate);
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
