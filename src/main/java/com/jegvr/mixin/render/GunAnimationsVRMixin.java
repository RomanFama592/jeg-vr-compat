package com.jegvr.mixin.render;

import com.jegvr.client.VrHands;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mrcrayfish.framework.api.sync.SyncedDataKey;
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
 * so it plays its own shoot/reload animations, and keep the main-hand trigger's SHOOTING flag from charging it.
 */
@Mixin(value = GunAnimations.class, remap = false)
public abstract class GunAnimationsVRMixin {
    @WrapOperation(method = "lambda$animationController$0", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/player/Player;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack jegvr$animatedHand(Player player, Operation<ItemStack> original) {
        return VrHands.animatedStack(player, original.call(player));
    }

    @WrapOperation(method = "lambda$animationController$0", at = @At(value = "INVOKE",
            target = "Lcom/mrcrayfish/framework/api/sync/SyncedDataKey;getValue(Lnet/minecraft/world/entity/Entity;)Ljava/lang/Object;"))
    private static Object jegvr$shootingFlag(SyncedDataKey<?, ?> key, Entity entity, Operation<Object> original) {
        Object value = original.call(key, entity);
        if (key == ModSyncedDataKeys.SHOOTING && value instanceof Boolean shooting && entity instanceof Player player) {
            return VrHands.shootingFlag(player, shooting);
        }
        return value;
    }
}
