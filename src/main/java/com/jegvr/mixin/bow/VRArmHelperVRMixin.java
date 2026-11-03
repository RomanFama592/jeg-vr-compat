package com.jegvr.mixin.bow;

import com.jegvr.client.VrBows;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.vivecraft.client_vr.render.helpers.VRArmHelper;

/**
 * F9.7: with a JEG bow in the roomscale bow, Vivecraft draws the bow in the bow hand and, until it is notched, the
 * arrow in the other; the arrow comes from {@code Player.getProjectile}, empty for a JEG gun.
 */
@Mixin(value = VRArmHelper.class, remap = false)
public abstract class VRArmHelperVRMixin {
    @WrapOperation(method = "getBowOverride", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/client/player/LocalPlayer;getProjectile(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack jegvr$jegArrow(LocalPlayer player, ItemStack bow, Operation<ItemStack> original) {
        return VrBows.isRoomscaleBow(bow) ? VrBows.ammo(player, bow) : original.call(player, bow);
    }
}
