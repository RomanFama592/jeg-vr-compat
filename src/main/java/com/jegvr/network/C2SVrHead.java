package com.jegvr.network;

import com.jegvr.server.VrServerHead;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Where a VR player's head looks, sent every tick while a stun grenade is near (F9.31): Vivecraft turns the player
 * towards the main controller's crosshair, so the server does not know it otherwise.
 */
public record C2SVrHead(Vec3 look) {
    public static void encode(C2SVrHead msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.look.x);
        buf.writeDouble(msg.look.y);
        buf.writeDouble(msg.look.z);
    }

    public static C2SVrHead decode(FriendlyByteBuf buf) {
        return new C2SVrHead(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    public static void handle(C2SVrHead msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            VrServerHead.update(player, msg.look);
        }
    }
}
