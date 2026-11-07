package com.jegvr.compat;

import com.jegvr.JegVrCompat;
import com.jegvr.compat.vivecraft.VivecraftBridge;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

import java.util.function.Function;

/**
 * Safe facade over the {@link VrBackend}: every method answers false, null or a neutral value when Vivecraft is absent or
 * its API misbehaves. The first failure turns VR support off for the session, which leaves JEG as it is in flat.
 */
public final class VR {
    private static final Logger LOGGER = LogManager.getLogger(JegVrCompat.MOD_ID);
    private static final KeyMapping[] NO_KEYS = new KeyMapping[0];
    /** The classes our mixins change, loaded (not initialized) at startup so that injection errors show up there. */
    private static final String[] VIVECRAFT_MIXIN_TARGETS = {
            "org.vivecraft.client_vr.render.VivecraftItemRendering",
            "org.vivecraft.client_vr.render.helpers.VREffectsHelper"
    };
    private static final String[] JEG_MIXIN_TARGETS = {
            "ttv.migami.jeg.network.message.C2SMessageShoot",
            "ttv.migami.jeg.common.network.ServerPlayHandler",
            "ttv.migami.jeg.common.ReloadTracker",
            "ttv.migami.jeg.animations.GunAnimations",
            "ttv.migami.jeg.client.handler.ReloadHandler",
            "ttv.migami.jeg.client.handler.AimingHandler"
    };

    private static Boolean loaded;
    private static boolean broken;
    private static VrBackend backend;
    private static KeyMapping[] teleportKeys;
    private static long attackSideCheckedAt;
    private static boolean attackOnOffHand;
    private static boolean attackSideLogged;

    private VR() {
    }

    /** World-space position and aim direction of a tracked device. */
    public record Aim(Vec3 pos, Vec3 dir) {
    }

    public static boolean loaded() {
        if (loaded == null) {
            ModList list = ModList.get();
            if (list == null) {
                return false;
            }
            loaded = list.isLoaded("vivecraft");
        }
        return loaded && !broken;
    }

    /** The backend for the Vivecraft that is installed. Only called with Vivecraft present (see the class's Javadoc). */
    private static VrBackend backend() {
        if (backend == null) {
            backend = new VivecraftBridge();
        }
        return backend;
    }

    /** The VR mod's line for bug reports, or a note that there is none. */
    public static String description() {
        return loaded() ? backend().name() : "none";
    }

    /** Forces the classes we mix into to load now, so injection problems surface at startup. */
    public static void preload() {
        if (!loaded()) {
            return;
        }
        try {
            ClassLoader classLoader = VR.class.getClassLoader();
            for (String name : VIVECRAFT_MIXIN_TARGETS) {
                Class.forName(name, false, classLoader);
            }
            if (ModList.get().isLoaded("jeg")) {
                for (String name : JEG_MIXIN_TARGETS) {
                    Class.forName(name, false, classLoader);
                }
            }
        } catch (Throwable t) {
            disable(t);
        }
    }

    /** True when the local player is currently playing in VR. Called constantly: no allocation on this path. */
    public static boolean active() {
        if (!loaded()) {
            return false;
        }
        try {
            return backend().isActive();
        } catch (Throwable t) {
            disable(t);
            return false;
        }
    }

    public static boolean isVRPlayer(Player player) {
        if (player == null || !loaded()) {
            return false;
        }
        try {
            return backend().isVRPlayer(player);
        } catch (Throwable t) {
            disable(t);
            return false;
        }
    }

    /** Asks the backend while the player is in VR; {@code outside} otherwise or when it fails. */
    private static <T> T inVr(Function<VrBackend, T> call, T outside) {
        return active() ? safely(call, outside) : outside;
    }

    /** Asks the backend with Vivecraft installed, in VR or not; {@code outside} otherwise or when it fails. */
    private static <T> T installed(Function<VrBackend, T> call, T outside) {
        return loaded() ? safely(call, outside) : outside;
    }

    private static <T> T safely(Function<VrBackend, T> call, T fallback) {
        try {
            return call.apply(backend());
        } catch (Throwable t) {
            disable(t);
            return fallback;
        }
    }

    /** Main-hand controller aim, or null outside VR. */
    public static Aim mainHandAim() {
        return inVr(VrBackend::mainHand, null);
    }

    /** Off-hand controller aim, or null outside VR. */
    public static Aim offHandAim() {
        return inVr(VrBackend::offHand, null);
    }

    /** Headset position and look direction, or null outside VR. */
    public static Aim hmdAim() {
        return inVr(VrBackend::hmd, null);
    }

    /** The headset's up axis in world space, or null outside VR. */
    public static Vec3 hmdUp() {
        return inVr(VrBackend::hmdUp, null);
    }

