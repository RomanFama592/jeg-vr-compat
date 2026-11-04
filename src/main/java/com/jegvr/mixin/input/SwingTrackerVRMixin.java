package com.jegvr.mixin.input;

import com.jegvr.client.VrMelee;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.vivecraft.client_vr.gameplay.trackers.SwingTracker;
import org.vivecraft.data.ItemTags;

/**
 * F9.22: Vivecraft recognises swords by class or by item tag. The tag comes from the server's data, which a server with
 * only JEG would not have, so here JEG's guns simply answer yes for it. What the swing hits gets JEG's melee ({@link VrMelee}).
 */
@Mixin(value = SwingTracker.class, remap = false)
public abstract class SwingTrackerVRMixin {
    @WrapOperation(method = "doProcess", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean jegvr$gunsAreSwords(ItemStack stack, TagKey<Item> tag, Operation<Boolean> original,
                                        @Local(name = "c") int controller) {
        return (tag == ItemTags.VIVECRAFT_SWORDS && VrMelee.swingsLikeSword(stack, controller == 0))
                || original.call(stack, tag);
    }

    @WrapOperation(method = "doProcess", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V"))
    private void jegvr$jegMelee(MultiPlayerGameMode gameMode, Player player, Entity target, Operation<Void> original,
                                @Local(name = "itemstack") ItemStack stack, @Local(name = "c") int controller) {
        if (target instanceof LivingEntity && VrMelee.swingsLikeSword(stack, controller == 0)) {
            VrMelee.strike((LocalPlayer) player, stack, controller == 0, target);
        } else {
            original.call(gameMode, player, target);
        }
    }
}
