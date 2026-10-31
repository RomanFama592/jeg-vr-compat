package com.jegvr.mixin.render;

import com.jegvr.client.VrDiag;
import com.jegvr.compat.VR;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ttv.migami.jeg.item.GunItem;

/**
 * JEG gun models carry a {@code firstperson_*} display offset that places the gun on a flat screen (every gun shifts
 * it left/forward). In VR the gun already sits in the controller frame, so first-person guns use no display transform.
 */
@Mixin(ItemRenderer.class)
public class ItemRendererVRMixin {
    @WrapOperation(method = "render",
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/minecraftforge/client/ForgeHooksClient;handleCameraTransforms(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemDisplayContext;Z)Lnet/minecraft/client/resources/model/BakedModel;"))
    private BakedModel jegvr$noFlatDisplayOffset(PoseStack poseStack, BakedModel model, ItemDisplayContext context,
                                                 boolean leftHand, Operation<BakedModel> original,
                                                 @Local(argsOnly = true) ItemStack stack) {
        if (context.firstPerson() && stack.getItem() instanceof GunItem && VR.active()) {
            VrDiag.once("displayOffset", "Skipping flat first-person display transform for JEG guns (context={}, leftHand={})",
                    context, leftHand);
            return original.call(poseStack, model, ItemDisplayContext.NONE, leftHand);
        }
        return original.call(poseStack, model, context, leftHand);
    }
}
