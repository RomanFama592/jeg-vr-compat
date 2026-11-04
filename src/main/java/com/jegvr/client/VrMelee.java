package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.network.C2SVrMeleeOffhand;
import com.jegvr.network.VrNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.AnimatedBowItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.network.PacketHandler;
import ttv.migami.jeg.network.message.C2SMessageMelee;

/**
 * F9.22: JEG's melee attack, done in VR by swinging the gun like a sword. Vivecraft's {@code SwingTracker} treats the gun
 * as a sword ({@code SwingTrackerVRMixin}), so reach and swing speed are the sword's, and when the swing hits something
 * this runs JEG's melee instead of a plain attack. It keeps JEG's damage, knockback, sound and cooldown.
 * <p>
 * JEG's melee hits a cone around where the player looks, so the player is turned to the target for that one message. On
 * a server without this addon that works too, for the main hand only; the off-hand gun needs {@link C2SVrMeleeOffhand}.
 */
public final class VrMelee {
    /** JEG's cooldown after a melee attack, and after one while sprinting ({@code ServerPlayHandler.handleMelee}). */
    private static final int COOLDOWN = 15;
    private static final int SPRINT_COOLDOWN = 40;
    /** JEG only looks for targets this far around the player. */
    private static final double JEG_RANGE = 2.0;

    private VrMelee() {
    }

    /** A gun with JEG's melee (the same exclusions as its {@code MeleeHandler}) in a hand that can use it. */
    public static boolean swingsLikeSword(ItemStack stack, boolean mainHand) {
        if (!VR.active() || !(stack.getItem() instanceof GunItem) || stack.getItem() instanceof AnimatedBowItem
                || stack.is(ModItems.MINIGUN.get())) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return (tag == null || !tag.getString("GunId").endsWith("blowpipe")) && (mainHand || VrAim.serverHasAddon());
    }

    /** The swing hit {@code target}. Does nothing while the gun cools down or if JEG would not reach the target. */
    public static void strike(LocalPlayer player, ItemStack gun, boolean mainHand, Entity target) {
        boolean cooling = mainHand ? player.getCooldowns().isOnCooldown(gun.getItem())
                : VrOffhandShooting.onCooldown(gun.getItem());
        if (cooling || !player.getBoundingBox().inflate(JEG_RANGE).intersects(target.getBoundingBox())) {
            return;
        }
        Vec3 toTarget = target.position().subtract(player.position()).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
        float pitch = (float) Math.toDegrees(-Math.asin(Mth.clamp(toTarget.y, -1.0, 1.0)));
        if (mainHand) {
            strikeMainHand(player, yaw, pitch);
        } else {
            VrNetwork.CHANNEL.sendToServer(new C2SVrMeleeOffhand(yaw, pitch));
            VrOffhandShooting.startCooldown(gun.getItem(), player.isSprinting() ? SPRINT_COOLDOWN : COOLDOWN);
        }
    }

    private static void strikeMainHand(LocalPlayer player, float yaw, float pitch) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return;
        }
        connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround()));
        PacketHandler.getPlayChannel().sendToServer(new C2SMessageMelee());
        connection.send(new ServerboundMovePlayerPacket.Rot(player.getYRot(), player.getXRot(), player.onGround()));
    }
}
