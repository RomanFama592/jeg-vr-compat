package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.config.VrClientConfig;
import com.jegvr.network.C2SVrHead;
import com.jegvr.network.VrNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ttv.migami.jeg.entity.throwable.ThrowableStunGrenadeEntity;

/**
 * F9.31: while a stun grenade is near, tells the server where the head looks ({@link C2SVrHead}), so that turning the
 * head away saves the player and pointing the hand at the grenade does not blind them. Only to a server whose addon
 * knows the packet; elsewhere JEG keeps measuring the player's rotation, which in VR is the main controller's aim.
 */
public final class VrStunLook {
    /** Further than any reach JEG's config is likely to give the grenade (15 blocks by default). */
    private static final double RANGE = 32.0;

    private VrStunLook() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (event.phase != TickEvent.Phase.END || player == null || !VrClientConfig.stunByHead() || !VR.active()) {
            return;
        }
        ClientPacketListener connection = mc.getConnection();
        if (connection == null || VrNetwork.serverProtocol(connection.getConnection()) < VrNetwork.HEAD_PROTOCOL) {
            return;
        }
        VR.Aim head = VR.hmdAim();
        if (head == null || player.level().getEntitiesOfClass(ThrowableStunGrenadeEntity.class,
                player.getBoundingBox().inflate(RANGE)).isEmpty()) {
            return;
        }
        VrNetwork.CHANNEL.sendToServer(new C2SVrHead(head.dir()));
    }
}
