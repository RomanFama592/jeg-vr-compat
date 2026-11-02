package com.jegvr.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

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
    public static final ForgeConfigSpec.EnumValue<HudPlacement> HUD_PLACEMENT;

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
        builder.pop();
        builder.push("visuals");
        HUD_PLACEMENT = builder
                .comment("With a gun in the off hand, where Vivecraft's hand or wrist HUD (hotbar) goes:",
                        "VIVECRAFT (unchanged), ELBOW (slid back towards the elbow), FOREARM (along the inner forearm).")
                .defineEnum("hudPlacement", HudPlacement.ELBOW);
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

    public static void setHudPlacement(HudPlacement placement) {
        HUD_PLACEMENT.set(placement);
        HUD_PLACEMENT.save();
    }

    public static void setGrabZone(GrabZone zone) {
        GRAB_ZONE.set(zone);
        GRAB_ZONE.save();
    }

    public static void setShowGuides(boolean show) {
        SHOW_GUIDES.set(show);
        SHOW_GUIDES.save();
    }
}
