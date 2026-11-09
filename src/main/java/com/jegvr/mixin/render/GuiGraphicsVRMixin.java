package com.jegvr.mixin.render;

import com.jegvr.client.VrBlindOverlay;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * F9.27.3: in VR, skips JEG's full-window stun/smoke fill on the HUD, since each eye shows it already. JEG draws it
 * from a method its own mixin adds to {@code GameRenderer}, which Mixin cannot target, so the fill itself is matched:
 * the whole window from (0, 0) in one of JEG's two colours, while the player has one of those effects.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsVRMixin {
    @Inject(method = "fill(IIIII)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void jegvr$skipJegOverlay(int x1, int y1, int x2, int y2, int color, CallbackInfo ci) {
        if (VrBlindOverlay.skipHudFill(x1, y1, x2, y2, color)) {
            ci.cancel();
        }
    }
}
