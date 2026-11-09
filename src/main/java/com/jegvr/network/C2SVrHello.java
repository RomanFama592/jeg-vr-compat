package com.jegvr.network;

import com.jegvr.server.VrServerRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S.8: the client's addon protocol and whether it plays in VR, sent on joining and whenever VR is turned on or off. The
 * server answers with {@link S2CVrRules}, and knows who plays in VR (it cannot ask Vivecraft). Since protocol
 * {@link VrNetwork#RULES_PROTOCOL}.
 */
public record C2SVrHello(int protocol, boolean vr) {
    public static void encode(C2SVrHello msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.protocol);
        buf.writeBoolean(msg.vr);
    }

    public static C2SVrHello decode(FriendlyByteBuf buf) {
        return new C2SVrHello(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(C2SVrHello msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            VrServerRules.hello(player, msg.protocol, msg.vr);
        }
    }
}
