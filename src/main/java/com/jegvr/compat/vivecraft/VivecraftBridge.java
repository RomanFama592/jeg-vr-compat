package com.jegvr.compat.vivecraft;

import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.vivecraft.api_beta.VivecraftAPI;
import org.vivecraft.client.VivecraftVRMod;
import org.vivecraft.api_beta.client.VivecraftClientAPI;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.VRData;
import org.vivecraft.client_vr.gameplay.VRPlayer;
import org.vivecraft.client_vr.gameplay.trackers.BowTracker;
import org.vivecraft.client_vr.provider.ControllerType;
import org.vivecraft.client_vr.provider.MCVR;
import org.vivecraft.client_vr.render.RenderPass;

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
        boolean right = mainHand != isLeftHanded();
        vr.triggerHapticPulse(right ? ControllerType.RIGHT : ControllerType.LEFT, seconds, 160.0F, amplitude);
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

    /** Vivecraft's roomscale bow: drawn with the off-hand trigger, so that trigger must keep its normal role. */
    public static boolean isBow(ItemStack stack) {
        return BowTracker.isBow(stack);
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
