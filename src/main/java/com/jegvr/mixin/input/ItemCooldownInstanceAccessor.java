package com.jegvr.mixin.input;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** {@code ItemCooldowns.CooldownInstance} is not public, so it is named by string. See {@link ItemCooldownsAccessor}. */
@Mixin(targets = "net.minecraft.world.item.ItemCooldowns$CooldownInstance")
public interface ItemCooldownInstanceAccessor {
    @Accessor("endTime")
    int jegvr$getEndTime();
}
