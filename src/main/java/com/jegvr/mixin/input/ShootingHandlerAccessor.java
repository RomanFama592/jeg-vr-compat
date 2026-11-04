package com.jegvr.mixin.input;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import ttv.migami.jeg.client.handler.ShootingHandler;

/** The main-hand gun's heat: when a gun changes hands, its heat goes with it ({@code VrOffhandShooting}). */
@Mixin(value = ShootingHandler.class, remap = false)
public interface ShootingHandlerAccessor {
    @Accessor("overheatTimer")
    int jegvr$getOverheatTimer();

    @Accessor("overheatTimer")
    void jegvr$setOverheatTimer(int ticks);

    @Accessor("overheated")
    boolean jegvr$isOverheated();

    @Accessor("overheated")
    void jegvr$setOverheated(boolean overheated);
}
