package com.jegvr.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionsTest {
    @Test
    void vivecraftLineComesFromItsOwnVersionNotTheGames() {
        // The mod version carries the game's first: 1.20.1 must not be read as Vivecraft 1.20.
        assertEquals(Versions.Vivecraft.V1_2, Versions.vivecraftLine("1.20.1-1.2.5"));
        assertEquals(Versions.Vivecraft.V1_2, Versions.vivecraftLine("1.20.1-1.2.0"));
        assertEquals(Versions.Vivecraft.V1_3, Versions.vivecraftLine("1.20.1-1.3.0"));
        assertEquals(Versions.Vivecraft.V1_3, Versions.vivecraftLine("1.20.1-1.3.15"));
        // As the files are named, with the loader at the end.
        assertEquals(Versions.Vivecraft.V1_2, Versions.vivecraftLine("1.20.1-1.2.5-forge"));
        assertEquals(Versions.Vivecraft.V1_3, Versions.vivecraftLine("1.20.1-1.3.15-forge"));
    }

    @Test
    void unknownVivecraftCountsAsTheNewestLine() {
        assertEquals(Versions.Vivecraft.V1_3, Versions.vivecraftLine("1.20.1-1.4.0"));
        assertEquals(Versions.Vivecraft.V1_3, Versions.vivecraftLine("1.20.1-2.0.1"));
        assertEquals(Versions.Vivecraft.V1_3, Versions.vivecraftLine("unknown"));
        assertEquals(Versions.Vivecraft.V1_3, Versions.vivecraftLine(null));
    }

    @Test
    void eachLineNamesItsBridgeAndOnlyNoneHasNone() {
        assertTrue(Versions.Vivecraft.V1_2.bridgeClass().endsWith("Vivecraft12Bridge"));
        assertTrue(Versions.Vivecraft.V1_3.bridgeClass().endsWith("Vivecraft13Bridge"));
        assertEquals(null, Versions.Vivecraft.NONE.bridgeClass());
    }

    @Test
    void atLeastComparesMajorThenMinor() {
        assertFalse(Versions.atLeast("0.13.2", 0, 14));
        assertTrue(Versions.atLeast("0.14.0", 0, 14));
        assertTrue(Versions.atLeast("0.14.5", 0, 14));
        assertTrue(Versions.atLeast("0.15.0", 0, 14));
        assertTrue(Versions.atLeast("1.0.0", 0, 14));
        assertFalse(Versions.atLeast("0.9.9", 0, 14));
    }

    @Test
    void aVersionThatCannotBeReadCountsAsNewer() {
        assertTrue(Versions.atLeast("unknown", 0, 14));
        assertTrue(Versions.atLeast(null, 0, 14));
    }
}
