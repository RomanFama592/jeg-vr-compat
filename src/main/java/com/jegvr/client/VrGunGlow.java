package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.mixin.render.CompositeRenderTypeAccessor;
import com.jegvr.mixin.render.CompositeStateAccessor;
import com.jegvr.mixin.render.TextureStateShardAccessor;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
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
 * F10.4: while the free hand is near a two-handed gun, the gun itself glows where it can be held, in the state's colour
 * (blue: can be held, green: hand on it, violet: holding), with soft rings rippling out from there along the gun
 * ({@link VrGlow#gunWave}). Only the gun's model is touched: its faces come in the entity vertex format; the muzzle
 * flash and other sprites JEG draws with the gun use other formats and are left alone.
 * <p>
 * Every face is cut into small pieces so a ring stays smooth across long parts like the barrel. Two ways to colour them:
 * <ul>
 *   <li>Without shaders: like an enchantment glint, the pieces are copied into an additive layer
 *   ({@code RenderType.eyes} with the gun's texture, no depth writes), kept and drawn right after the gun, so the gun's
 *   own texture lights up in the colour.</li>
 *   <li>With a shader pack: packs draw such layers their own way (on the hands it did not show), so the gun's own
 *   vertices are tinted instead: their colour moves towards the glow's and their light up to full. That goes through
 *   the same program as the gun, whatever the pack.</li>
 * </ul>
 */
public final class VrGunGlow {
    /** Largest side of a piece of a face, metres in the room: the wave is computed at the pieces' corners. */
    private static final float CELL = 0.015F;
    private static final int MAX_CUTS = 16;
    /** How much colour the additive layer adds at full strength (0–1). */
    private static final float STRENGTH = 0.85F;
    /** With shaders, how far the gun's colour moves towards the glow's at full strength (0–1). */
    private static final float TINT_STRENGTH = 0.75F;
    /** The copies sit this fraction closer to the eye than the gun, so they never fight its depth. */
    private static final float DEPTH_PULL = 0.002F;
    /** Values kept per copied vertex: position, colour, uv, normal. */
    private static final int STRIDE = 11;

    // Per corner of a face: position, uv, normal, colour (0-255), overlay, light.
    private static final int X = 0;
    private static final int U = 3;
    private static final int NX = 5;
    private static final int R = 8;
    private static final int OVERLAY = 12;
    private static final int LIGHT = 14;
    private static final int VALUES = 16;
    private static final float FULL_LIGHT = 240.0F;

    /** What the gun being drawn glows with; set around {@code ItemInHandRenderer.renderItem}. */
    private static Vec3 center;
    private static float[] rgb;
    private static float fade;
    private static float time;
    private static float scale;
    /** Additive layer (no shaders) or tint (shaders), for the gun being drawn. */
    private static boolean additive;
    /** Where the additive copies go after the gun; null when no gun is glowing. */
    private static MultiBufferSource target;

    private static final Map<RenderType, ResourceLocation> TEXTURES = new IdentityHashMap<>();
    /** Each wrapper class's {@code unwrap()} (null when it has none). */
    private static final Map<Class<?>, Method> UNWRAP = new HashMap<>();
    private static final int MAX_UNWRAP = 4;
    private static final ResourceLocation NO_TEXTURE = new ResourceLocation("jeg_vr_compat", "none");
    private static final Map<ResourceLocation, float[]> COPIES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Integer> COPY_SIZES = new LinkedHashMap<>();
    // Corners of the piece being cut (render thread only).
    private static final float[] P00 = new float[VALUES];
    private static final float[] P10 = new float[VALUES];
    private static final float[] P11 = new float[VALUES];
    private static final float[] P01 = new float[VALUES];

    private VrGunGlow() {
    }

    /**
     * Called with the buffers of the item Vivecraft is about to draw in {@code drawnHand}: returns them wrapped when it
     * is a gun that should glow, unchanged otherwise.
     */
    public static MultiBufferSource wrap(MultiBufferSource buffers, InteractionHand drawnHand, ItemStack stack) {
        target = null;
        Minecraft mc = Minecraft.getInstance();
        // Only the gun itself: whatever else is drawn afterwards still carries the last gun's hand.
        boolean isThatGun = drawnHand != null && mc.player != null && !stack.isEmpty()
                && ItemStack.isSameItem(stack, mc.player.getItemInHand(drawnHand));
        VrPrecision.Glow glow = !isThatGun || !VR.isEyePass() ? null : VrPrecision.glow(drawnHand);
        if (glow == null) {
            return buffers;
        }
        // Vivecraft draws the hands in world axes, relative to the eye.
        center = glow.center().subtract(mc.gameRenderer.getMainCamera().getPosition());
        rgb = glow.rgb();
        fade = glow.fade();
        scale = VR.worldScale();
        time = mc.player.tickCount + mc.getFrameTime();
        additive = !VR.shadersActive();
        VrDiag.once("gunGlow" + additive, "Gun glow: {} (buffers {})",
                additive ? "additive layer" : "tint (shader pack)", buffers.getClass().getName());
        target = additive ? buffers : null;
        COPY_SIZES.replaceAll((texture, size) -> 0);
        return type -> {
            VertexConsumer gun = buffers.getBuffer(type);
            if (type.mode() != VertexFormat.Mode.QUADS || type.format() != DefaultVertexFormat.NEW_ENTITY) {
                return gun;
            }
            ResourceLocation texture = additive ? texture(type) : null;
            if (additive && texture == null) {
                return gun;
            }
            return new Tee(gun, texture, additive);
        };
    }

    /** After the gun is drawn: draws its glowing copies over it (additive layer only). */
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
     * mods may wrap render types: those are unwrapped by their {@code unwrap()} method, found by reflection so this
     * addon never imports them. Only Minecraft's own types are remembered; wrappers may be new objects every frame.
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
            ResourceLocation found = key.toString().contains("glint") ? NO_TEXTURE
                    : ((TextureStateShardAccessor) ((CompositeStateAccessor) (Object) composite.jegvr$state())
                    .jegvr$textureState()).jegvr$cutoutTexture().orElse(NO_TEXTURE);
            VrDiag.once("gunGlowType" + key, "Gun glow: render type {} -> texture {}", key, found);
            return found;
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

    /** Whether the wave can reach any part of this face (its centre and half its diagonal). */
    private static boolean reachable(float[][] corners) {
        float reach = (VrGlow.GUN_WAVE_REACH + 3.0F * VrGlow.GUN_RING_WIDTH) * scale;
        float[] a = corners[0];
        float[] c = corners[2];
        float cx = (a[X] + c[X]) * 0.5F - (float) center.x;
        float cy = (a[X + 1] + c[X + 1]) * 0.5F - (float) center.y;
        float cz = (a[X + 2] + c[X + 2]) * 0.5F - (float) center.z;
        float half = (float) Math.sqrt(sq(c[X] - a[X]) + sq(c[X + 1] - a[X + 1]) + sq(c[X + 2] - a[X + 2])) * 0.5F;
        return Math.sqrt(cx * cx + cy * cy + cz * cz) - half <= reach;
    }

    /**
     * Cuts a face (corners in drawing order) into pieces, in the same winding, and hands each piece's corners to
     * {@code sink}: kept for the additive layer, or written tinted to the gun.
     */
    private static void cut(float[][] corners, PieceSink sink) {
        float cell = CELL * scale;
        int cutsS = cuts(dist(corners[0], corners[1]), cell);
        int cutsT = cuts(dist(corners[0], corners[3]), cell);
        for (int i = 0; i < cutsS; i++) {
            for (int j = 0; j < cutsT; j++) {
                corner(corners, (float) i / cutsS, (float) j / cutsT, P00);
                corner(corners, (float) (i + 1) / cutsS, (float) j / cutsT, P10);
                corner(corners, (float) (i + 1) / cutsS, (float) (j + 1) / cutsT, P11);
                corner(corners, (float) i / cutsS, (float) (j + 1) / cutsT, P01);
                sink.accept(P00);
                sink.accept(P10);
                sink.accept(P11);
                sink.accept(P01);
            }
        }
    }

    private interface PieceSink {
        void accept(float[] vertex);
    }

    /** The point at (s, t) of a face: s runs from corner 0 to 1 (and 3 to 2), t from corner 0 to 3. */
    private static void corner(float[][] c, float s, float t, float[] out) {
        for (int k = 0; k < VALUES; k++) {
            float bottom = c[0][k] + (c[1][k] - c[0][k]) * s;
            float top = c[3][k] + (c[2][k] - c[3][k]) * s;
            out[k] = bottom + (top - bottom) * t;
        }
    }

    /** The wave's strength (0–1) at this vertex, with the fade-in. */
    private static float glowAt(float[] v) {
        float distance = (float) Math.sqrt(sq(v[X] - (float) center.x) + sq(v[X + 1] - (float) center.y)
                + sq(v[X + 2] - (float) center.z)) / scale;
        return VrGlow.gunWave(distance, time) * fade;
    }

    private static void keep(ResourceLocation texture, float[] v) {
        float glow = glowAt(v) * STRENGTH;
        int size = COPY_SIZES.getOrDefault(texture, 0);
        float[] data = COPIES.get(texture);
        if (data == null || data.length < size + STRIDE) {
            data = data == null ? new float[STRIDE * 4096] : Arrays.copyOf(data, data.length * 2);
            COPIES.put(texture, data);
        }
        float pull = 1.0F - DEPTH_PULL;
        data[size] = v[X] * pull;
        data[size + 1] = v[X + 1] * pull;
        data[size + 2] = v[X + 2] * pull;
        data[size + 3] = rgb[0] * glow;
        data[size + 4] = rgb[1] * glow;
        data[size + 5] = rgb[2] * glow;
        data[size + 6] = v[U];
        data[size + 7] = v[U + 1];
        data[size + 8] = v[NX];
        data[size + 9] = v[NX + 1];
        data[size + 10] = v[NX + 2];
        COPY_SIZES.put(texture, size + STRIDE);
    }

    /** Writes one vertex of the gun, its colour moved towards the glow's and its light raised where the wave is. */
    private static void writeTinted(VertexConsumer gun, float[] v) {
        float glow = glowAt(v) * TINT_STRENGTH;
        int light = Math.round(FULL_LIGHT * glow);
        gun.vertex(v[X], v[X + 1], v[X + 2])
                .color(tint(v[R], rgb[0], glow), tint(v[R + 1], rgb[1], glow), tint(v[R + 2], rgb[2], glow),
                        Math.round(v[R + 3]))
                .uv(v[U], v[U + 1])
                .overlayCoords(Math.round(v[OVERLAY]), Math.round(v[OVERLAY + 1]))
                .uv2(Math.max(Math.round(v[LIGHT]), light), Math.max(Math.round(v[LIGHT + 1]), light))
                .normal(v[NX], v[NX + 1], v[NX + 2])
                .endVertex();
    }

    private static int tint(float channel, float glowChannel, float glow) {
        return Math.min(255, Math.round(channel * (1.0F - glow) + 255.0F * glowChannel * glow));
    }

    private static int cuts(float length, float cell) {
        return Math.max(1, Math.min(MAX_CUTS, (int) Math.ceil(length / cell)));
    }

    private static float dist(float[] a, float[] b) {
        return (float) Math.sqrt(sq(b[X] - a[X]) + sq(b[X + 1] - a[X + 1]) + sq(b[X + 2] - a[X + 2]));
    }

    private static float sq(float x) {
        return x * x;
    }

    /**
     * Gathers each face of the gun. Additive: passes the gun's vertices on unchanged and keeps the face's pieces for
     * the layer. Tint: holds the face back and writes its pieces tinted instead (or the face as it was, when the wave
     * cannot reach it). Vertices arrive as position, colour, uv, overlay, light, normal (the entity format).
     */
    private static final class Tee implements VertexConsumer {
        private final VertexConsumer gun;
        private final ResourceLocation texture;
        private final boolean passThrough;
        private final float[][] corners = new float[4][VALUES];
        private int corner;

        Tee(VertexConsumer gun, ResourceLocation texture, boolean passThrough) {
            this.gun = gun;
            this.texture = texture;
            this.passThrough = passThrough;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            if (passThrough) {
                gun.vertex(x, y, z);
            }
            float[] c = corners[corner];
            c[X] = (float) x;
            c[X + 1] = (float) y;
            c[X + 2] = (float) z;
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            if (passThrough) {
                gun.color(r, g, b, a);
            }
            float[] c = corners[corner];
            c[R] = r;
            c[R + 1] = g;
            c[R + 2] = b;
            c[R + 3] = a;
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            if (passThrough) {
                gun.uv(u, v);
            }
            corners[corner][U] = u;
            corners[corner][U + 1] = v;
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            if (passThrough) {
                gun.overlayCoords(u, v);
            }
            corners[corner][OVERLAY] = u;
            corners[corner][OVERLAY + 1] = v;
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            if (passThrough) {
                gun.uv2(u, v);
            }
            corners[corner][LIGHT] = u;
            corners[corner][LIGHT + 1] = v;
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            if (passThrough) {
                gun.normal(x, y, z);
            }
            float[] c = corners[corner];
            c[NX] = x;
            c[NX + 1] = y;
            c[NX + 2] = z;
            return this;
        }

        @Override
        public void endVertex() {
            if (passThrough) {
                gun.endVertex();
            }
            if (++corner < 4) {
                return;
            }
            corner = 0;
            boolean reach = center != null && reachable(corners);
            if (passThrough) {
                if (reach && target != null) {
                    cut(corners, v -> keep(texture, v));
                }
            } else if (reach) {
                cut(corners, v -> writeTinted(gun, v));
            } else {
                for (float[] v : corners) {
                    writeUnchanged(v);
                }
            }
        }

        private void writeUnchanged(float[] v) {
            gun.vertex(v[X], v[X + 1], v[X + 2])
                    .color(Math.round(v[R]), Math.round(v[R + 1]), Math.round(v[R + 2]), Math.round(v[R + 3]))
                    .uv(v[U], v[U + 1])
                    .overlayCoords(Math.round(v[OVERLAY]), Math.round(v[OVERLAY + 1]))
                    .uv2(Math.round(v[LIGHT]), Math.round(v[LIGHT + 1]))
                    .normal(v[NX], v[NX + 1], v[NX + 2])
                    .endVertex();
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
