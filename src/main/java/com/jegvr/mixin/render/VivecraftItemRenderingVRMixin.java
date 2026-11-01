package com.jegvr.mixin.render;

import com.jegvr.client.VrDiag;
import com.jegvr.client.VrGunPose;
import com.jegvr.client.VrHands;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.render.VivecraftItemRendering;
import ttv.migami.jeg.item.GunItem;

/**
 * Vivecraft rotates every held item by a per-type transform designed for sprites, tools and swords. JEG guns are
 * modelled with the barrel along -Z, which is exactly the controller's forward axis, so for guns we skip that
 * transform and keep the bare controller frame.
 */
@Mixin(value = VivecraftItemRendering.class, remap = false)
public class VivecraftItemRenderingVRMixin {
    @Inject(method = "applyFirstPersonItemTransforms", at = @At("HEAD"), cancellable = true)
    private static void jegvr$gunsUseControllerFrame(PoseStack poseStack,
                                                     VivecraftItemRendering.VivecraftItemTransformType type,
                                                     boolean rightHand, AbstractClientPlayer player,
                                                     float equipProgress, float swingProgress, ItemStack stack,
                                                     InteractionHand hand, CallbackInfo ci) {
        if (stack.getItem() instanceof GunItem) {
            VrDiag.once("vivecraftTransform", "Vivecraft first-person item transform replaced for JEG guns (type={})", type);
            VrHands.setRenderingHand(hand);
            VrGunPose.apply(poseStack, stack,
                    rightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                    hand == InteractionHand.MAIN_HAND);
            ci.cancel();
        }
    }
}
