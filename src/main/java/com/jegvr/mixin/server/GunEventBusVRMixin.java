package com.jegvr.mixin.server;

import com.jegvr.server.VrOffhand;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ttv.migami.jeg.event.GunEventBus;

/**
 * JEG's shot listeners read the gun from the shooter's main hand, not from the event: the pre-shot checks, durability,
 * casings and the special shots (the sonic blast of the Supersonic Shotgun and Hypersonic Cannon is fired here, not as
 * a projectile). While the server processes an off-hand shot ({@link VrOffhand#firing()}), give them the off-hand gun,
 * so each hand fires its own gun. Outside off-hand shots nothing changes.
 */
@Mixin(value = GunEventBus.class, remap = false)
public abstract class GunEventBusVRMixin {
    @WrapOperation(method = {"preShoot", "postShoot"}, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/player/Player;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack jegvr$firingGun(Player player, Operation<ItemStack> original) {
        return firingHand(player, original.call(player));
    }

    @WrapOperation(method = {"ejectCasing", "specialEffects"}, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/LivingEntity;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack jegvr$firingGunEffects(LivingEntity entity, Operation<ItemStack> original) {
        return firingHand(entity, original.call(entity));
    }

    private static ItemStack firingHand(LivingEntity entity, ItemStack mainHand) {
        // firing() is only set on the server thread; in single player the client thread must not see it.
        return VrOffhand.firing() && !entity.level().isClientSide ? entity.getOffhandItem() : mainHand;
    }
}
