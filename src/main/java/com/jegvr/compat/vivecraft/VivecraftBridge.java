package com.jegvr.compat.vivecraft;

import com.jegvr.mixin.input.MCOpenVRAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.vivecraft.client.ClientVRPlayers;
import org.vivecraft.client.network.ClientNetworking;
import org.vivecraft.api_beta.VivecraftAPI;
import org.vivecraft.client.VivecraftVRMod;
import org.vivecraft.api_beta.client.VivecraftClientAPI;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.VRData;
import org.vivecraft.client_vr.gameplay.VRPlayer;
import org.vivecraft.client_vr.gameplay.trackers.BowTracker;
import org.vivecraft.client_vr.provider.ControllerType;
import org.vivecraft.client_vr.provider.MCVR;
import org.vivecraft.client_vr.provider.openvr_lwjgl.MCOpenVR;
import org.vivecraft.client_vr.provider.openvr_lwjgl.VRInputAction;
import org.vivecraft.client_vr.render.RenderPass;
import org.vivecraft.client_vr.settings.VRSettings;

/**
 * The only class that touches Vivecraft 1.2.5 directly. Never load it unless the "vivecraft" mod is present;
 * go through {@link com.jegvr.compat.VR}.
 */
public final class VivecraftBridge {
    /** World-space pose of a tracked device. */
    public record Pose(Vec3 pos, Vec3 dir) {
    }

    private VivecraftBridge() {
    }

    public static boolean isActive() {
        return VivecraftClientAPI.getInstance().isVrActive() && data() != null;
    }

    public static boolean isVRPlayer(Player player) {
        return VivecraftAPI.getInstance().isVRPlayer(player);
    }

    /** Pose of the hand holding the main item (follows Vivecraft's left-handed setting). */
    public static Pose mainHand() {
        VRData d = data();
        return d == null ? null : pose(d.c0);
    }

    public static Pose offHand() {
        VRData d = data();
        return d == null ? null : pose(d.c1);
    }

    public static Pose hmd() {
        VRData d = data();
        return d == null ? null : pose(d.hmd);
    }

    /** {@code dir} (world space) in a controller's own axes, the frame the gun held in that hand is drawn in. */
    public static Vec3 toHandLocal(boolean mainHand, Vec3 dir) {
        VRData d = data();
        VRData.VRDevicePose c = d == null ? null : mainHand ? d.c0 : d.c1;
        if (c == null) {
            return null;
        }
        Vector3f x = c.getCustomVector(new Vector3f(1.0F, 0.0F, 0.0F));
        Vector3f y = c.getCustomVector(new Vector3f(0.0F, 1.0F, 0.0F));
        Vector3f z = c.getCustomVector(new Vector3f(0.0F, 0.0F, 1.0F));
        return new Vec3(dir.x * x.x + dir.y * x.y + dir.z * x.z,
                dir.x * y.x + dir.y * y.y + dir.z * y.z,
                dir.x * z.x + dir.y * z.y + dir.z * z.z);
    }

    /** A controller's own X, Y and Z axes in world space. */
    public static Vec3[] handAxes(boolean mainHand) {
        VRData d = data();
        VRData.VRDevicePose c = d == null ? null : mainHand ? d.c0 : d.c1;
        if (c == null) {
            return null;
        }
        Vector3f x = c.getCustomVector(new Vector3f(1.0F, 0.0F, 0.0F));
        Vector3f y = c.getCustomVector(new Vector3f(0.0F, 1.0F, 0.0F));
        Vector3f z = c.getCustomVector(new Vector3f(0.0F, 0.0F, 1.0F));
        return new Vec3[]{new Vec3(x.x, x.y, x.z), new Vec3(y.x, y.y, y.z), new Vec3(z.x, z.y, z.z)};
    }

    /** False while Vivecraft draws the third-person or the mixed-reality camera view (not the player's own eyes). */
    public static boolean isEyePass() {
        RenderPass pass = ClientDataHolderVR.getInstance().currentPass;
        return pass != RenderPass.THIRD && pass != RenderPass.CAMERA;
    }

