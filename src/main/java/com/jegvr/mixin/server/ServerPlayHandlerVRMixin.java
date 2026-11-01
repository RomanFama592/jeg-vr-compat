package com.jegvr.mixin.server;

import com.jegvr.server.VrOffhand;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.common.ReloadTracker;
import ttv.migami.jeg.common.ShootTracker;
import ttv.migami.jeg.common.network.ServerPlayHandler;

/**
 * {@code handleShoot} always fires {@code getItemInHand(MAIN_HAND)}. During a {@link VrOffhand} it reads the off
 * hand instead, with a separate fire-rate tracker. Normal shots are untouched.
 */
@Mixin(value = ServerPlayHandler.class, remap = false)
public abstract class ServerPlayHandlerVRMixin {
    @WrapOperation(method = "handleShoot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;",
            remap = true))
    private static ItemStack jegvr$offhandGun(ServerPlayer player, InteractionHand hand,
                                              Operation<ItemStack> original) {
        return original.call(player, VrOffhand.firing() ? InteractionHand.OFF_HAND : hand);
    }

    @WrapOperation(method = "handleShoot", at = @At(value = "INVOKE",
            target = "Lttv/migami/jeg/common/ShootTracker;getShootTracker(Lnet/minecraft/world/entity/player/Player;)Lttv/migami/jeg/common/ShootTracker;"))
    private static ShootTracker jegvr$offhandTracker(Player player, Operation<ShootTracker> original) {
        return VrOffhand.firing() ? VrOffhand.tracker(player) : original.call(player);
    }

    /** {@code inventoryFeed} refills the main-hand gun; skip it for off-hand shots rather than feed the wrong gun. */
    @WrapWithCondition(method = "handleShoot", at = @At(value = "INVOKE",
            target = "Lttv/migami/jeg/common/ReloadTracker;inventoryFeed(Lnet/minecraft/world/entity/player/Player;Lttv/migami/jeg/common/Gun;)V"))
    private static boolean jegvr$skipMainHandFeed(Player player, Gun gun) {
        return !VrOffhand.firing();
    }
}
