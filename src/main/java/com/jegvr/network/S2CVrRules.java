package com.jegvr.network;

import com.jegvr.config.VrRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S.8: the options the server decides, sent in answer to {@link C2SVrHello} and again when the server's file changes.
 * Since protocol {@link VrNetwork#RULES_PROTOCOL}; only sent to clients that said they understand it.
 */
public record S2CVrRules(VrRules.Rules rules) {
    public static void encode(S2CVrRules msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.rules.reloadWithBothGuns());
        buf.writeEnum(msg.rules.throwMode());
        buf.writeEnum(msg.rules.bowLaser());
    }

    public static S2CVrRules decode(FriendlyByteBuf buf) {
        return new S2CVrRules(new VrRules.Rules(buf.readEnum(VrRules.Toggle.class), buf.readEnum(VrRules.Throw.class),
                buf.readEnum(VrRules.Toggle.class)));
    }

    public static void handle(S2CVrRules msg, Supplier<NetworkEvent.Context> ctx) {
        // The client class is only touched on the client (a dedicated server never runs this).
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.jegvr.client.VrRulesClient.receive(msg.rules));
        // Network-thread handlers must say so, or Forge reports the packet as unknown (a warning in the log).
        ctx.get().setPacketHandled(true);
    }
}
