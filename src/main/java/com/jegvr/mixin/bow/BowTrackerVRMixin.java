package com.jegvr.mixin.bow;

import com.jegvr.client.VrBows;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vivecraft.client_vr.gameplay.trackers.BowTracker;

/**
 * F9.7: Vivecraft's roomscale bow also handles JEG's bows ({@link VrBows}). Vivecraft only counts items whose use
 * animation is the bow's; JEG's bows are guns.
 */
@Mixin(value = BowTracker.class, remap = false)
public abstract class BowTrackerVRMixin {
    @Inject(method = "isBow", at = @At("HEAD"), cancellable = true)
    private static void jegvr$jegBows(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (VrBows.isRoomscaleBow(stack)) {
            cir.setReturnValue(true);
        }
    }

    /** The arrow to notch: {@code Player.getProjectile} is empty for a JEG gun. */
    @WrapOperation(method = "doProcess", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/client/player/LocalPlayer;getProjectile(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jegvr$jegArrow(LocalPlayer player, ItemStack bow, Operation<ItemStack> original) {
        return VrBows.isRoomscaleBow(bow) ? VrBows.ammo(player, bow) : original.call(player, bow);
    }
}
