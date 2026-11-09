package com.jegvr.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S.8: what the server decides and what stays with the player. */
class VrRulesTest {
    @Test
    void playerChoiceKeepsTheOwnOption() {
        assertTrue(VrRules.resolve(VrRules.Toggle.PLAYER, true));
        assertFalse(VrRules.resolve(VrRules.Toggle.PLAYER, false));
        assertEquals("GESTURE", VrRules.resolve(VrRules.Throw.PLAYER, "GESTURE"));
        assertFalse(VrRules.Rules.PLAYER_CHOICE.decidesAny());
    }

    @Test
    void theServerOverridesTheOwnOption() {
        assertTrue(VrRules.resolve(VrRules.Toggle.ALLOW, false));
        assertFalse(VrRules.resolve(VrRules.Toggle.DENY, true));
        assertEquals("TRIGGER", VrRules.resolve(VrRules.Throw.TRIGGER, "GESTURE"));
        assertTrue(new VrRules.Rules(VrRules.Toggle.PLAYER, VrRules.Throw.PLAYER, VrRules.Toggle.DENY).decidesAny());
    }

    @Test
    void throwNamesMatchTheClientOption() {
        for (VrClientConfig.ThrowMode mode : VrClientConfig.ThrowMode.values()) {
            assertEquals(mode, VrClientConfig.ThrowMode.valueOf(VrRules.Throw.valueOf(mode.name()).name()));
        }
        assertEquals(VrClientConfig.ThrowMode.values().length + 1, VrRules.Throw.values().length);
    }

    @Test
    void serverEnforcement() {
        VrRules.Rules deny = new VrRules.Rules(VrRules.Toggle.DENY, VrRules.Throw.TRIGGER, VrRules.Toggle.PLAYER);
        assertTrue(VrRules.deniesBothGunReload(deny));
        assertFalse(VrRules.deniesBothGunReload(VrRules.Rules.PLAYER_CHOICE));
        assertFalse(VrRules.acceptsGestureSpeed(deny));
        assertTrue(VrRules.acceptsGestureSpeed(VrRules.Rules.PLAYER_CHOICE));
        assertTrue(VrRules.aimsThrowFromHand(deny));
        assertFalse(VrRules.aimsThrowFromHand(
                new VrRules.Rules(VrRules.Toggle.PLAYER, VrRules.Throw.VIVECRAFT, VrRules.Toggle.PLAYER)));
    }
}