    /** Vibrates a controller; {@code mainHand} false targets the free hand. */
    public static void haptic(boolean mainHand, float seconds, float amplitude) {
        MCVR vr = MCVR.get();
        if (vr == null) {
            return;
        }
        // Vivecraft's ControllerType is logical (RIGHT = main hand, LEFT = off hand); it maps it to the physical
        // controller itself when hands are reversed.
        vr.triggerHapticPulse(mainHand ? ControllerType.RIGHT : ControllerType.LEFT, seconds, 160.0F, amplitude);
    }

    /** Vivecraft's teleport keys (teleport mode and free-move fallback), bound to the off-hand trigger by default. */
    public static KeyMapping[] teleportKeys() {
        VivecraftVRMod mod = VivecraftVRMod.INSTANCE;
        return new KeyMapping[]{mod.keyTeleport, mod.keyTeleportFallback};
    }

    /**
     * The key on the off-hand grip: Vivecraft's default bindings put "hotbar previous" on the left grip and "hotbar
     * next" on the right one, and the off hand is the left one unless hands are reversed.
     */
    public static KeyMapping offHandGripKey() {
        VivecraftVRMod mod = VivecraftVRMod.INSTANCE;
        return isLeftHanded() ? mod.keyHotbarNext : mod.keyHotbarPrev;
    }

    /** The key on the main-hand grip: the other "hotbar" key. */
    public static KeyMapping mainHandGripKey() {
        VivecraftVRMod mod = VivecraftVRMod.INSTANCE;
        return isLeftHanded() ? mod.keyHotbarPrev : mod.keyHotbarNext;
    }

    /**
     * Whether {@code key} is bound, in the current SteamVR binding, to the main-hand controller (true), the off-hand one
     * (false), or neither or both (null).
     */
    public static Boolean boundToMainHand(KeyMapping key) {
        MCVR vr = MCVR.get();
        if (!(vr instanceof MCOpenVR openVr)) {
            return null;
        }
        VRInputAction action = vr.getInputAction(key);
        if (action == null) {
            return null;
        }
        boolean left = false;
        boolean right = false;
        for (long origin : openVr.getOrigins(action)) {
            ControllerType type = ((MCOpenVRAccessor) openVr).jegvr$originController(origin);
            left |= type == ControllerType.LEFT;
            right |= type == ControllerType.RIGHT;
        }
        if (left == right) {
            return null;
        }
        // Logical types: with reversed hands Vivecraft swaps its device slots, so RIGHT is always the main hand.
        return right;
    }

    /** Vivecraft's roomscale bow: drawn with the off-hand trigger, so that trigger must keep its normal role. */
    public static boolean isBow(ItemStack stack) {
        return BowTracker.isBow(stack);
    }

    /** Vivecraft's bow mode is "on" (every bow-like item, not only the vanilla bow). */
    public static boolean isBowModeOn() {
        return ClientDataHolderVR.getInstance().vrSettings.bowMode == VRSettings.BowMode.ON;
    }

