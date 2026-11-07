package com.jegvr.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A mirrored gun's faces must keep every value of each vertex and only change the order the vertices arrive in. */
class VrGunMirrorTest {
    /** Writes down what reaches the real buffer. */
    private static final class Recorder implements VertexConsumer {
        final List<String> calls = new ArrayList<>();

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            calls.add("pos " + x + " " + y + " " + z);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            calls.add("color " + r + " " + g + " " + b + " " + a);
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            calls.add("uv " + u + " " + v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            calls.add("overlay " + u + " " + v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            calls.add("light " + u + " " + v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            calls.add("normal " + x + " " + y + " " + z);
            return this;
        }

        @Override
        public void endVertex() {
            calls.add("end");
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
        }

        @Override
        public void unsetDefaultColor() {
        }
    }

    private static void fullVertex(VertexConsumer consumer, int i) {
        consumer.vertex(i + 0.25, i + 0.5, i + 0.75).color(i, i + 1, i + 2, 255).uv(i * 0.1F, i * 0.2F)
                .overlayCoords(i, i + 3).uv2(i + 4, i + 5).normal(i * 0.3F, i * 0.4F, i * 0.5F).endVertex();
    }

    private static List<String> expected(int... order) {
        Recorder recorder = new Recorder();
        for (int i : order) {
            fullVertex(recorder, i);
        }
        return recorder.calls;
    }

    @Test
    void quadsArriveAsZeroThreeTwoOne() {
        Recorder out = new Recorder();
        VertexConsumer reversed = new VrGunMirror.ReversedWinding(out, 4);
        for (int i = 0; i < 4; i++) {
            fullVertex(reversed, i);
        }
        assertEquals(expected(0, 3, 2, 1), out.calls);
    }

    @Test
    void trianglesArriveAsZeroTwoOne() {
        Recorder out = new Recorder();
        VertexConsumer reversed = new VrGunMirror.ReversedWinding(out, 3);
        for (int i = 0; i < 3; i++) {
            fullVertex(reversed, i);
        }
        assertEquals(expected(0, 2, 1), out.calls);
    }

    @Test
    void nothingArrivesUntilTheFaceIsComplete() {
        Recorder out = new Recorder();
        VertexConsumer reversed = new VrGunMirror.ReversedWinding(out, 4);
        for (int i = 0; i < 3; i++) {
            fullVertex(reversed, i);
        }
        assertEquals(List.of(), out.calls);
    }

    @Test
    void consecutiveFacesDoNotMix() {
        Recorder out = new Recorder();
        VertexConsumer reversed = new VrGunMirror.ReversedWinding(out, 4);
        for (int i = 0; i < 8; i++) {
            fullVertex(reversed, i);
        }
        assertEquals(expected(0, 3, 2, 1, 4, 7, 6, 5), out.calls);
    }

    @Test
    void elementsKeepTheOrderTheyCameIn() {
        Recorder out = new Recorder();
        VertexConsumer reversed = new VrGunMirror.ReversedWinding(out, 3);
        for (int i = 0; i < 3; i++) {
            reversed.vertex(i, 0.0, 0.0).uv(0.5F, 0.5F).color(1, 2, 3, 4).endVertex();
        }
        assertEquals(List.of("pos 0.0 0.0 0.0", "uv 0.5 0.5", "color 1 2 3 4", "end"), out.calls.subList(0, 4));
    }
}
