package com.jegvr.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jegvr.compat.jeg.JegKeys;
import org.junit.jupiter.api.Test;

class VrBlindFadeTest {
    private static final int FADE = JegKeys.DEFAULT_ALPHA_FADE_THRESHOLD;

    @Test
    void fullAlphaUntilTheFadeStarts() {
        assertEquals(255, VrBlindFade.alpha(200, FADE, 255));
        assertEquals(255, VrBlindFade.alpha(FADE, FADE, 255));
        assertEquals(248, VrBlindFade.alpha(100, FADE, JegKeys.SMOKED_OVERLAY_ALPHA));
    }

    @Test
    void fadesLinearlyLikeJeg() {
        assertEquals(128, VrBlindFade.alpha(20, FADE, 255)); // 127.5 rounds up, as in JEG
        assertEquals(6, VrBlindFade.alpha(1, FADE, 255));
        assertEquals(0, VrBlindFade.alpha(0, FADE, 255));
    }

    @Test
    void zeroFadeTicksMeansNoFade() {
        assertEquals(255, VrBlindFade.alpha(5, 0, 255));
        assertEquals(0, VrBlindFade.alpha(0, 0, 255));
    }

    @Test
    void argbKeepsTheColour() {
        assertEquals(0x80FFFFFF, VrBlindFade.argb(JegKeys.BLINDED_OVERLAY_RGB, 0x80));
        assertEquals(0xF87C7670, VrBlindFade.argb(JegKeys.SMOKED_OVERLAY_RGB, 248));
    }

    @Test
    void recognisesOnlyJegsWholeWindowFills() {
        int blind = JegKeys.BLINDED_OVERLAY_RGB;
        int smoke = JegKeys.SMOKED_OVERLAY_RGB;
        assertTrue(VrBlindFade.isOverlayFill(0, 0, 1920, 1080, 0x40FFFFFF, 1920, 1080, blind, smoke));
        assertTrue(VrBlindFade.isOverlayFill(0, 0, 1920, 1080, 0xF87C7670, 1920, 1080, blind, smoke));
        assertFalse(VrBlindFade.isOverlayFill(0, 0, 1920, 1080, 0x80000000, 1920, 1080, blind, smoke));
        assertFalse(VrBlindFade.isOverlayFill(0, 0, 960, 540, 0x40FFFFFF, 1920, 1080, blind, smoke));
        assertFalse(VrBlindFade.isOverlayFill(2, 2, 1920, 1080, 0x40FFFFFF, 1920, 1080, blind, smoke));
    }
}
