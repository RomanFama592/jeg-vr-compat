package com.jegvr.network;

import com.jegvr.server.VrOffhand;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** JEG's melee attack with the off-hand gun, turned towards the target (JEG's own message only uses the main-hand gun). */
public record C2SVrMeleeOffhand(float yaw, float pitch) {
    public static void encode(C2SVrMeleeOffhand msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.yaw);
        buf.writeFloat(msg.pitch);
    }

    public static C2SVrMeleeOffhand decode(FriendlyByteBuf buf) {
        return new C2SVrMeleeOffhand(buf.readFloat(), buf.readFloat());
    }

    public static void handle(C2SVrMeleeOffhand msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        // VrOffhand uses JEG classes: never load it on a server without JEG.
        if (player != null && ModList.get().isLoaded("jeg")) {
            VrOffhand.melee(player, msg.yaw, msg.pitch);
        }
    }
}
