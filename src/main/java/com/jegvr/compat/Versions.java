package com.jegvr.compat;

import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;

import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which JEG and which Vivecraft are installed, read from the mod list and the mods' files without loading any of their
 * classes: the mixin plugin asks before the game's classes may be touched. The addon supports two lines of each mod in
 * the same jar; what differs between them is chosen from here.
 */
public final class Versions {
    /** The lines of Vivecraft the addon knows: their internals differ, so each has its own bridge and a few mixins. */
    public enum Vivecraft {
        NONE("none", null, null),
        /** 1.2.x: internals only (its API is {@code api_beta}). */
        V1_2("vc12", "com.jegvr.compat.vivecraft.Vivecraft12Bridge", "com.jegvr.mixin.vc12."),
        /** 1.3.x: the official API ({@code org.vivecraft.api}) plus internals. */
        V1_3("vc13", "com.jegvr.compat.vivecraft.Vivecraft13Bridge", "com.jegvr.mixin.vc13.");

        private final String id;
        private final String bridgeClass;
        private final String mixinPackage;

        Vivecraft(String id, String bridgeClass, String mixinPackage) {
            this.id = id;
            this.bridgeClass = bridgeClass;
            this.mixinPackage = mixinPackage;
        }

        /** The short name logged with the profile, also the name of the source set with this line's code. */
        public String id() {
            return id;
        }

        /** The {@link VrBackend} for this line, or null for {@link #NONE}. */
        public String bridgeClass() {
            return bridgeClass;
        }
    }

    private static final String JEG = "jeg";
    private static final String VIVECRAFT = "vivecraft";
    /** Vivecraft's mod version carries the game's first: 1.20.1-1.3.15. Its own version is the last three numbers. */
    private static final Pattern VIVECRAFT_VERSION = Pattern.compile("([0-9]+)[.]([0-9]+)[.]([0-9]+)[^0-9]*$");
    private static final Pattern LEADING_VERSION = Pattern.compile("^([0-9]+)[.]([0-9]+)");
    private static final String[] VERSION_MIXIN_PACKAGES = {Vivecraft.V1_2.mixinPackage, Vivecraft.V1_3.mixinPackage};

    private static Vivecraft vivecraft;
    private static Boolean jegHarvesting;
    private static Boolean jegVehicles;
    private static Boolean jegHeatsPerShot;

    private Versions() {
    }

    public static boolean hasJeg() {
        return file(JEG) != null;
    }

    public static boolean hasVivecraft() {
        return file(VIVECRAFT) != null;
    }

    public static String jegVersion() {
        return version(JEG);
    }

    public static String vivecraftVersion() {
        return version(VIVECRAFT);
    }

    /** The line of the installed Vivecraft. A version this addon does not know counts as the newest line. */
    public static Vivecraft vivecraft() {
        if (vivecraft == null) {
            vivecraft = hasVivecraft() ? vivecraftLine(vivecraftVersion()) : Vivecraft.NONE;
        }
        return vivecraft;
    }

    /** Pure: the line a Vivecraft mod version belongs to ("1.20.1-1.2.5" is 1.2). */
    public static Vivecraft vivecraftLine(String modVersion) {
        Matcher matcher = VIVECRAFT_VERSION.matcher(modVersion == null ? "" : modVersion);
        if (!matcher.find()) {
            return Vivecraft.V1_3;
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        return major == 1 && minor <= 2 ? Vivecraft.V1_2 : Vivecraft.V1_3;
    }

    /**
     * Whether a mixin of this addon is for the installed Vivecraft. Mixins in {@code mixin.vc12} and {@code mixin.vc13}
     * belong to one line; every other one works with both.
     */
    public static boolean mixinFitsVivecraft(String mixinClassName) {
        for (String versionPackage : VERSION_MIXIN_PACKAGES) {
            if (mixinClassName.startsWith(versionPackage)) {
                return versionPackage.equals(vivecraft().mixinPackage);
            }
        }
        return true;
    }

    /** JEG 0.14: mining guns ({@code HarvestingGunItem}: chainsaw, jackhammer). */
    public static boolean jegHasHarvesting() {
        if (jegHarvesting == null) {
            jegHarvesting = jegHasClass("ttv/migami/jeg/item/HarvestingGunItem.class");
        }
        return jegHarvesting;
    }

    /** JEG 0.14: shooting from a mount ({@code C2SMessageVehicleShoot}). */
    public static boolean jegHasVehicles() {
        if (jegVehicles == null) {
            jegVehicles = jegHasClass("ttv/migami/jeg/network/message/C2SMessageVehicleShoot.class");
        }
        return jegVehicles;
    }

    /**
     * JEG 0.14 heats a gun by one for every shot ({@code ShootingHandler.fire}); before, by one for every tick the
     * trigger was held. There is no class to look for: it is the same method, so this goes by version.
     */
    public static boolean jegHeatsPerShot() {
        if (jegHeatsPerShot == null) {
            jegHeatsPerShot = hasJeg() && atLeast(jegVersion(), 0, 14);
        }
        return jegHeatsPerShot;
    }

    /** Pure: whether a version like "0.14.5" is at least {@code major.minor}. A version that cannot be read counts as newer. */
    public static boolean atLeast(String version, int major, int minor) {
        Matcher matcher = LEADING_VERSION.matcher(version == null ? "" : version);
        if (!matcher.find()) {
            return true;
        }
        int foundMajor = Integer.parseInt(matcher.group(1));
        int foundMinor = Integer.parseInt(matcher.group(2));
        return foundMajor > major || (foundMajor == major && foundMinor >= minor);
    }

    /** One line for the log and for bug reports: {@code JEG 0.14.5 [mining, mounts] / Vivecraft 1.20.1-1.3.15 (vc13)}. */
    public static String profile() {
        StringBuilder text = new StringBuilder("JEG ").append(jegVersion());
        if (hasJeg()) {
            StringBuilder features = new StringBuilder();
            feature(features, jegHasHarvesting(), "mining");
            feature(features, jegHasVehicles(), "mounts");
            if (features.length() > 0) {
                text.append(" [").append(features).append(']');
            }
        }
        text.append(" / Vivecraft ").append(vivecraftVersion());
        if (hasVivecraft()) {
            text.append(" (").append(vivecraft().id()).append(')');
        }
        return text.toString();
    }

    private static void feature(StringBuilder features, boolean present, String name) {
        if (present) {
            features.append(features.length() > 0 ? ", " : "").append(name);
        }
    }

    private static boolean jegHasClass(String path) {
        ModFileInfo jeg = file(JEG);
        try {
            return jeg != null && Files.exists(jeg.getFile().findResource(path));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String version(String modId) {
        ModFileInfo info = file(modId);
        if (info == null) {
            return "none";
        }
        return info.getMods().stream().filter(mod -> mod.getModId().equals(modId)).findFirst()
                .map(mod -> mod.getVersion().toString()).orElse("unknown");
    }

    private static ModFileInfo file(String modId) {
        LoadingModList mods = LoadingModList.get();
        return mods == null ? null : mods.getModFileById(modId);
    }
}
