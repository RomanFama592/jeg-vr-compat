package com.jegvr.mixin.render;

import com.jegvr.client.VrHandFollow;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.provider.ControllerType;
import org.vivecraft.client_vr.render.VRArmRenderer;

/** Phase H: Vivecraft's first-person hand, at the frame it would use; {@link VrHandFollow} may draw it elsewhere. */
@Mixin(value = VRArmRenderer.class, remap = false)
public abstract class VRArmRendererVRMixin {
    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void jegvr$followAnimation(ControllerType side, PoseStack poseStack, MultiBufferSource buffer, int light,
                                       AbstractClientPlayer player, ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        if (VrHandFollow.onVivecraftHand(ClientDataHolderVR.IS_MAIN_HAND, (PlayerRenderer) (Object) this,
                side == ControllerType.RIGHT, poseStack, light, player)) {
            ci.cancel();
        }
    }
}
