package com.jegvr.network;

import com.jegvr.server.VrServerThrow;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Speed of a grenade thrown with the throwing gesture (F9.26), sent with {@link C2SVrAim} right before the vanilla
 * "release use item" packet. Since protocol {@link VrNetwork#THROW_PROTOCOL}.
 */
public record C2SVrThrow(InteractionHand hand, float speed) {
    public static void encode(C2SVrThrow msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.hand);
        buf.writeFloat(msg.speed);
    }

    public static C2SVrThrow decode(FriendlyByteBuf buf) {
        return new C2SVrThrow(buf.readEnum(InteractionHand.class), buf.readFloat());
    }

    public static void handle(C2SVrThrow msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            VrServerThrow.update(player, msg.hand, msg.speed);
        }
    }
}
