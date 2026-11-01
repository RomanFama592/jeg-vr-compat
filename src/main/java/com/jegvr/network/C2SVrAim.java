package com.jegvr.network;

import com.jegvr.server.VrServerAim;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Muzzle position and barrel direction of a VR player's gun, sent right before JEG's shoot message. */
public record C2SVrAim(InteractionHand hand, Vec3 muzzle, Vec3 direction) {
    public static void encode(C2SVrAim msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.hand);
        writeVec(buf, msg.muzzle);
        writeVec(buf, msg.direction);
    }

    public static C2SVrAim decode(FriendlyByteBuf buf) {
        return new C2SVrAim(buf.readEnum(InteractionHand.class), readVec(buf), readVec(buf));
    }

    public static void handle(C2SVrAim msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            VrServerAim.update(player, msg.hand, msg.muzzle, msg.direction);
        }
    }

    private static void writeVec(FriendlyByteBuf buf, Vec3 v) {
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
        buf.writeDouble(v.z);
    }

    private static Vec3 readVec(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}
