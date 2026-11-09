package com.jegvr.client;

/**
 * F9.27: the arithmetic of JEG's blinded and smoked overlays, without game state (tested). JEG computes it in
 * {@code GameRendererMixin.updateCameraAndRender}; see {@link com.jegvr.compat.jeg.JegKeys#BLINDED_OVERLAY_RGB}.
 */
public final class VrBlindFade {
    private VrBlindFade() {
    }

    /**
     * The overlay's alpha (0–255) with this many ticks of the effect left: full {@code maxAlpha} until the last
     * {@code fadeTicks}, then fading linearly to 0. Same float math and rounding as JEG.
     */
    public static int alpha(int ticksLeft, int fadeTicks, int maxAlpha) {
        float progress = Math.min((float) ticksLeft / (float) fadeTicks, 1.0F);
        int alpha = (int) ((double) (progress * (float) maxAlpha) + 0.5);
        return Math.max(0, Math.min(255, alpha));
    }

    /** {@code rgb} with this alpha, as ARGB. */
    public static int argb(int rgb, int alpha) {
        return alpha << 24 | (rgb & 0xFFFFFF);
    }

    /**
     * Whether a {@code GuiGraphics.fill(x1, y1, x2, y2, color)} call is one of JEG's overlays: the whole window from
     * (0, 0), in one of its two colours.
     */
    public static boolean isOverlayFill(int x1, int y1, int x2, int y2, int color, int windowWidth, int windowHeight,
                                        int blindRgb, int smokeRgb) {
        if (x1 != 0 || y1 != 0 || x2 != windowWidth || y2 != windowHeight) {
            return false;
        }
        int rgb = color & 0xFFFFFF;
        return rgb == blindRgb || rgb == smokeRgb;
    }
}
