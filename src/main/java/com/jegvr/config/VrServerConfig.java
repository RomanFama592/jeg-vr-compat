package com.jegvr.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * S.8: server options, in {@code <world>/serverconfig/jeg_vr_compat-server.toml} (a copy in {@code defaultconfigs/}
 * is used for new worlds). Read on both sides by the server only; no client classes. Every rule defaults to
 * {@code PLAYER}, which is how the addon behaved before this file existed.
 */
public final class VrServerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<VrRules.Toggle> RELOAD_WITH_BOTH_GUNS;
    public static final ForgeConfigSpec.EnumValue<VrRules.Throw> THROW_MODE;
    public static final ForgeConfigSpec.EnumValue<VrRules.Toggle> BOW_LASER;
    public static final ForgeConfigSpec.EnumValue<VrRules.Toggle> SELF_DAMAGE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Options this server decides for VR players with JEG x Vivecraft. PLAYER leaves each player's own",
                "option. Players without this addon, or on a server without it, always use their own.").push("rules");
        RELOAD_WITH_BOTH_GUNS = builder
                .comment("Reloading with a gun in each hand: PLAYER, ALLOW or DENY. DENY is also enforced here: the",
                        "reload of a VR player holding two guns is cancelled.")
                .defineEnum("reloadWithBothGuns", VrRules.Toggle.PLAYER);
        THROW_MODE = builder
                .comment("How VR players throw grenades: PLAYER, TRIGGER, GESTURE or VIVECRAFT. Anything but PLAYER or",
                        "GESTURE ignores the strength of a throwing gesture.")
                .defineEnum("throwMode", VrRules.Throw.PLAYER);
        BOW_LASER = builder
                .comment("Laser sight on JEG's compound bow: PLAYER, ALLOW or DENY. Only drawn by the player's game.")
                .defineEnum("bowLaser", VrRules.Toggle.PLAYER);
        SELF_DAMAGE = builder
                .comment("Whether a VR player's own bullets can hit their head (it kills) or their other hand:",
                        "PLAYER (each player's own option, off by default), ALLOW or DENY. Minecraft still spares",
                        "players on a server with pvp off, or in a team without friendly fire.")
                .defineEnum("selfDamage", VrRules.Toggle.PLAYER);
        builder.pop();
        SPEC = builder.build();
    }

    private VrServerConfig() {
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SPEC);
    }

    /** The rules of the running server, or {@link VrRules.Rules#PLAYER_CHOICE} while no world is loaded. */
    public static VrRules.Rules rules() {
        if (!SPEC.isLoaded()) {
            return VrRules.Rules.PLAYER_CHOICE;
        }
        return new VrRules.Rules(RELOAD_WITH_BOTH_GUNS.get(), THROW_MODE.get(), BOW_LASER.get());
    }

    /** F9.8: kept apart from {@link #rules()}, whose packet has a fixed format. */
    public static VrRules.Toggle selfDamageRule() {
        return SPEC.isLoaded() ? SELF_DAMAGE.get() : VrRules.Toggle.PLAYER;
    }
}
