package com.jegvr.compat.vivecraft;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.vivecraft.api.VRAPI;
import org.vivecraft.api.client.VRClientAPI;
import org.vivecraft.api.client.VRRenderingAPI;
import org.vivecraft.api.client.data.RenderPass;
import org.vivecraft.api.data.VRBodyPart;
import org.vivecraft.client.network.ClientNetworking;
import org.vivecraft.client_vr.ClientDataHolderVR;

/**
 * Vivecraft 1.3.x: the official API ({@code org.vivecraft.api}) for what it offers, internals for the rest. Compiled
 * against 1.3.15. The poses stay on the internals shared with 1.2 ({@link VivecraftBridge}): the API has no controller
 * axes, and both lines must place a gun exactly alike.
 */
public final class Vivecraft13Bridge extends VivecraftBridge {
    @Override
    public String name() {
        return "Vivecraft 1.3";
    }

    @Override
    public boolean isActive() {
        return VRClientAPI.instance().isVRActive() && data() != null;
    }

    @Override
    public boolean isVRPlayer(Player player) {
        return VRAPI.instance().isVRPlayer(player);
    }

    @Override
    public boolean isLeftEyePass() {
        return VRRenderingAPI.instance().getCurrentRenderPass() == RenderPass.LEFT;
    }

    @Override
    public boolean isEyePass() {
        return !RenderPass.isThirdPerson(VRRenderingAPI.instance().getCurrentRenderPass());
    }

    @Override
    public boolean isDrawingMainHand() {
        return ClientDataHolderVR.getInstance().isMainHand;
    }

    @Override
    public void haptic(boolean mainHand, float seconds, float amplitude) {
        VRClientAPI.instance().triggerHapticPulse(mainHand ? VRBodyPart.MAIN_HAND : VRBodyPart.OFF_HAND, seconds, 160.0F,
                Math.max(0.0F, Math.min(1.0F, amplitude)), 0.0F);
    }

    /** As 1.2 did: the server takes both the position and the direction of what is thrown from that hand. */
    @Override
    public void setActiveHand(InteractionHand hand) {
        ClientNetworking.sendActiveHand(hand, true);
    }

    @Override
    public boolean isSeated() {
        return VRClientAPI.instance().isSeated();
    }

    @Override
    public boolean isLeftHanded() {
        return VRClientAPI.instance().isLeftHanded();
    }

    @Override
    public float worldScale() {
        return VRClientAPI.instance().getWorldScale();
    }
}
