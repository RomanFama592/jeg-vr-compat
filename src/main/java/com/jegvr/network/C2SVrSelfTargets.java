package com.jegvr.network;

import com.jegvr.server.VrSelfDamage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * F9.8: where the shooter's headset and hands are, sent right before a shot while self-damage is on (the player's own
 * option, or the server's rule). A hand is null when it may not be hit (both hands while holding a gun with both
 * hands: the free one is on the handguard). Since protocol {@link VrNetwork#SELF_DAMAGE_PROTOCOL}.
 */
public record C2SVrSelfTargets(Vec3 eyes, Vec3 look, @Nullable Vec3 mainHand, @Nullable Vec3 offHand, float scale) {
    public static void encode(C2SVrSelfTargets msg, FriendlyByteBuf buf) {
        writeVec(buf, msg.eyes);
        writeVec(buf, msg.look);
        writeOptionalVec(buf, msg.mainHand);
        writeOptionalVec(buf, msg.offHand);
        buf.writeFloat(msg.scale);
    }

    public static C2SVrSelfTargets decode(FriendlyByteBuf buf) {
        return new C2SVrSelfTargets(readVec(buf), readVec(buf), readOptionalVec(buf), readOptionalVec(buf),
                buf.readFloat());
    }

    public static void handle(C2SVrSelfTargets msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            VrSelfDamage.update(player, msg.eyes, msg.look, msg.mainHand, msg.offHand, msg.scale);
        }
    }

    private static void writeOptionalVec(FriendlyByteBuf buf, @Nullable Vec3 v) {
        buf.writeBoolean(v != null);
        if (v != null) {
            writeVec(buf, v);
        }
    }

    @Nullable
    private static Vec3 readOptionalVec(FriendlyByteBuf buf) {
        return buf.readBoolean() ? readVec(buf) : null;
    }

    private static void writeVec(FriendlyByteBuf buf, Vec3 v) {
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
        buf.writeDouble(v.z);
    }

    private static Vec3 readVec(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}