    /** Vivecraft's roomscale bow handles the item held now (a bow in either hand, standing, alive). */
    public static boolean isBowActive() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && ClientDataHolderVR.getInstance().bowTracker.isActive(mc.player);
    }

    /** An arrow sits on the string (the arrow hand is at the string, or the bow is being drawn). */
    public static boolean isBowNotched() {
        return ClientDataHolderVR.getInstance().bowTracker.isNotched();
    }

    public static boolean isBowDrawing() {
        return ClientDataHolderVR.getInstance().bowTracker.isDrawing;
    }

    /** How far the string is pulled, 0 to 1. */
    public static float bowDrawPercent() {
        BowTracker bow = ClientDataHolderVR.getInstance().bowTracker;
        return bow.isDrawing ? Math.max(0.0F, Math.min(1.0F, bow.getDrawPercent())) : 0.0F;
    }

    /** True when the bow is held by the main-hand controller (Vivecraft's "reverse shooting eye" with server support). */
    public static boolean isBowInMainController() {
        return ClientDataHolderVR.getInstance().vrSettings.reverseShootingEye && ClientNetworking.supportsReversedBow();
    }

    /**
     * Where the arrow leaves: the bow controller, and the direction from the arrow hand to the bow hand (Vivecraft's
     * aim vector points the other way, see {@code VRPlayer.getAimVector}). Null without an aim yet.
     */
    public static Pose bowAim() {
        VRData d = data();
        Vector3fc aim = ClientDataHolderVR.getInstance().bowTracker.getAimVector();
        if (d == null || aim == null || aim.lengthSquared() < 1.0E-6F) {
            return null;
        }
        VRData.VRDevicePose bow = isBowInMainController() ? d.c0 : d.c1;
        return new Pose(bow.getPosition(), new Vec3(-aim.x(), -aim.y(), -aim.z()).normalize());
    }

    /**
     * The bow hand's grip pose ({@code VRData.getHand}, not the aim pose), the one Vivecraft's bow is laid out by: its
     * up and back axes in world space. Holding a bow, "up" points at the target and "back" runs along the controller.
     */
    public static Vec3[] bowHandAxes() {
        VRData d = data();
        if (d == null) {
            return null;
        }
        VRData.VRDevicePose hand = d.getHand(isBowInMainController() ? 0 : 1);
        Vector3f up = hand.getCustomVector(new Vector3f(0.0F, 1.0F, 0.0F));
        Vector3f back = hand.getCustomVector(new Vector3f(0.0F, 0.0F, 1.0F));
        return new Vec3[]{new Vec3(up.x, up.y, up.z), new Vec3(back.x, back.y, back.z)};
    }

    /** A remote VR player in Vivecraft's seated mode (no tracked hands). */
    public static boolean isRemoteSeated(Player player) {
        return ClientVRPlayers.getInstance().isVRAndSeated(player.getUUID());
    }

    /** Distance between a remote VR player's hands, in their room-scale metres, or -1 if unknown. */
    public static double remoteHandGap(Player player) {
        ClientVRPlayers.RotInfo info = ClientVRPlayers.getInstance().getRotationsForPlayer(player.getUUID());
        if (info == null || info.mainHandPos == null || info.offHandPos == null) {
            return -1.0;
        }
        double gap = info.mainHandPos.distance(info.offHandPos);
        return info.worldScale > 0.0F ? gap / info.worldScale : gap;
    }

    /** True when Vivecraft's HUD (with the hotbar) is locked to the off-hand controller, not to the head. */
    public static boolean isHudOnHand() {
        VRSettings.HUDLock lock = ClientDataHolderVR.getInstance().vrSettings.vrHudLockMode;
        return lock == VRSettings.HUDLock.HAND || lock == VRSettings.HUDLock.WRIST;
    }

    /** Vivecraft's seated mode: the hands follow the view, they are not tracked. */
    public static boolean isSeated() {
        return ClientDataHolderVR.getInstance().vrSettings.seated;
    }

    public static boolean isLeftHanded() {
        return ClientDataHolderVR.getInstance().vrSettings.reverseHands;
    }

    public static float worldScale() {
        VRPlayer vrPlayer = VRPlayer.get();
        return vrPlayer == null ? 1.0F : vrPlayer.worldScale;
    }

    private static VRData data() {
        VRPlayer vrPlayer = VRPlayer.get();
        if (vrPlayer == null) {
            return null;
        }
        return vrPlayer.vrdata_world_render != null ? vrPlayer.vrdata_world_render : vrPlayer.getVRDataWorld();
    }

    private static Pose pose(VRData.VRDevicePose pose) {
        if (pose == null) {
            return null;
        }
        Vector3f dir = pose.getDirection();
        return new Pose(pose.getPosition(), new Vec3(dir.x, dir.y, dir.z));
    }
}
