package com.jegvr.mixin.render;

import com.jegvr.compat.VR;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.client.handler.CrosshairHandler;
import ttv.migami.jeg.item.GunItem;

/**
 * In VR the GUI is a floating panel, so a screen-centre crosshair only clutters it: skip JEG's crosshair and keep the
 * vanilla one hidden too while a gun is held.
 */
@Mixin(value = CrosshairHandler.class, remap = false)
public class CrosshairHandlerVRMixin {
    @Inject(method = "onRenderOverlay", at = @At("HEAD"), cancellable = true)
    private void jegvr$noScreenCrosshair(RenderGuiOverlayEvent.Pre event, CallbackInfo ci) {
        if (!VR.active()) {
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player != null && (player.getMainHandItem().getItem() instanceof GunItem
                || player.getOffhandItem().getItem() instanceof GunItem)
                && event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
            event.setCanceled(true);
        }
        ci.cancel();
    }
}
