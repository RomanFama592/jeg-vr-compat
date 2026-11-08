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

    /** The GeckoLib controller JEG gives every animated gun ({@code GunAnimations.animationController}). */
    public static final String CONTROLLER = "Controller";
    /** JEG's inspect animation, in that controller. */
    public static final String INSPECT_ANIMATION = "inspect";
    /**
     * Arm bones of JEG's gun models: {@code right_arm}/{@code left_arm}, and {@code fake_*} copies some guns use while
     * an arm leaves the gun. The right arm holds the grip.
     */
    public static final String RIGHT_ARM_BONE = "right_arm";
    public static final String FAKE_BONE_PREFIX = "fake_";
    /**
     * JEG's third-person gun renderer, merged into {@code ItemInHandLayer} by JEG's mixin and private there: the addon
     * reaches it by reflection ({@code ItemInHandLayerVRMixin}).
     */
    public static final String RENDER_ARM_WITH_GUN = "mrCrayfishGunMod$renderArmWithGun";

    private JegKeys() {
    }
}
