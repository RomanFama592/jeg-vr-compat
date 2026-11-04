package com.jegvr.mixin.input;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** How long a gun's cooldown has left: when a gun changes hands, its cooldown goes with it ({@code VrOffhandShooting}). */
@Mixin(ItemCooldowns.class)
public interface ItemCooldownsAccessor {
    @Accessor("cooldowns")
    Map<Item, ?> jegvr$getCooldowns();

    @Accessor("tickCount")
    int jegvr$getTickCount();
}
