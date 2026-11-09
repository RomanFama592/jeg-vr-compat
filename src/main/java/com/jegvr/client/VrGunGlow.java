package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.mixin.render.CompositeRenderTypeAccessor;
import com.jegvr.mixin.render.CompositeStateAccessor;
import com.jegvr.mixin.render.TextureStateShardAccessor;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * F10.4: while the free hand is near a two-handed gun, the gun itself glows where it can be held: its own texture is
 * added again in the state's colour (blue: can be held, green: hand on it, violet: holding), with soft rings rippling
 * out from that point along the gun ({@link VrGlow#gunWave}). Like an enchantment glint, every face of the gun is
 * copied, while it is drawn, into an additive layer ({@code RenderType.eyes} with the gun's texture) that writes no
 * depth; the faces are cut into small pieces so a ring stays smooth across long parts like the barrel. The copies are
 * kept and drawn right after the gun, so they never interleave with the gun's own buffers.
 */
public final class VrGunGlow {
    /** Largest side of a piece of a face, metres in the room: the wave is computed at the pieces' corners. */
    private static final float CELL = 0.015F;
    private static final int MAX_CUTS = 16;
    /** How much colour the layer adds at full strength (0–1). */
    private static final float STRENGTH = 0.85F;
    /** The copies sit this fraction closer to the eye than the gun, so they never fight its depth. */
    private static final float DEPTH_PULL = 0.002F;
    /** Values kept per copied vertex: position, colour, uv, normal. */
    private static final int STRIDE = 11;

    /** What the gun being drawn glows with, or null; set around {@code ItemInHandRenderer.renderItem}. */
    private static Vec3 center;
    private static float[] rgb;
    private static float fade;
    private static float time;
    private static float scale;
    private static MultiBufferSource target;

    private static final Map<RenderType, ResourceLocation> TEXTURES = new IdentityHashMap<>();
    /** Each wrapper class's {@code unwrap()} (null when it has none). */
    private static final Map<Class<?>, Method> UNWRAP = new HashMap<>();
    private static final int MAX_UNWRAP = 4;
    private static final ResourceLocation NO_TEXTURE = new ResourceLocation("jeg_vr_compat", "none");
    private static final Map<ResourceLocation, float[]> COPIES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Integer> COPY_SIZES = new LinkedHashMap<>();
    // Corners of the piece being cut (render thread only).
    private static final float[] P00 = new float[8];
    private static final float[] P10 = new float[8];
    private static final float[] P11 = new float[8];
    private static final float[] P01 = new float[8];

    private VrGunGlow() {
    }

    /**
     * Called with the buffers of the gun Vivecraft is about to draw in {@code drawnHand}: returns them wrapped when that
     * gun should glow, unchanged otherwise.
     */
    public static MultiBufferSource wrap(MultiBufferSource buffers, InteractionHand drawnHand, ItemStack stack) {
        target = null;
        Minecraft game = Minecraft.getInstance();
        // Only the gun itself: whatever else is drawn afterwards still carries the last gun's hand.
        boolean isThatGun = drawnHand != null && game.player != null && !stack.isEmpty()
                && ItemStack.isSameItem(stack, game.player.getItemInHand(drawnHand));
        VrPrecision.Glow glow = !isThatGun || !VR.isEyePass() ? null : VrPrecision.glow(drawnHand);
        if (glow == null) {
            return buffers;
        }
        Minecraft mc = Minecraft.getInstance();
        // Vivecraft draws the hands in world axes, relative to the eye.
        center = glow.center().subtract(mc.gameRenderer.getMainCamera().getPosition());
        rgb = glow.rgb();
        fade = glow.fade();
        scale = VR.worldScale();
        time = (mc.player == null ? 0 : mc.player.tickCount) + mc.getFrameTime();
        target = buffers;
        COPY_SIZES.replaceAll((texture, size) -> 0);
        return type -> {
            VertexConsumer gun = buffers.getBuffer(type);
            ResourceLocation texture = type.mode() == VertexFormat.Mode.QUADS ? texture(type) : null;
            return texture == null ? gun : new Tee(gun, texture);
        };
    }

    /** After the gun is drawn: draws its glowing copies over it. */
    public static void flush() {
        MultiBufferSource buffers = target;
        target = null;
        if (buffers == null) {
            return;
        }
        for (Map.Entry<ResourceLocation, Integer> entry : COPY_SIZES.entrySet()) {
            int size = entry.getValue();
            if (size == 0) {
                continue;
            }
            float[] data = COPIES.get(entry.getKey());
            VertexConsumer layer = buffers.getBuffer(RenderType.eyes(entry.getKey()));
            for (int at = 0; at < size; at += STRIDE) {
                layer.vertex(data[at], data[at + 1], data[at + 2])
                        .color(data[at + 3], data[at + 4], data[at + 5], 1.0F)
                        .uv(data[at + 6], data[at + 7])
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(LightTexture.FULL_BRIGHT)
                        .normal(data[at + 8], data[at + 9], data[at + 10])
                        .endVertex();
            }
        }
    }

    /**
     * The texture of a render type, or null for untextured types and glints (their texture is not the gun's). Shader
     * mods wrap render types (Oculus tags each entity's types, and wraps them again at times): those are unwrapped by
     * their {@code unwrap()} method, found by reflection so this addon never imports them. Only Minecraft's own types are
     * remembered; wrappers may be new objects every frame.
     */
    private static ResourceLocation texture(RenderType type) {
        RenderType inner = type;
        for (int depth = 0; depth < MAX_UNWRAP && !(inner instanceof CompositeRenderTypeAccessor); depth++) {
            inner = unwrap(inner);
            if (inner == null) {
                return null;
            }
        }
        if (!(inner instanceof CompositeRenderTypeAccessor)) {
            return null;
        }
        ResourceLocation texture = TEXTURES.computeIfAbsent(inner, key -> {
            CompositeRenderTypeAccessor composite = (CompositeRenderTypeAccessor) key;
            if (key.toString().contains("glint")) {
                return NO_TEXTURE;
            }
            Optional<ResourceLocation> found = ((TextureStateShardAccessor) ((CompositeStateAccessor) (Object)
                    composite.jegvr$state()).jegvr$textureState()).jegvr$cutoutTexture();
            return found.orElse(NO_TEXTURE);
        });
        return texture == NO_TEXTURE ? null : texture;
    }

    /** {@code type.unwrap()} when its class has that method (a shader mod's wrapper), or null. */
    private static RenderType unwrap(RenderType type) {
        Method method = UNWRAP.computeIfAbsent(type.getClass(), cls -> {
            try {
                Method found = cls.getMethod("unwrap");
                return RenderType.class.isAssignableFrom(found.getReturnType()) ? found : null;
            } catch (NoSuchMethodException | SecurityException e) {
                return null;
            }
        });
        if (method == null) {
            return null;
        }
        try {
            return (RenderType) method.invoke(type);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** Cuts one face (corners in drawing order) into pieces and keeps them, coloured by the wave. */
    private static void copyQuad(ResourceLocation texture, float[][] corners) {
        float[] a = corners[0];
        float[] b = corners[1];
        float[] d = corners[3];
        // Skip faces the wave cannot reach.
        float reach = (VrGlow.GUN_WAVE_REACH + 3.0F * VrGlow.GUN_RING_WIDTH) * scale;
        float cx = (a[0] + corners[2][0]) * 0.5F - (float) center.x;
        float cy = (a[1] + corners[2][1]) * 0.5F - (float) center.y;
        float cz = (a[2] + corners[2][2]) * 0.5F - (float) center.z;
        float half = (float) Math.sqrt(sq(corners[2][0] - a[0]) + sq(corners[2][1] - a[1]) + sq(corners[2][2] - a[2]))
                * 0.5F;
        if (Math.sqrt(cx * cx + cy * cy + cz * cz) - half > reach) {
            return;
        }
        float cell = CELL * scale;
        int cutsS = cuts(dist(a, b), cell);
        int cutsT = cuts(dist(a, d), cell);
        float[] p00 = P00;
        float[] p10 = P10;
        float[] p11 = P11;
        float[] p01 = P01;
        for (int i = 0; i < cutsS; i++) {
            for (int j = 0; j < cutsT; j++) {
                corner(corners, (float) i / cutsS, (float) j / cutsT, p00);
                corner(corners, (float) (i + 1) / cutsS, (float) j / cutsT, p10);
                corner(corners, (float) (i + 1) / cutsS, (float) (j + 1) / cutsT, p11);
                corner(corners, (float) i / cutsS, (float) (j + 1) / cutsT, p01);
                keep(texture, p00);
                keep(texture, p10);
                keep(texture, p11);
                keep(texture, p01);
            }
        }
    }

    /** The point at (s, t) of a face: s runs from corner 0 to 1 (and 3 to 2), t from corner 0 to 3. */
    private static void corner(float[][] c, float s, float t, float[] out) {
        for (int k = 0; k < 8; k++) {
            float bottom = c[0][k] + (c[1][k] - c[0][k]) * s;
            float top = c[3][k] + (c[2][k] - c[3][k]) * s;
            out[k] = bottom + (top - bottom) * t;
        }
    }

    private static void keep(ResourceLocation texture, float[] v) {
        float distance = (float) Math.sqrt(sq(v[0] - (float) center.x) + sq(v[1] - (float) center.y)
                + sq(v[2] - (float) center.z)) / scale;
        float glow = VrGlow.gunWave(distance, time) * fade * STRENGTH;
        int size = COPY_SIZES.getOrDefault(texture, 0);
        float[] data = COPIES.get(texture);
        if (data == null || data.length < size + STRIDE) {
            data = data == null ? new float[STRIDE * 4096] : Arrays.copyOf(data, data.length * 2);
            COPIES.put(texture, data);
        }
        float pull = 1.0F - DEPTH_PULL;
        data[size] = v[0] * pull;
        data[size + 1] = v[1] * pull;
        data[size + 2] = v[2] * pull;
        data[size + 3] = rgb[0] * glow;
        data[size + 4] = rgb[1] * glow;
        data[size + 5] = rgb[2] * glow;
        data[size + 6] = v[3];
        data[size + 7] = v[4];
        data[size + 8] = v[5];
        data[size + 9] = v[6];
        data[size + 10] = v[7];
        COPY_SIZES.put(texture, size + STRIDE);
    }

    private static int cuts(float length, float cell) {
        return Math.max(1, Math.min(MAX_CUTS, (int) Math.ceil(length / cell)));
    }

    private static float dist(float[] a, float[] b) {
        return (float) Math.sqrt(sq(b[0] - a[0]) + sq(b[1] - a[1]) + sq(b[2] - a[2]));
    }

    private static float sq(float x) {
        return x * x;
    }

    /** Passes the gun's vertices on unchanged and keeps each face (position, uv, normal) for its glowing copy. */
    private static final class Tee implements VertexConsumer {
        private final VertexConsumer gun;
        private final ResourceLocation texture;
        /** Per corner: x, y, z, u, v, nx, ny, nz. */
        private final float[][] corners = new float[4][8];
        private int corner;

        Tee(VertexConsumer gun, ResourceLocation texture) {
            this.gun = gun;
            this.texture = texture;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            gun.vertex(x, y, z);
            corners[corner][0] = (float) x;
            corners[corner][1] = (float) y;
            corners[corner][2] = (float) z;
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            gun.color(r, g, b, a);
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            gun.uv(u, v);
            corners[corner][3] = u;
            corners[corner][4] = v;
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            gun.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            gun.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            gun.normal(x, y, z);
            corners[corner][5] = x;
            corners[corner][6] = y;
            corners[corner][7] = z;
            return this;
        }

        @Override
        public void endVertex() {
            gun.endVertex();
            if (++corner == 4) {
                corner = 0;
                if (target != null) {
                    copyQuad(texture, corners);
                }
            }
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            gun.defaultColor(r, g, b, a);
        }

        @Override
        public void unsetDefaultColor() {
            gun.unsetDefaultColor();
        }
    }
}
