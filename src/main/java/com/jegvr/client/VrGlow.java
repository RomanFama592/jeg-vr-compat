package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.config.VrClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * F10.1: a soft glow facing the headset with rings rippling out of it, marking where a hand can do something
 * (grab ammo at the hip, put the magazine in the gun). The textures are white ({@code tools/make_glow_textures.py});
 * the vertex colour gives the state. Drawn without writing depth, so it never hides the gun or the hand.
 */
public final class VrGlow {
    private static final ResourceLocation GLOW = new ResourceLocation("jeg_vr_compat", "textures/misc/grab_glow.png");
    private static final ResourceLocation RING = new ResourceLocation("jeg_vr_compat", "textures/misc/grab_ring.png");
    /** Ticks for one ring to grow from the centre to the edge (about 1.2 s). */
    static final float PERIOD_TICKS = 24.0F;
    /** Rings out of phase with each other, so one is always on its way. */
    static final int RINGS = 2;
    private static final float GLOW_ALPHA = 0.7F;
    private static final float RING_ALPHA = 0.9F;

    /** Colours of the states (as the guide boxes of F7.5). */
    public static final float[] BLUE = {0.25F, 0.55F, 1.0F};
    public static final float[] GREEN = {0.2F, 1.0F, 0.3F};
    public static final float[] WHITE = {1.0F, 1.0F, 1.0F};
    public static final float[] VIOLET = {0.8F, 0.3F, 1.0F};

    /** F10.4, the wave over the gun: how far a ring travels from where the hand holds, and how soft it is (metres). */
    static final float GUN_WAVE_REACH = 0.3F;
    static final float GUN_RING_WIDTH = 0.045F;
    /** The steady glow right where the hand holds, and its strength. */
    static final float GUN_CORE = 0.05F;
    static final float GUN_CORE_STRENGTH = 0.5F;

    private VrGlow() {
    }

    /** The option is on (it only matters in VR, where the callers already are). */
    public static boolean enabled() {
        return VrClientConfig.showSprites();
    }

    /**
     * Draws a glow of this radius (metres in the room; scaled with Vivecraft's world scale) at {@code center}. The pose
     * is in world coordinates already moved by -camera. Call {@code buffers.endBatch()} afterwards (rule 9).
     */
    public static void draw(PoseStack pose, MultiBufferSource buffers, Vec3 center, float radius, float[] rgb,
                            float time) {
        VR.Aim hmd = VR.hmdAim();
        Vec3 hmdUp = VR.hmdUp();
        if (center == null || hmd == null || hmdUp == null || hmd.pos().distanceToSqr(center) < 1.0E-6) {
            return;
        }
        float size = radius * VR.worldScale();
        pose.pushPose();
        pose.translate(center.x, center.y, center.z);
        pose.mulPose(VrGunHud.facing(hmd.pos().subtract(center), hmdUp));
        quad(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(GLOW)), size * 0.6F, rgb, GLOW_ALPHA);
        VertexConsumer rings = buffers.getBuffer(RenderType.entityTranslucentEmissive(RING));
        for (int i = 0; i < RINGS; i++) {
            float phase = ringPhase(time, i);
            quad(pose, rings, size * ringScale(phase), rgb, RING_ALPHA * ringAlpha(phase));
        }
        pose.popPose();
    }

    /** How far ring {@code index} is through its growth (0 at the centre, 1 at the edge), at this time in ticks. */
    static float ringPhase(float time, int index) {
        float t = time / PERIOD_TICKS + (float) index / RINGS;
        return t - (float) Math.floor(t);
    }

    /**
     * F10.4: how strongly a point of the gun this far from the held point (metres in the room) glows, 0–1, at this
     * time in ticks: a steady core plus soft rings travelling outward (wider and more diffuse than the sprite's).
     */
    static float gunWave(float distance, float time) {
        float glow = GUN_CORE_STRENGTH * gauss(distance / GUN_CORE);
        for (int i = 0; i < RINGS; i++) {
            float phase = ringPhase(time, i);
            glow += ringAlpha(phase) * gauss((distance - phase * GUN_WAVE_REACH) / GUN_RING_WIDTH);
        }
        return Math.min(glow, 1.0F);
    }

    private static float gauss(float x) {
        return (float) Math.exp(-x * x);
    }

    /** A ring's size along its growth: from a dot to the full radius, fast at first. */
    static float ringScale(float phase) {
        return 0.15F + 0.85F * (float) Math.sqrt(phase);
    }

    /** A ring's opacity along its growth: fades in quickly, then out towards the edge. */
    static float ringAlpha(float phase) {
        float in = Math.min(phase / 0.15F, 1.0F);
        return in * (1.0F - phase);
    }

    private static void quad(PoseStack pose, VertexConsumer consumer, float half, float[] rgb, float alpha) {
        if (alpha <= 0.0F || half <= 0.0F) {
            return;
        }
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        vertex(consumer, matrix, normal, -half, -half, 0.0F, 1.0F, rgb, alpha);
        vertex(consumer, matrix, normal, half, -half, 1.0F, 1.0F, rgb, alpha);
        vertex(consumer, matrix, normal, half, half, 1.0F, 0.0F, rgb, alpha);
        vertex(consumer, matrix, normal, -half, half, 0.0F, 0.0F, rgb, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y, float u,
                               float v, float[] rgb, float alpha) {
        consumer.vertex(matrix, x, y, 0.0F).color(rgb[0], rgb[1], rgb[2], alpha).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
    }
}
