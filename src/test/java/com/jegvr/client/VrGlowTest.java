package com.jegvr.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VrGlowTest {
    private static final float EPS = 1.0E-5F;

    @Test
    void ringsAreOutOfPhaseAndRepeat() {
        assertEquals(0.0F, VrGlow.ringPhase(0.0F, 0), EPS);
        assertEquals(0.5F, VrGlow.ringPhase(0.0F, 1), EPS);
        assertEquals(0.25F, VrGlow.ringPhase(VrGlow.PERIOD_TICKS / 4, 0), EPS);
        assertEquals(VrGlow.ringPhase(3.0F, 0), VrGlow.ringPhase(3.0F + VrGlow.PERIOD_TICKS * 5, 0), EPS);
    }

    @Test
    void ringsGrowFromTheCentreToTheEdge() {
        assertTrue(VrGlow.ringScale(0.0F) < 0.2F);
        assertEquals(1.0F, VrGlow.ringScale(1.0F), EPS);
        for (float p = 0.0F; p < 0.95F; p += 0.05F) {
            assertTrue(VrGlow.ringScale(p + 0.05F) > VrGlow.ringScale(p));
        }
    }

    @Test
    void ringsFadeInThenOut() {
        assertEquals(0.0F, VrGlow.ringAlpha(0.0F), EPS);
        assertEquals(0.0F, VrGlow.ringAlpha(1.0F), EPS);
        assertTrue(VrGlow.ringAlpha(0.15F) > 0.8F);
        assertTrue(VrGlow.ringAlpha(0.9F) < VrGlow.ringAlpha(0.5F));
    }
}
