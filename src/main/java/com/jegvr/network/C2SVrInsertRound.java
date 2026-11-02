package com.jegvr.network;

import com.jegvr.server.VrInsertRound;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Loads one round by hand into the gun in {@code hand} (F7.8); JEG has no single-round message. */
public record C2SVrInsertRound(InteractionHand hand) {
    public static void encode(C2SVrInsertRound msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.hand);
    }

    public static C2SVrInsertRound decode(FriendlyByteBuf buf) {
        return new C2SVrInsertRound(buf.readEnum(InteractionHand.class));
    }

    public static void handle(C2SVrInsertRound msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        // VrInsertRound uses JEG classes: never load it on a server without JEG.
        if (player != null && ModList.get().isLoaded("jeg")) {
            VrInsertRound.insert(player, msg.hand);
        }
    }
}
