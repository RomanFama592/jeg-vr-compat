package com.jegvr.compat;

import com.jegvr.JegVrCompat;
import com.jegvr.compat.vivecraft.VivecraftBridge;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Safe facade: every method is false/null when Vivecraft is absent or its API misbehaves. */
public final class VR {
    private static final Logger LOGGER = LogManager.getLogger(JegVrCompat.MOD_ID);
    private static final KeyMapping[] NO_KEYS = new KeyMapping[0];
    private static Boolean loaded;
    private static boolean broken;
    private static KeyMapping[] teleportKeys;

    private VR() {
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

    /** Forces Vivecraft classes that we mix into to load now, so injection problems surface at startup. */
    public static void preload() {
        if (!loaded()) {
            return;
        }
        try {
            Class.forName("org.vivecraft.client_vr.render.VivecraftItemRendering");
            Class.forName("org.vivecraft.client_vr.render.helpers.VREffectsHelper");
            if (ModList.get().isLoaded("jeg")) {
                Class.forName("ttv.migami.jeg.network.message.C2SMessageShoot");
                Class.forName("ttv.migami.jeg.common.network.ServerPlayHandler");
                Class.forName("ttv.migami.jeg.common.ReloadTracker");
                Class.forName("ttv.migami.jeg.animations.GunAnimations");
                Class.forName("ttv.migami.jeg.client.handler.ReloadHandler");
                Class.forName("ttv.migami.jeg.client.handler.AimingHandler");
            }
        } catch (Throwable t) {
            disable(t);
        }
    }

    /** True when the local player is currently playing in VR. */
    public static boolean active() {
        if (!loaded()) {
            return false;
        }
        try {
            return VivecraftBridge.isActive();
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
            return VivecraftBridge.isVRPlayer(player);
        } catch (Throwable t) {
            disable(t);
            return false;
        }
    }

    /** World-space position and aim direction of a controller. */
    public record Aim(Vec3 pos, Vec3 dir) {
    }

    /** Main-hand controller aim, or null outside VR. */
    public static Aim mainHandAim() {
        if (!active()) {
            return null;
        }
        try {
            VivecraftBridge.Pose pose = VivecraftBridge.mainHand();
            return pose == null ? null : new Aim(pose.pos(), pose.dir());
        } catch (Throwable t) {
            disable(t);
            return null;
        }
    }

    /** Off-hand controller aim, or null outside VR. */
    public static Aim offHandAim() {
        if (!active()) {
            return null;
        }
        try {
            VivecraftBridge.Pose pose = VivecraftBridge.offHand();
            return pose == null ? null : new Aim(pose.pos(), pose.dir());
        } catch (Throwable t) {
            disable(t);
            return null;
        }
    }

    /** Headset position and look direction, or null outside VR. */
    public static Aim hmdAim() {
        if (!active()) {
            return null;
        }
        try {
            VivecraftBridge.Pose pose = VivecraftBridge.hmd();
            return pose == null ? null : new Aim(pose.pos(), pose.dir());
        } catch (Throwable t) {
            disable(t);
            return null;
        }
    }

    /** Vivecraft's world scale (1 = real size); distances measured in metres in the room scale with it. */
    public static float worldScale() {
        if (!active()) {
            return 1.0F;
        }
        try {
            return VivecraftBridge.worldScale();
        } catch (Throwable t) {
            disable(t);
            return 1.0F;
        }
    }

    /** True when Vivecraft's hands are reversed (main hand on the left controller). */
    /** A world direction in the main controller's axes (where the held gun is drawn), or null outside VR. */
    public static Vec3 toMainHandLocal(Vec3 dir) {
        if (!active()) {
            return null;
        }
        try {
            return VivecraftBridge.toMainHandLocal(dir);
        } catch (Throwable t) {
            disable(t);
            return null;
        }
    }

    /** False while Vivecraft draws a view that is not the player's eyes (third-person or mixed-reality camera). */
    public static boolean isEyePass() {
        if (!active()) {
            return true;
        }
        try {
            return VivecraftBridge.isEyePass();
        } catch (Throwable t) {
            disable(t);
            return true;
        }
    }

    /** True in Vivecraft's seated mode (hands not tracked). */
    public static boolean seated() {
        if (!active()) {
            return false;
        }
        try {
            return VivecraftBridge.isSeated();
        } catch (Throwable t) {
            disable(t);
            return false;
        }
    }

    public static boolean leftHanded() {
        if (!loaded()) {
            return false;
        }
        try {
            return VivecraftBridge.isLeftHanded();
        } catch (Throwable t) {
            disable(t);
            return false;
        }
    }

    /** The key on the off-hand grip with Vivecraft's default bindings, or null without Vivecraft. */
    public static KeyMapping offHandGripKey() {
        if (!loaded()) {
            return null;
        }
        try {
            return VivecraftBridge.offHandGripKey();
        } catch (Throwable t) {
            disable(t);
            return null;
        }
    }

    /** Vivecraft's teleport keys (normally the off-hand trigger), or none without Vivecraft. */
    public static KeyMapping[] teleportKeys() {
        if (!loaded()) {
            return NO_KEYS;
        }
        if (teleportKeys == null) {
            try {
                teleportKeys = VivecraftBridge.teleportKeys();
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
        if (!loaded()) {
            return false;
        }
        try {
            return VivecraftBridge.isBow(stack);
        } catch (Throwable t) {
            disable(t);
            return false;
        }
    }

    /** Vibrates the main-hand or off-hand controller; no-op outside VR. */
    public static void haptic(boolean mainHand, float seconds, float amplitude) {
        if (!active()) {
            return;
        }
        try {
            VivecraftBridge.haptic(mainHand, seconds, amplitude);
        } catch (Throwable t) {
            disable(t);
        }
    }

    private static void disable(Throwable t) {
        broken = true;
        LOGGER.error("Vivecraft bridge failed, VR compat disabled for this session", t);
    }
}
