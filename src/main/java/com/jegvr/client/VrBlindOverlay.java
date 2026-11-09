package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.compat.jeg.JegKeys;
import com.jegvr.config.VrClientConfig;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.joml.Matrix4f;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.init.ModEffects;

/**
 * F9.27: JEG whitens (stun grenade) or greys (smoke) the screen with a full-window fill on the GUI, which in VR is
 * only Vivecraft's HUD panel. In VR the same colour covers each eye instead, after Vivecraft's own post-processing
 * ({@code ShaderHelperVRMixin}), and JEG's fill on the HUD is skipped ({@code GuiGraphicsVRMixin}).
 */
public final class VrBlindOverlay {
    private VrBlindOverlay() {
    }

    /** In VR with the option on: the eyes get the overlay and JEG's HUD fill is skipped. */
    public static boolean inEyes() {
        return VR.active() && VrClientConfig.eyeFlash() != VrClientConfig.EyeFlash.OFF;
    }

    /** Draws the overlays over the eye's framebuffer, which Vivecraft has bound. */
    public static void drawEye() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !inEyes()) {
            return;
        }
        int fade = fadeTicks();
        int blind = alpha(player, ModEffects.BLINDED.get(), fade, maxAlpha());
        int smoke = alpha(player, ModEffects.SMOKED.get(), fade, JegKeys.SMOKED_OVERLAY_ALPHA);
        if (blind <= 0 && smoke <= 0) {
            return;
        }
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();

        // Same order as JEG: the flash below, the smoke over it. Corners in clip space: the whole framebuffer.
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        if (blind > 0) {
            quad(buffer, VrBlindFade.argb(VrClientConfig.eyeFlash().rgb(), blind));
        }
        if (smoke > 0) {
            quad(buffer, VrBlindFade.argb(JegKeys.SMOKED_OVERLAY_RGB, smoke));
        }
        BufferUploader.drawWithShader(buffer.end());

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
    }

    /** Whether this {@code GuiGraphics.fill} is JEG's overlay, which the eyes already show. Called for every fill. */
    public static boolean skipHudFill(int x1, int y1, int x2, int y2, int color) {
        if (x1 != 0 || y1 != 0) {
            return false;
        }
        Window window = Minecraft.getInstance().getWindow();
        return VrBlindFade.isOverlayFill(x1, y1, x2, y2, color, window.getScreenWidth(), window.getScreenHeight(),
                JegKeys.BLINDED_OVERLAY_RGB, JegKeys.SMOKED_OVERLAY_RGB)
                && inEyes() && hasOverlayEffect(Minecraft.getInstance().player);
    }

    private static boolean hasOverlayEffect(LocalPlayer player) {
        return player != null
                && (player.hasEffect(ModEffects.BLINDED.get()) || player.hasEffect(ModEffects.SMOKED.get()));
    }

    private static int alpha(LocalPlayer player, MobEffect effect, int fade, int max) {
        MobEffectInstance instance = player.getEffect(effect);
        return instance == null ? 0 : VrBlindFade.alpha(instance.getDuration(), fade, max);
    }

    private static void quad(BufferBuilder buffer, int argb) {
        int a = argb >>> 24;
        int r = argb >> 16 & 0xFF;
        int g = argb >> 8 & 0xFF;
        int b = argb & 0xFF;
        buffer.vertex(-1, -1, 0).color(r, g, b, a).endVertex();
        buffer.vertex(1, -1, 0).color(r, g, b, a).endVertex();
        buffer.vertex(1, 1, 0).color(r, g, b, a).endVertex();
        buffer.vertex(-1, 1, 0).color(r, g, b, a).endVertex();
    }

    // JEG's server config, synced to the client; the defaults while it is not loaded (or if JEG renames it).
    private static int maxAlpha() {
        try {
            return Config.SERVER.alphaOverlay.get();
        } catch (Throwable t) {
            return JegKeys.DEFAULT_ALPHA_OVERLAY;
        }
    }

    private static int fadeTicks() {
        try {
            return Config.SERVER.alphaFadeThreshold.get();
        } catch (Throwable t) {
            return JegKeys.DEFAULT_ALPHA_FADE_THRESHOLD;
        }
    }
}
