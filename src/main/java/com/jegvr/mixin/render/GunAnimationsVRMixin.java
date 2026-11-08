package com.jegvr.mixin.render;

import com.jegvr.client.VrHands;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mrcrayfish.framework.api.sync.SyncedDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ttv.migami.jeg.animations.GunAnimations;
import ttv.migami.jeg.init.ModSyncedDataKeys;

/**
 * JEG's animation controller drives every animated gun from the main-hand gun's tags (and plays nothing when the
 * animated gun is not the main-hand item). While Vivecraft draws the off-hand gun, feed it the off-hand gun instead,
 * so it plays its own shoot/reload animations, keep the main-hand trigger's SHOOTING flag from charging it, and let
 * the per-player RELOADING flag animate only the gun that reloads.
 */
@Mixin(value = GunAnimations.class, remap = false)
public abstract class GunAnimationsVRMixin {
    /**
     * The controller's lambda, by pattern: javac numbers lambdas in order, and JEG's other classes already renumbered
     * theirs between versions. Mixin reads {@code /.../} as a regular expression.
     */
    private static final String CONTROLLER_LAMBDA = "/^lambda[$]animationController[$][0-9]+$/";

    @WrapOperation(method = CONTROLLER_LAMBDA, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/player/Player;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack jegvr$animatedHand(Player player, Operation<ItemStack> original) {
        return VrHands.animatedStack(player, original.call(player));
    }

    @WrapOperation(method = CONTROLLER_LAMBDA, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/nbt/CompoundTag;getBoolean(Ljava/lang/String;)Z"))
    private static boolean jegvr$tagFlag(CompoundTag tag, String key, Operation<Boolean> original) {
        Player player = Minecraft.getInstance().player;
        return player == null ? original.call(tag, key) : VrHands.tagFlag(player, key, original.call(tag, key));
    }

    @WrapOperation(method = CONTROLLER_LAMBDA, at = @At(value = "INVOKE",
            target = "Lcom/mrcrayfish/framework/api/sync/SyncedDataKey;getValue(Lnet/minecraft/world/entity/Entity;)Ljava/lang/Object;"))
    private static Object jegvr$shootingFlag(SyncedDataKey<?, ?> key, Entity entity, Operation<Object> original) {
        Object value = original.call(key, entity);
        if (value instanceof Boolean flag && entity instanceof Player player) {
            if (key == ModSyncedDataKeys.SHOOTING) {
                return VrHands.shootingFlag(player, flag);
            }
            if (key == ModSyncedDataKeys.RELOADING) {
                return VrHands.reloadingFlag(player, flag);
            }
        }
        return value;
    }
}
