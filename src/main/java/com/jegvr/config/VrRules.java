package com.jegvr.config;

/**
 * S.8: the options a server with this addon may decide for its players instead of each player. {@code PLAYER} leaves the
 * choice to the player's own option (the default, and the only value without the addon on the server). No game state,
 * so it can be tested; {@code VrServerConfig} reads it from the server file and {@code S2CVrRules} carries it.
 */
public final class VrRules {
    /** An on/off option the server may force. */
    public enum Toggle {
        PLAYER, ALLOW, DENY
    }

    /** {@code grenades.throwMode}, or the player's own. The names match {@link VrClientConfig.ThrowMode}. */
    public enum Throw {
        PLAYER, TRIGGER, GESTURE, VIVECRAFT
    }

    /** What a server decides. A new rule goes in a new packet (packets never change format), not in this record's wire form. */
    public record Rules(Toggle reloadWithBothGuns, Throw throwMode, Toggle bowLaser) {
        /** Every option left to the player: no server, a JEG-only server, or a server that does not set them. */
        public static final Rules PLAYER_CHOICE = new Rules(Toggle.PLAYER, Throw.PLAYER, Toggle.PLAYER);

        /** True when the server decides at least one option. */
        public boolean decidesAny() {
            return reloadWithBothGuns != Toggle.PLAYER || throwMode != Throw.PLAYER || bowLaser != Toggle.PLAYER;
        }
    }

    private VrRules() {
    }

    /** The option in effect: the server's when it decides, the player's own otherwise. */
    public static boolean resolve(Toggle rule, boolean own) {
        return switch (rule) {
            case ALLOW -> true;
            case DENY -> false;
            case PLAYER -> own;
        };
    }

    /** The throw mode's name in effect: the server's when it decides, the player's own otherwise. */
    public static String resolve(Throw rule, String own) {
        return rule == Throw.PLAYER ? own : rule.name();
    }

    /** The server forbids reloading with a gun in each hand (only {@code DENY} is enforced by the server). */
    public static boolean deniesBothGunReload(Rules rules) {
        return rules.reloadWithBothGuns() == Toggle.DENY;
    }

    /** The server accepts the strength of a throwing gesture: the throw mode is the player's, or the gesture. */
    public static boolean acceptsGestureSpeed(Rules rules) {
        return rules.throwMode() == Throw.PLAYER || rules.throwMode() == Throw.GESTURE;
    }

    /** The server aims grenades from the hand: anything but Vivecraft's own bow-like throw. */
    public static boolean aimsThrowFromHand(Rules rules) {
        return rules.throwMode() != Throw.VIVECRAFT;
    }
}
