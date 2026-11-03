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

/** Safe facade: every method is false/null when Vivecraft is absent or its API misbehaves. */
public final class VR {
    private static final Logger LOGGER = LogManager.getLogger(JegVrCompat.MOD_ID);
    private static final KeyMapping[] NO_KEYS = new KeyMapping[0];
    private static Boolean loaded;
    private static boolean broken;
    private static KeyMapping[] teleportKeys;
    private static long attackSideCheckedAt;
    private static boolean attackOnOffHand;
    private static boolean attackSideLogged;

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
    /** A world direction in a controller's axes (where the gun in that hand is drawn), or null outside VR. */
    public static Vec3 toHandLocal(boolean mainHand, Vec3 dir) {
        if (!active()) {
            return null;
        }
        try {
            return VivecraftBridge.toHandLocal(mainHand, dir);
        } catch (Throwable t) {
            disable(t);
            return null;
        }
    }

    /** A controller's X, Y and Z axes in world space, or null outside VR. */
    public static Vec3[] handAxes(boolean mainHand) {
        if (!active()) {
            return null;
        }
        try {
            return VivecraftBridge.handAxes(mainHand);
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
                Boolean main = VivecraftBridge.boundToMainHand(Minecraft.getInstance().options.keyAttack);
                boolean crossed = main != null && !main;
                if (crossed != attackOnOffHand || attackSideCheckedAt == now && !attackSideLogged) {
                    attackSideLogged = true;
                    LOGGER.info("SteamVR attack trigger: {} (left-handed: {}) -> gun triggers {}",
                            main == null ? "unknown" : main ? "main hand" : "off hand", VivecraftBridge.isLeftHanded(),
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
        if (!active()) {
            return false;
        }
        try {
            return VivecraftBridge.isHudOnHand();
        } catch (Throwable t) {
            disable(t);
            return false;
        }
    }

    /** Vivecraft's bow mode is "on" (every bow-like item); false without Vivecraft. */
    public static boolean bowModeOn() {
        if (!loaded()) {
            return false;
        }
        try {
            return VivecraftBridge.isBowModeOn();
        } catch (Throwable t) {
            disable(t);
            return false;
        }
    }

    /** Vivecraft's roomscale bow handles what the local player holds now. */
    public static boolean bowActive() {
        if (!active()) {
            return false;
        }
        try {
            return VivecraftBridge.isBowActive();
        } catch (Throwable t) {
            disable(t);
            return false;
        }
    }

    /** An arrow sits on the string of Vivecraft's roomscale bow (or it is being drawn). */
    public static boolean bowNotched() {
        if (!active()) {
            return false;
        }
        try {
            return VivecraftBridge.isBowNotched();
        } catch (Throwable t) {
            disable(t);
            return false;
        }
    }

    public static boolean bowDrawing() {
        if (!active()) {
            return false;
        }
        try {
            return VivecraftBridge.isBowDrawing();
        } catch (Throwable t) {
            disable(t);
            return false;
        }
    }

    /** How far the bow string is pulled, 0 to 1 (0 when not drawing). */
    public static float bowDrawPercent() {
        if (!active()) {
            return 0.0F;
        }
        try {
            return VivecraftBridge.bowDrawPercent();
        } catch (Throwable t) {
            disable(t);
            return 0.0F;
        }
    }

    /** True when the bow sits in the main-hand controller (Vivecraft's reversed bow); normally it is the off hand. */
    public static boolean bowInMainController() {
        if (!active()) {
            return false;
        }
        try {
            return VivecraftBridge.isBowInMainController();
        } catch (Throwable t) {
            disable(t);
            return false;
        }
    }

    /** Bow controller position and the arrow's direction (arrow hand towards bow hand), or null. */
    public static Aim bowAim() {
        if (!active()) {
            return null;
        }
        try {
            VivecraftBridge.Pose pose = VivecraftBridge.bowAim();
            return pose == null ? null : new Aim(pose.pos(), pose.dir());
        } catch (Throwable t) {
            disable(t);
            return null;
        }
    }

    /** A remote VR player in seated mode; false when unknown or not in VR. */
    public static boolean remoteSeated(Player player) {
        if (player == null || !loaded()) {
            return false;
        }
        try {
            return VivecraftBridge.isRemoteSeated(player);
        } catch (Throwable t) {
            disable(t);
            return false;
        }
    }

    /** Distance between a remote VR player's hands in room-scale metres, or -1 when unknown. */
    public static double remoteHandGap(Player player) {
        if (player == null || !loaded()) {
            return -1.0;
        }
        try {
            return VivecraftBridge.remoteHandGap(player);
        } catch (Throwable t) {
            disable(t);
            return -1.0;
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

    /** The key on the main-hand grip with Vivecraft's default bindings, or null without Vivecraft. */
    public static KeyMapping mainHandGripKey() {
        if (!loaded()) {
            return null;
        }
        try {
            return VivecraftBridge.mainHandGripKey();
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
