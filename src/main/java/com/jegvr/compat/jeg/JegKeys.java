package com.jegvr.compat.jeg;

/**
 * JEG's private conventions the addon relies on, in one place: when JEG changes one, only this class changes. The names
 * are checked against JEG's jar with {@code javap}; JEG keeps them as string literals.
 */
public final class JegKeys {
    /** Boolean and number tags of a gun's stack. */
    public static final String AMMO_COUNT = "AmmoCount";
    public static final String IGNORE_AMMO = "IgnoreAmmo";
    public static final String IS_RELOADING = "IsReloading";
    public static final String IS_SHOOTING = "IsShooting";
    public static final String IS_INSPECTING = "IsInspecting";
    public static final String IS_DRAWING = "IsDrawing";
    public static final String ANIMATION_TICK = "AnimationTick";
    public static final String CUSTOM_MODIFIER = "CustomModifier";
    public static final String GUN_ID = "GunId";
    /** In the player's persistent data, not on the gun: JEG keeps one charge per player. */
    public static final String CHARGE_PROGRESS = "ChargeProgress";
    /** Length of JEG's main-hand shoot animation ({@code AnimatedGunItem.inventoryTick}). */
    public static final int SHOOT_ANIMATION_TICKS = 5;

    private JegKeys() {
    }
}
