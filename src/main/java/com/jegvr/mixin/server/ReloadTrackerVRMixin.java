package com.jegvr.mixin.server;

import com.jegvr.server.VrOffhand;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ttv.migami.jeg.common.ReloadTracker;

/**
 * JEG's reload always works on the main-hand gun ({@code getMainHandItem} / {@code Inventory.getSelected}). While
 * {@link VrOffhand#reloading} is true for the player, the same code reloads the off-hand gun instead.
 * {@code inventoryFeed} is left alone: it runs on every main-hand shot.
 */
@Mixin(value = ReloadTracker.class, remap = false)
public abstract class ReloadTrackerVRMixin {
    private static final String ON_TICK = "onPlayerTick(Lnet/minecraftforge/event/TickEvent$PlayerTickEvent;)V";

    private static final String MAIN_HAND =
            "Lnet/minecraft/world/entity/player/Player;getMainHandItem()Lnet/minecraft/world/item/ItemStack;";
    private static final String SELECTED =
            "Lnet/minecraft/world/entity/player/Inventory;getSelected()Lnet/minecraft/world/item/ItemStack;";

    @WrapOperation(method = {"canReload", "shrinkFromAmmoPool"},
            at = @At(value = "INVOKE", target = MAIN_HAND, remap = true))
    private ItemStack jegvr$offhandGun(Player player, Operation<ItemStack> original) {
        return mainOrOffhand(player, original);
    }

    @WrapOperation(method = {"playReloadSound", ON_TICK},
            at = @At(value = "INVOKE", target = MAIN_HAND, remap = true))
    private static ItemStack jegvr$offhandGunStatic(Player player, Operation<ItemStack> original) {
        return mainOrOffhand(player, original);
    }

    @WrapOperation(method = {"<init>", "isSameWeapon"},
            at = @At(value = "INVOKE", target = SELECTED, remap = true))
    private ItemStack jegvr$offhandSelected(Inventory inventory, Operation<ItemStack> original) {
        return selectedOrOffhand(inventory, original);
    }

    @WrapOperation(method = ON_TICK, at = @At(value = "INVOKE", target = SELECTED, remap = true))
    private static ItemStack jegvr$offhandSelectedStatic(Inventory inventory, Operation<ItemStack> original) {
        return selectedOrOffhand(inventory, original);
    }

    private static ItemStack mainOrOffhand(Player player, Operation<ItemStack> original) {
        return VrOffhand.reloading(player) ? player.getOffhandItem() : original.call(player);
    }

    private static ItemStack selectedOrOffhand(Inventory inventory, Operation<ItemStack> original) {
        return VrOffhand.reloading(inventory.player) ? inventory.player.getOffhandItem() : original.call(inventory);
    }
}
