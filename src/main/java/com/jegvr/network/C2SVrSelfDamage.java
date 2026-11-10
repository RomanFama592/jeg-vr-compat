package com.jegvr.network;

import com.jegvr.server.VrSelfDamage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * F9.8: the player's own "self-damage" option, sent on joining and whenever it changes. The server only follows it
 * while its rule is {@code PLAYER}. Since protocol {@link VrNetwork#SELF_DAMAGE_PROTOCOL}.
 */
public record C2SVrSelfDamage(boolean enabled) {
    public static void encode(C2SVrSelfDamage msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.enabled);
    }

    public static C2SVrSelfDamage decode(FriendlyByteBuf buf) {
        return new C2SVrSelfDamage(buf.readBoolean());
    }

    public static void handle(C2SVrSelfDamage msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            VrSelfDamage.choose(player, msg.enabled);
        }
    }
}
