package com.jegvr.network;

import com.jegvr.server.VrOffhand;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Reloads the gun in the off hand (JEG's own reload only handles the main hand). */
public record C2SVrReloadOffhand() {
    public static void encode(C2SVrReloadOffhand msg, FriendlyByteBuf buf) {
    }

    public static C2SVrReloadOffhand decode(FriendlyByteBuf buf) {
        return new C2SVrReloadOffhand();
    }

    public static void handle(C2SVrReloadOffhand msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        // VrOffhand uses JEG classes: never load it on a server without JEG.
        if (player != null && ModList.get().isLoaded("jeg")) {
            VrOffhand.startReload(player);
        }
    }
}
