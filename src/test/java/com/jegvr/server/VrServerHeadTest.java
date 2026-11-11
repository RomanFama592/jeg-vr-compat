package com.jegvr.server;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VrServerHeadTest {
    @Test
    void onlyARecentRecordCounts() {
        assertTrue(VrServerHead.fresh(100, 100));
        assertTrue(VrServerHead.fresh(100, 100 + VrServerHead.MAX_AGE_TICKS));
        assertFalse(VrServerHead.fresh(100, 101 + VrServerHead.MAX_AGE_TICKS));
        assertFalse(VrServerHead.fresh(100, 99));
    }

    @Test
    void theLookIsStoredAsAUnitVector() {
        assertEquals(1.0, VrServerHead.unit(new Vec3(0, 0, -7)).length(), 1.0E-9);
        assertNull(VrServerHead.unit(Vec3.ZERO));
        assertNull(VrServerHead.unit(new Vec3(Double.NaN, 0, 1)));
        assertNull(VrServerHead.unit(new Vec3(Double.POSITIVE_INFINITY, 0, 1)));
    }
}
