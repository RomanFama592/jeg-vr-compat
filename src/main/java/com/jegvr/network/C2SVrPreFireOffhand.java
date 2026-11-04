package com.jegvr.network;

import com.jegvr.server.VrOffhand;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Plays the off-hand gun's wind-up sound (JEG's own message only reads the main-hand gun). */
public record C2SVrPreFireOffhand() {
    public static void encode(C2SVrPreFireOffhand msg, FriendlyByteBuf buf) {
    }

    public static C2SVrPreFireOffhand decode(FriendlyByteBuf buf) {
        return new C2SVrPreFireOffhand();
    }

    public static void handle(C2SVrPreFireOffhand msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        // VrOffhand uses JEG classes: never load it on a server without JEG.
        if (player != null && ModList.get().isLoaded("jeg")) {
            VrOffhand.preFireSound(player);
        }
    }
}
