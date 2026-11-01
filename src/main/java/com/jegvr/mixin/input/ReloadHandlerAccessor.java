package com.jegvr.mixin.input;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import ttv.migami.jeg.client.handler.ReloadHandler;

/**
 * JEG cancels a reload when the hotbar slot differs from the one it reloads; an off-hand reload has to record the
 * current slot too, or JEG would cancel it on the next tick.
 */
@Mixin(value = ReloadHandler.class, remap = false)
public interface ReloadHandlerAccessor {
    @Accessor("reloadingSlot")
    void jegvr$setReloadingSlot(int slot);
}
