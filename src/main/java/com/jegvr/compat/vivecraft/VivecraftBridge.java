package com.jegvr.compat.vivecraft;

import com.jegvr.compat.VR;
import com.jegvr.compat.VrBackend;
import com.jegvr.mixin.input.MCOpenVRAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
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
import org.vivecraft.client_vr.provider.openvr_lwjgl.MCOpenVR;
import org.vivecraft.client_vr.provider.openvr_lwjgl.VRInputAction;
import org.vivecraft.client_vr.render.RenderPass;
import org.vivecraft.client_vr.settings.VRSettings;
import org.vivecraft.mod_compat_vr.shaders.ShadersHelper;

/**
 * {@link VrBackend} for Vivecraft 1.2.x. Outside the mixins, the only class that touches Vivecraft directly. Never load it
 * unless the "vivecraft" mod is present; go through {@link VR}.
 */
public final class VivecraftBridge implements VrBackend {
    @Override
    public String name() {
        return "Vivecraft 1.2";
    }

    @Override
    public boolean isActive() {
        return VivecraftClientAPI.getInstance().isVrActive() && data() != null;
    }

    @Override
    public boolean isVRPlayer(Player player) {
        return VivecraftAPI.getInstance().isVRPlayer(player);
    }

    @Override
    public VR.Aim mainHand() {
        VRData d = data();
        return d == null ? null : pose(d.c0);
    }

    @Override
    public VR.Aim offHand() {
        VRData d = data();
        return d == null ? null : pose(d.c1);
    }

    @Override
    public VR.Aim hmd() {
        VRData d = data();
        return d == null ? null : pose(d.hmd);
    }

    @Override
    public Vec3 hmdUp() {
        VRData d = data();
        if (d == null) {
            return null;
        }
        Vector3f up = d.hmd.getCustomVector(new Vector3f(0.0F, 1.0F, 0.0F));
        return new Vec3(up.x, up.y, up.z);
    }

    @Override
    public Vec3 toHandLocal(boolean mainHand, Vec3 dir) {
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

    @Override
    public Matrix4f roomHand(boolean mainHand) {
        VRPlayer player = VRPlayer.get();
        VRData room = player == null ? null : player.vrdata_room_pre;
        if (room == null) {
            return null;
        }
        VRData.VRDevicePose c = mainHand ? room.c0 : room.c1;
        Vector3f position = c.getPositionF();
        // Same rotation that RenderHelper.setupRenderingAtController uses.
        return c.getMatrix().invert().transpose().setTranslation(position);
    }

    @Override
    public boolean isLeftEyePass() {
        return ClientDataHolderVR.getInstance().currentPass == RenderPass.LEFT;
    }

    @Override
    public Vec3[] handAxes(boolean mainHand) {
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

    @Override
    public boolean isEyePass() {
        RenderPass pass = ClientDataHolderVR.getInstance().currentPass;
        return pass != RenderPass.THIRD && pass != RenderPass.CAMERA;
    }

    @Override
    public void haptic(boolean mainHand, float seconds, float amplitude) {
        MCVR vr = MCVR.get();
        if (vr == null) {
            return;
        }
        // Vivecraft's ControllerType is logical (RIGHT = main hand, LEFT = off hand); it maps it to the physical
        // controller itself when hands are reversed.
        vr.triggerHapticPulse(mainHand ? ControllerType.RIGHT : ControllerType.LEFT, seconds, 160.0F, amplitude);
    }

    @Override
    public KeyMapping[] teleportKeys() {
        VivecraftVRMod mod = VivecraftVRMod.INSTANCE;
        return new KeyMapping[]{mod.keyTeleport, mod.keyTeleportFallback};
    }

    /**
     * Vivecraft's default bindings put "hotbar previous" on the left grip and "hotbar next" on the right one, and the off
     * hand is the left one unless hands are reversed.
     */
    @Override
    public KeyMapping offHandGripKey() {
        VivecraftVRMod mod = VivecraftVRMod.INSTANCE;
        return isLeftHanded() ? mod.keyHotbarNext : mod.keyHotbarPrev;
    }

    @Override
    public KeyMapping mainHandGripKey() {
        VivecraftVRMod mod = VivecraftVRMod.INSTANCE;
        return isLeftHanded() ? mod.keyHotbarPrev : mod.keyHotbarNext;
    }

    @Override
    public Boolean boundToMainHand(KeyMapping key) {
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

    @Override
    public boolean isBow(ItemStack stack) {
        return BowTracker.isBow(stack);
    }

    @Override
    public boolean isHudOnHand() {
        VRSettings.HUDLock lock = ClientDataHolderVR.getInstance().vrSettings.vrHudLockMode;
        return lock == VRSettings.HUDLock.HAND || lock == VRSettings.HUDLock.WRIST;
    }

    @Override
    public boolean isSeated() {
        return ClientDataHolderVR.getInstance().vrSettings.seated;
    }

    @Override
    public boolean isLeftHanded() {
        return ClientDataHolderVR.getInstance().vrSettings.reverseHands;
    }

    @Override
    public float worldScale() {
        VRPlayer vrPlayer = VRPlayer.get();
        return vrPlayer == null ? 1.0F : vrPlayer.worldScale;
    }

    @Override
    public boolean isShaderActive() {
        return ShadersHelper.isShaderActive();
    }

    private static VRData data() {
        VRPlayer vrPlayer = VRPlayer.get();
        if (vrPlayer == null) {
            return null;
        }
        return vrPlayer.vrdata_world_render != null ? vrPlayer.vrdata_world_render : vrPlayer.getVRDataWorld();
    }

    private static VR.Aim pose(VRData.VRDevicePose pose) {
        if (pose == null) {
            return null;
        }
        Vector3f dir = pose.getDirection();
        return new VR.Aim(pose.getPosition(), new Vec3(dir.x, dir.y, dir.z));
    }
}
