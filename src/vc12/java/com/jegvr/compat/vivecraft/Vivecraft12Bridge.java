package com.jegvr.compat.vivecraft;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.vivecraft.api_beta.VivecraftAPI;
import org.vivecraft.api_beta.client.VivecraftClientAPI;
import org.vivecraft.client.network.ClientNetworking;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.render.RenderPass;

/** Vivecraft 1.2.x: its API is {@code api_beta}; the rest comes from internals. Compiled against 1.2.5. */
public final class Vivecraft12Bridge extends VivecraftBridge {
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
    public boolean isLeftEyePass() {
        return ClientDataHolderVR.getInstance().currentPass == RenderPass.LEFT;
    }

    @Override
    public boolean isEyePass() {
        RenderPass pass = ClientDataHolderVR.getInstance().currentPass;
        return pass != RenderPass.THIRD && pass != RenderPass.CAMERA;
    }

    @Override
    public boolean isDrawingMainHand() {
        return ClientDataHolderVR.IS_MAIN_HAND;
    }

    @Override
    public void setActiveHand(InteractionHand hand) {
        ClientNetworking.sendActiveHand(hand);
    }
}
