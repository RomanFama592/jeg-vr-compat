package com.jegvr.mixin.input;

import com.jegvr.client.VrMelee;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.vivecraft.client_vr.gameplay.trackers.SwingTracker;

/**
 * F9.22: Vivecraft recognises swords by class or by item tag. The tag comes from the server's data, which a server with
 * only JEG would not have, so here JEG's guns simply answer yes for it. What the swing hits gets JEG's melee ({@link VrMelee}).
 * The swing is {@code doProcess} in Vivecraft 1.2 and {@code activeProcess} in 1.3, with the same locals; the class
 * holding the tag changed name, so the tag is recognised by its id.
 */
@Mixin(value = SwingTracker.class, remap = false)
public abstract class SwingTrackerVRMixin {
    private static final ResourceLocation SWORDS_TAG = new ResourceLocation("vivecraft", "swords");

    @WrapOperation(method = {"doProcess", "activeProcess"}, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean jegvr$gunsAreSwords(ItemStack stack, TagKey<Item> tag, Operation<Boolean> original,
                                        @Local(name = "c") int controller) {
        return (SWORDS_TAG.equals(tag.location()) && VrMelee.swingsLikeSword(stack, controller == 0))
                || original.call(stack, tag);
    }

    @WrapOperation(method = {"doProcess", "activeProcess"}, at = @At(value = "INVOKE", remap = true,
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
