package com.jegvr.mixin.server;

import com.jegvr.server.VrOffhand;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ttv.migami.jeg.common.container.AttachmentContainer;

/** JEG's attachments menu edits the selected item; opened for the off-hand gun, it edits that one, on both sides. */
@Mixin(value = AttachmentContainer.class, remap = false)
public abstract class AttachmentContainerVRMixin {
    @WrapOperation(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;)V", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/player/Inventory;getSelected()Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack jegvr$weapon(Inventory inventory, Operation<ItemStack> original) {
        return VrOffhand.menuForOffhand(inventory.player) ? inventory.offhand.get(0) : original.call(inventory);
    }
}
