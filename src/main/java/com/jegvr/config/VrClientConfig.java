package com.jegvr.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;

/** Client options, in {@code config/jeg_vr_compat-client.toml}. */
public final class VrClientConfig {
    /** Where the off hand takes a magazine for grab-to-reload. */
    public enum GrabZone {
        /** At the hip on the off-hand side (standing play). */
        HIP,
        /** In front of the belly on the off-hand side (playing seated with tracked controllers). */
        FRONT,
        /** No grab-to-reload; the reload and aim keys still reload. */
        OFF
    }

    /** Where Vivecraft's hand or wrist HUD (hotbar) goes while the off hand holds a gun. */
    public enum HudPlacement {
        /** Where Vivecraft puts it. */
        VIVECRAFT,
        /** Slid back towards the elbow, facing as Vivecraft faces it. */
        ELBOW,
        /** Lying along the inner side of the forearm. */
        FOREARM
    }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<GrabZone> GRAB_ZONE;
    public static final ForgeConfigSpec.BooleanValue SHOW_GUIDES;
    public static final ForgeConfigSpec.BooleanValue RELOAD_WITH_BOTH_GUNS;
    public static final ForgeConfigSpec.EnumValue<HudPlacement> HUD_PLACEMENT;
    public static final ForgeConfigSpec.BooleanValue MIRROR_LEFT_HAND;
    public static final ForgeConfigSpec.BooleanValue BOW_LASER;
    public static final ForgeConfigSpec.BooleanValue GUN_HUD;
    public static final ForgeConfigSpec.BooleanValue HANDS_FOLLOW_ANIMATIONS;
    public static final ForgeConfigSpec.BooleanValue HUD_EXCLUSION;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> HUD_EXCLUDED_GUNS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("reload");
        GRAB_ZONE = builder
                .comment("Where the free hand grabs a magazine: HIP (standing), FRONT (seated), OFF.",
                        "Ignored in Vivecraft's seated mode (no tracked hands): grab-to-reload is off there.")
                .defineEnum("grabZone", GrabZone.HIP);
        SHOW_GUIDES = builder
                .comment("Show the coloured helper boxes (hip pouch, magazine well, foregrip). Off by default;",
                        "/jegvr zones toggles it.")
                .define("showGuides", false);
        RELOAD_WITH_BOTH_GUNS = builder
                .comment("Allow reloading while holding a gun in each hand. Off: put one gun away first (no free hand for",
                        "the magazine); trying to reload gives a short double buzz in that hand. Unloading still works.")
                .define("reloadWithBothGuns", true);
        builder.pop();
        builder.push("visuals");
        HUD_PLACEMENT = builder
                .comment("With a gun in the off hand, where Vivecraft's hand or wrist HUD (hotbar) goes:",
                        "VIVECRAFT (unchanged), ELBOW (slid back towards the elbow), FOREARM (along the inner forearm).")
                .defineEnum("hudPlacement", HudPlacement.ELBOW);
        MIRROR_LEFT_HAND = builder
                .comment("Draw guns held in the left controller mirrored, as if made for the left hand (JEG models are",
                        "made for the right hand). Their animation is mirrored too.")
                .define("mirrorLeftHand", true);
        BOW_LASER = builder
                .comment("Laser sight on JEG's compound bow while it is drawn: follows the arrow's predicted flight",
                        "for 32 blocks, turning transparent towards the tip.")
                .define("bowLaser", true);
        GUN_HUD = builder
                .comment("Each gun's name, ammo and meters float beside the hand that holds it, instead of JEG's overlay",
                        "on the HUD (which only shows the main-hand gun).")
                .define("gunHud", true);
        HANDS_FOLLOW_ANIMATIONS = builder
                .comment("The hands let go of the controllers and follow the gun's animations, and the controller carries",
                        "the gun with a delay during reloads and inspections. Off: the hands always stay with the",
                        "controllers and the gun follows them at once; the guns still play their animations.")
                .define("handsFollowAnimations", true);
        HUD_EXCLUSION = builder
                .comment("With a gun from hudExcludedGuns in the off hand, Vivecraft's hand or wrist HUD (hotbar) stays",
                        "where Vivecraft puts it, on the back of the hand, whatever hudPlacement says.")
                .define("hudExclusion", true);
        HUD_EXCLUDED_GUNS = builder
                .comment("The guns of hudExclusion, by item id (for example jeg:minigun).")
                .defineListAllowEmpty(List.of("hudExcludedGuns"), () -> List.of("jeg:minigun"),
                        id -> id instanceof String);
        builder.pop();
        SPEC = builder.build();
    }

    private VrClientConfig() {
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC);
    }

    /** The value, or its default while the config is not loaded yet. */
    public static GrabZone grabZone() {
        return SPEC.isLoaded() ? GRAB_ZONE.get() : GrabZone.HIP;
    }

    public static boolean showGuides() {
        return SPEC.isLoaded() && SHOW_GUIDES.get();
    }

    public static HudPlacement hudPlacement() {
        return SPEC.isLoaded() ? HUD_PLACEMENT.get() : HudPlacement.ELBOW;
    }

    public static boolean mirrorLeftHand() {
        return !SPEC.isLoaded() || MIRROR_LEFT_HAND.get();
    }

    public static boolean bowLaser() {
        return !SPEC.isLoaded() || BOW_LASER.get();
    }

    public static boolean gunHud() {
        return !SPEC.isLoaded() || GUN_HUD.get();
    }

    public static boolean handsFollowAnimations() {
        return !SPEC.isLoaded() || HANDS_FOLLOW_ANIMATIONS.get();
    }

    public static void setHandsFollowAnimations(boolean follow) {
        HANDS_FOLLOW_ANIMATIONS.set(follow);
        HANDS_FOLLOW_ANIMATIONS.save();
    }

    public static boolean hudExclusion() {
        return !SPEC.isLoaded() || HUD_EXCLUSION.get();
    }

    public static void setHudExclusion(boolean exclusion) {
        HUD_EXCLUSION.set(exclusion);
        HUD_EXCLUSION.save();
    }

    public static List<? extends String> hudExcludedGuns() {
        return SPEC.isLoaded() ? HUD_EXCLUDED_GUNS.get() : List.of("jeg:minigun");
    }

    public static void setHudExcludedGuns(List<String> guns) {
        HUD_EXCLUDED_GUNS.set(guns);
        HUD_EXCLUDED_GUNS.save();
    }

    /** The exclusion is on and this gun is listed: the HUD stays where Vivecraft puts it. */
    public static boolean hudExcluded(String gunId) {
        return hudExclusion() && hudExcludedGuns().contains(gunId);
    }

    public static void setGunHud(boolean hud) {
        GUN_HUD.set(hud);
        GUN_HUD.save();
    }

    public static void setBowLaser(boolean laser) {
        BOW_LASER.set(laser);
        BOW_LASER.save();
    }

    public static void setMirrorLeftHand(boolean mirror) {
        MIRROR_LEFT_HAND.set(mirror);
        MIRROR_LEFT_HAND.save();
    }

    public static void setHudPlacement(HudPlacement placement) {
        HUD_PLACEMENT.set(placement);
        HUD_PLACEMENT.save();
    }

    public static void setGrabZone(GrabZone zone) {
        GRAB_ZONE.set(zone);
        GRAB_ZONE.save();
    }

    public static boolean reloadWithBothGuns() {
        return !SPEC.isLoaded() || RELOAD_WITH_BOTH_GUNS.get();
    }

    public static void setReloadWithBothGuns(boolean allow) {
        RELOAD_WITH_BOTH_GUNS.set(allow);
        RELOAD_WITH_BOTH_GUNS.save();
    }

    public static void setShowGuides(boolean show) {
        SHOW_GUIDES.set(show);
        SHOW_GUIDES.save();
    }
}
