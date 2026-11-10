package com.jegvr.network;

import com.jegvr.config.VrRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * F9.8: the server's self-damage rule, sent next to {@link S2CVrRules} (whose format is fixed). Since protocol
 * {@link VrNetwork#SELF_DAMAGE_PROTOCOL}; only sent to clients that said they understand it.
 */
public record S2CVrSelfDamageRule(VrRules.Toggle rule) {
    public static void encode(S2CVrSelfDamageRule msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.rule);
    }

    public static S2CVrSelfDamageRule decode(FriendlyByteBuf buf) {
        return new S2CVrSelfDamageRule(buf.readEnum(VrRules.Toggle.class));
    }

    public static void handle(S2CVrSelfDamageRule msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.jegvr.client.VrRulesClient.receiveSelfDamage(msg.rule));
    }
}
