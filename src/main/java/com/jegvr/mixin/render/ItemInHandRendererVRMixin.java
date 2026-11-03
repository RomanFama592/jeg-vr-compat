package com.jegvr.mixin.render;

import com.jegvr.client.VrGunMirror;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vivecraft draws the held gun with {@code renderItem} right after its first-person transform ({@link
 * VivecraftItemRenderingVRMixin}). A mirrored gun ({@link VrGunMirror}) is drawn as a right-hand gun, through buffers
 * that undo the mirror's inside-out faces.
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererVRMixin {
    @ModifyVariable(method = "renderItem", at = @At("HEAD"), argsOnly = true)
    private ItemDisplayContext jegvr$mirroredContext(ItemDisplayContext context) {
        return VrGunMirror.pending() ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : context;
    }

    @ModifyVariable(method = "renderItem", at = @At("HEAD"), argsOnly = true)
    private boolean jegvr$mirroredLeftHand(boolean leftHand) {
        return !VrGunMirror.pending() && leftHand;
    }

    @ModifyVariable(method = "renderItem", at = @At("HEAD"), argsOnly = true)
    private MultiBufferSource jegvr$mirroredBuffers(MultiBufferSource buffers) {
        return VrGunMirror.pending() ? VrGunMirror.wrap(buffers) : buffers;
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void jegvr$mirrorDrawn(CallbackInfo ci) {
        VrGunMirror.drawn();
    }
}
