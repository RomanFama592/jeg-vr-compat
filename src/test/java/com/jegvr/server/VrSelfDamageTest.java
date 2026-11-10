package com.jegvr.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** F9.8: how much a bullet hurts the player who fired it. */
class VrSelfDamageTest {
    @Test
    void bodyShotKeepsJegDamage() {
        assertEquals(7.5F, VrSelfDamage.damage(7.5F, false));
        assertEquals(0.0F, VrSelfDamage.damage(0.0F, false));
    }

    @Test
    void headshotIsLethal() {
        assertEquals(VrSelfDamage.LETHAL_DAMAGE, VrSelfDamage.damage(7.5F, true));
        assertTrue(VrSelfDamage.damage(7.5F, true) >= 1000.0F);
        // A gun that already does more keeps its damage.
        assertEquals(5000.0F, VrSelfDamage.damage(5000.0F, true));
    }
}
