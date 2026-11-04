package com.jegvr.mixin.server;

import com.jegvr.server.VrOffhand;
import com.jegvr.server.VrServerAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ttv.migami.jeg.event.FlashlightEvent;

/**
 * JEG's flashlight and laser pointer work every tick from the main-hand gun, along the player's view. During
 * {@link VrOffhand#attachmentsPass()} they read the off-hand gun instead, from its muzzle along its barrel.
 */
@Mixin(value = FlashlightEvent.class, remap = false)
public abstract class FlashlightEventVRMixin {
    @WrapOperation(method = {"flashlight", "laserPointer"}, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/player/Player;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack jegvr$offhandGun(Player player, Operation<ItemStack> original) {
        return VrOffhand.attachmentsPass() ? player.getOffhandItem() : original.call(player);
    }

    @WrapOperation(method = "flashlight", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/player/Player;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 jegvr$flashlightFromMuzzle(Player player, float partialTick, Operation<Vec3> original) {
        return VrOffhand.attachmentsPass() ? VrServerAim.muzzleFor(player, InteractionHand.OFF_HAND)
                : original.call(player, partialTick);
    }

    @WrapOperation(method = "flashlight", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/player/Player;getViewVector(F)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 jegvr$flashlightAlongBarrel(Player player, float partialTick, Operation<Vec3> original) {
        return VrOffhand.attachmentsPass() ? VrServerAim.directionFor(player, InteractionHand.OFF_HAND)
                : original.call(player, partialTick);
    }

    @WrapOperation(method = "laserPointer", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/player/Player;getEyePosition()Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 jegvr$laserFromMuzzle(Player player, Operation<Vec3> original) {
        return VrOffhand.attachmentsPass() ? VrServerAim.muzzleFor(player, InteractionHand.OFF_HAND) : original.call(player);
    }

    @WrapOperation(method = "laserPointer", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/player/Player;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 jegvr$laserAlongBarrel(Player player, Operation<Vec3> original) {
        return VrOffhand.attachmentsPass() ? VrServerAim.directionFor(player, InteractionHand.OFF_HAND) : original.call(player);
    }
}
