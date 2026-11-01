package com.jegvr.network;

import com.jegvr.server.VrOffhand;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Fires the gun in the off hand. JEG only shoots the main hand, so this has no JEG counterpart (dual wield). */
public record C2SVrShootOffhand(float yaw, float pitch) {
    public static void encode(C2SVrShootOffhand msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.yaw);
        buf.writeFloat(msg.pitch);
    }

    public static C2SVrShootOffhand decode(FriendlyByteBuf buf) {
        return new C2SVrShootOffhand(buf.readFloat(), buf.readFloat());
    }

    public static void handle(C2SVrShootOffhand msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        // VrOffhand uses JEG classes: never load it on a server without JEG.
        if (player != null && ModList.get().isLoaded("jeg")) {
            VrOffhand.fire(player, msg.yaw, msg.pitch);
        }
    }
}
