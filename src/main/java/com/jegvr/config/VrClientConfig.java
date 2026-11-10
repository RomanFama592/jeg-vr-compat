package com.jegvr.config;

import com.jegvr.client.VrRulesClient;
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

    /**
     * F9.27: the colour of the stun grenade's flash in each eye, or {@link #OFF} for JEG's own overlay on the HUD
     * panel. Grey and black are easier on the eyes than JEG's white. JEG's smoke keeps its own grey.
     */
    public enum EyeFlash {
        WHITE(0xFFFFFF),
        GRAY(0x808080),
        BLACK(0x000000),
        OFF(0);

        private final int rgb;

        EyeFlash(int rgb) {
            this.rgb = rgb;
        }

        public int rgb() {
            return rgb;
        }
    }

    /** F10.4: how a two-handed gun says where the free hand can hold it, once the ghost hand's lessons are over. */
    public enum GripHint {
        /** Soft particles at the grip, and a buzz while the free hand is on it. */
        PARTICLES_AND_BUZZ,
        /** Only the buzz. */
        BUZZ,
        OFF
    }

    /** How JEG's grenades, molotov and water bomb are thrown in VR (F9.26). */
    public enum ThrowMode {
        /** Hold the trigger of the hand that holds it to pull the pin, release to throw where that hand points. */
        TRIGGER,
        /** Hold the trigger, swing as if throwing and release: direction and strength come from the hand's motion. */
        GESTURE,
        /** As Vivecraft does it: like a bow, drawn with the other hand. */
        VIVECRAFT
    }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<ThrowMode> THROW_MODE;
    public static final ForgeConfigSpec.EnumValue<GrabZone> GRAB_ZONE;
    public static final ForgeConfigSpec.BooleanValue SHOW_GUIDES;
    public static final ForgeConfigSpec.BooleanValue SHOW_SPRITES;
    public static final ForgeConfigSpec.EnumValue<GripHint> GRIP_HINT;
    public static final ForgeConfigSpec.BooleanValue LOCK_HOTBAR_IN_PRECISION;
    public static final ForgeConfigSpec.DoubleValue GRIP_HINT_RANGE;
    public static final ForgeConfigSpec.IntValue GRIP_GHOST_SHOWS_LEFT;
    public static final ForgeConfigSpec.BooleanValue RELOAD_WITH_BOTH_GUNS;
    public static final ForgeConfigSpec.EnumValue<HudPlacement> HUD_PLACEMENT;
    public static final ForgeConfigSpec.BooleanValue MIRROR_LEFT_HAND;
    public static final ForgeConfigSpec.BooleanValue BOW_LASER;
    public static final ForgeConfigSpec.BooleanValue SELF_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue GUN_HUD;
    public static final ForgeConfigSpec.BooleanValue HANDS_FOLLOW_ANIMATIONS;
    public static final ForgeConfigSpec.EnumValue<EyeFlash> EYE_FLASH;
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
        SHOW_SPRITES = builder
                .comment("JEG's ammo box at the hip while a gun is held (open when ammo can be grabbed), and a glow with",
                        "rippling rings where the free hand can grab ammo and where the magazine goes in. Colours: blue",
                        "ready, green with the hand in it, white to aim for. The grabbed magazine or round is drawn in",
                        "the free hand.")
                .define("showSprites", true);
        GRIP_HINT = builder
                .comment("Two-handed guns (precision mode): PARTICLES_AND_BUZZ shows soft particles where the free hand can",
                        "hold the gun when it comes close, and buzzes while the hand is there until it grabs; BUZZ only",
                        "buzzes; OFF neither.")
                .defineEnum("gripHint", GripHint.PARTICLES_AND_BUZZ);
        LOCK_HOTBAR_IN_PRECISION = builder
                .comment("While holding a gun with both hands (precision mode), Vivecraft's hotbar (touching a slot, the",
                        "hotbar buttons, reaching over the shoulder) and the swap-hands key do nothing, so the grip is not",
                        "ended by accident.")
                .define("lockHotbarInPrecision", true);
        GRIP_HINT_RANGE = builder
                .comment("How close (metres) the free hand must come to a two-handed gun for the particles to show.")
                .defineInRange("gripHintRange", 0.35, 0.1, 1.0);
        GRIP_GHOST_SHOWS_LEFT = builder
                .comment("How many more times a ghost hand shows how to hold a two-handed gun when one is taken (it",
                        "counts down; set it back to see the lesson again). /jegvr grip ghost shows it at any time.")
                .defineInRange("gripGhostShowsLeft", 2, 0, 100);
        RELOAD_WITH_BOTH_GUNS = builder
                .comment("Allow reloading while holding a gun in each hand. Off (default): put one gun away first (no free",
                        "hand for the magazine); trying to reload gives a short double buzz in that hand. Unloading still works.")
                .define("reloadWithBothGuns", false);
        builder.pop();
        builder.push("grenades");
        THROW_MODE = builder
                .comment("How grenades, the molotov and the water bomb are thrown: TRIGGER (hold the trigger of the hand",
                        "that holds it, release to throw where it points), GESTURE (hold the trigger, swing and release:",
                        "direction and strength from the hand's motion; strength needs this mod on the server too),",
                        "VIVECRAFT (like a bow, drawn with the other hand).")
                .defineEnum("throwMode", ThrowMode.GESTURE);
        builder.pop();
        builder.push("combat");
        SELF_DAMAGE = builder
                .comment("Your own bullets can hit your head (a shot there kills) or your other hand (the gun's normal",
                        "damage). Only bullets fired in VR, and not the free hand while holding a gun with both hands.",
                        "Needs this mod on the server, whose selfDamage rule may decide it instead.")
                .define("selfDamage", false);
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
        EYE_FLASH = builder
                .comment("The stun grenade's flash and JEG's smoke cover what each eye sees, instead of only the HUD panel.",
                        "The flash's colour: WHITE (as JEG), GRAY or BLACK (easier on the eyes). The smoke stays grey.",
                        "OFF: as JEG draws them, on the HUD.")
                .defineEnum("eyeFlash", EyeFlash.WHITE);
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

    public static boolean showSprites() {
        return !SPEC.isLoaded() || SHOW_SPRITES.get();
    }

    public static GripHint gripHint() {
        return SPEC.isLoaded() ? GRIP_HINT.get() : GripHint.PARTICLES_AND_BUZZ;
    }

    public static void setGripHint(GripHint hint) {
        GRIP_HINT.set(hint);
        GRIP_HINT.save();
    }

    public static boolean lockHotbarInPrecision() {
        return !SPEC.isLoaded() || LOCK_HOTBAR_IN_PRECISION.get();
    }

    public static void setLockHotbarInPrecision(boolean lock) {
        LOCK_HOTBAR_IN_PRECISION.set(lock);
        LOCK_HOTBAR_IN_PRECISION.save();
    }

    public static double gripHintRange() {
        return SPEC.isLoaded() ? GRIP_HINT_RANGE.get() : 0.35;
    }

    public static void setGripHintRange(double metres) {
        GRIP_HINT_RANGE.set(metres);
        GRIP_HINT_RANGE.save();
    }

    public static int gripGhostShowsLeft() {
        return SPEC.isLoaded() ? GRIP_GHOST_SHOWS_LEFT.get() : 0;
    }

    public static void setGripGhostShowsLeft(int shows) {
        GRIP_GHOST_SHOWS_LEFT.set(shows);
        GRIP_GHOST_SHOWS_LEFT.save();
    }

    public static void setShowSprites(boolean show) {
        SHOW_SPRITES.set(show);
        SHOW_SPRITES.save();
    }

    public static HudPlacement hudPlacement() {
        return SPEC.isLoaded() ? HUD_PLACEMENT.get() : HudPlacement.ELBOW;
    }

    public static boolean mirrorLeftHand() {
        return !SPEC.isLoaded() || MIRROR_LEFT_HAND.get();
    }

    /** In effect: the server's rule when it decides (S.8), the player's own option otherwise. */
    public static boolean bowLaser() {
        return VrRules.resolve(VrRulesClient.rules().bowLaser(), ownBowLaser());
    }

    /** The player's own option, whatever the server decides. */
    public static boolean ownBowLaser() {
        return !SPEC.isLoaded() || BOW_LASER.get();
    }

    public static boolean gunHud() {
        return !SPEC.isLoaded() || GUN_HUD.get();
    }

    public static boolean handsFollowAnimations() {
        return !SPEC.isLoaded() || HANDS_FOLLOW_ANIMATIONS.get();
    }

    public static EyeFlash eyeFlash() {
        return SPEC.isLoaded() ? EYE_FLASH.get() : EyeFlash.WHITE;
    }

    public static void setEyeFlash(EyeFlash flash) {
        EYE_FLASH.set(flash);
        EYE_FLASH.save();
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

    /** F9.8, in effect: the server's rule when it decides, the player's own option otherwise. */
    public static boolean selfDamage() {
        return VrRules.resolve(VrRulesClient.selfDamageRule(), ownSelfDamage());
    }

    /** The player's own option, whatever the server decides. Off until the file is loaded. */
    public static boolean ownSelfDamage() {
        return SPEC.isLoaded() && SELF_DAMAGE.get();
    }

    public static void setSelfDamage(boolean on) {
        SELF_DAMAGE.set(on);
        SELF_DAMAGE.save();
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

    /** In effect: the server's rule when it decides (S.8), the player's own option otherwise. */
    public static ThrowMode throwMode() {
        return ThrowMode.valueOf(VrRules.resolve(VrRulesClient.rules().throwMode(), ownThrowMode().name()));
    }

    /** The player's own option, whatever the server decides. */
    public static ThrowMode ownThrowMode() {
        return SPEC.isLoaded() ? THROW_MODE.get() : ThrowMode.GESTURE;
    }

    public static void setThrowMode(ThrowMode mode) {
        THROW_MODE.set(mode);
        THROW_MODE.save();
    }

    /** In effect: the server's rule when it decides (S.8), the player's own option otherwise. */
    public static boolean reloadWithBothGuns() {
        return VrRules.resolve(VrRulesClient.rules().reloadWithBothGuns(), ownReloadWithBothGuns());
    }

    /** The player's own option, whatever the server decides. */
    public static boolean ownReloadWithBothGuns() {
        return SPEC.isLoaded() && RELOAD_WITH_BOTH_GUNS.get();
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
