package com.jegvr.mixin.render;

import com.jegvr.server.VrOffhand;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ttv.migami.jeg.client.screen.AttachmentScreen;

/** JEG's attachments screen shows the main-hand gun; opened for the off-hand gun, it shows that one. */
@Mixin(value = AttachmentScreen.class, remap = false)
public abstract class AttachmentScreenVRMixin {
    // Vanilla method names, overridden by JEG: the first of each pair exists in development, the second in the game.
    @WrapOperation(method = {"containerTick", "m_181908_", "renderLabels", "m_280003_", "renderBg", "m_7286_"},
            at = @At(value = "INVOKE", remap = true,
                    target = "Lnet/minecraft/client/player/LocalPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jegvr$shownGun(LocalPlayer player, Operation<ItemStack> original) {
        return VrOffhand.clientMenuForOffhand() ? player.getOffhandItem() : original.call(player);
    }
}