    /** Vivecraft's world scale (1 = real size); distances measured in metres in the room scale with it. */
    public static float worldScale() {
        return inVr(VrBackend::worldScale, 1.0F);
    }

    /** A world direction in a controller's axes (where the gun in that hand is drawn), or null outside VR. */
    public static Vec3 toHandLocal(boolean mainHand, Vec3 dir) {
        return inVr(vr -> vr.toHandLocal(mainHand, dir), null);
    }

    /** A controller's X, Y and Z axes in world space, or null outside VR. */
    public static Vec3[] handAxes(boolean mainHand) {
        return inVr(vr -> vr.handAxes(mainHand), null);
    }

    /** A controller's rotation and position in the room, or null outside VR. */
    public static Matrix4f roomHand(boolean mainHand) {
        return inVr(vr -> vr.roomHand(mainHand), null);
    }

    /** True while Vivecraft draws the left eye, once per frame; false outside VR. */
    public static boolean isLeftEyePass() {
        return inVr(VrBackend::isLeftEyePass, false);
    }

    /** False while Vivecraft draws a view that is not the player's eyes (third-person or mixed-reality camera). */
    public static boolean isEyePass() {
        return inVr(VrBackend::isEyePass, true);
    }

    /**
     * True when SteamVR binds the attack key (JEG's shoot) to the off-hand controller: hands reversed in Vivecraft
     * while SteamVR kept the right-handed binding, or the other way round. Checked once a second.
     */
    public static boolean attackOnOffHand() {
        if (!active()) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - attackSideCheckedAt > 1000L) {
            attackSideCheckedAt = now;
            try {
                Boolean main = backend().boundToMainHand(Minecraft.getInstance().options.keyAttack);
                boolean crossed = main != null && !main;
                if (crossed != attackOnOffHand || !attackSideLogged) {
                    attackSideLogged = true;
                    LOGGER.info("SteamVR attack trigger: {} (left-handed: {}) -> gun triggers {}",
                            main == null ? "unknown" : main ? "main hand" : "off hand", backend().isLeftHanded(),
                            crossed ? "swapped" : "as bound");
                }
                attackOnOffHand = crossed;
            } catch (Throwable t) {
                disable(t);
                attackOnOffHand = false;
            }
        }
        return attackOnOffHand;
    }

    /** True when Vivecraft's HUD is locked to the off-hand controller (hand or wrist). */
    public static boolean hudOnHand() {
        return inVr(VrBackend::isHudOnHand, false);
    }

    /** True in Vivecraft's seated mode (hands not tracked). */
    public static boolean seated() {
        return inVr(VrBackend::isSeated, false);
    }

    /** True when Vivecraft's hands are reversed (main hand on the left controller). */
    public static boolean leftHanded() {
        return installed(VrBackend::isLeftHanded, false);
    }

    /** True in VR with a shader pack on: what is drawn then goes through the pack's programs, not the game's. */
    public static boolean shadersActive() {
        return inVr(VrBackend::isShaderActive, false);
    }

    /** The key on the off-hand grip with Vivecraft's default bindings, or null without Vivecraft. */
    public static KeyMapping offHandGripKey() {
        return installed(VrBackend::offHandGripKey, null);
    }

    /** The key on the main-hand grip with Vivecraft's default bindings, or null without Vivecraft. */
    public static KeyMapping mainHandGripKey() {
        return installed(VrBackend::mainHandGripKey, null);
    }

    /** Vivecraft's teleport keys (normally the off-hand trigger), or none without Vivecraft. */
    public static KeyMapping[] teleportKeys() {
        if (!loaded()) {
            return NO_KEYS;
        }
        if (teleportKeys == null) {
            try {
                teleportKeys = backend().teleportKeys();
            } catch (Throwable t) {
                disable(t);
                return NO_KEYS;
            }
        }
        return teleportKeys;
    }

    public static boolean isTeleportKey(KeyMapping key) {
        for (KeyMapping teleport : teleportKeys()) {
            if (teleport == key) {
                return true;
            }
        }
        return false;
    }

    /** True when Vivecraft treats {@code stack} as a roomscale bow (false without Vivecraft). */
    public static boolean isBow(ItemStack stack) {
        return installed(vr -> vr.isBow(stack), false);
    }

    /** Vibrates the main-hand or off-hand controller; no-op outside VR. */
    public static void haptic(boolean mainHand, float seconds, float amplitude) {
        if (!active()) {
            return;
        }
        try {
            backend().haptic(mainHand, seconds, amplitude);
        } catch (Throwable t) {
            disable(t);
        }
    }

    private static void disable(Throwable t) {
        broken = true;
        LOGGER.error("Vivecraft bridge failed, VR compat disabled for this session", t);
    }
}
