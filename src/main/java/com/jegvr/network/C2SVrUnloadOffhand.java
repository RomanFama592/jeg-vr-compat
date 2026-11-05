package com.jegvr.network;

import com.jegvr.server.VrOffhand;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Unloads the gun in the off hand (JEG's own message only handles the main hand). */
public record C2SVrUnloadOffhand() {
    public static void encode(C2SVrUnloadOffhand msg, FriendlyByteBuf buf) {
    }

    public static C2SVrUnloadOffhand decode(FriendlyByteBuf buf) {
        return new C2SVrUnloadOffhand();
    }

    public static void handle(C2SVrUnloadOffhand msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        // VrOffhand uses JEG classes: never load it on a server without JEG.
        if (player != null && ModList.get().isLoaded("jeg")) {
            VrOffhand.unload(player);
        }
    }
}
