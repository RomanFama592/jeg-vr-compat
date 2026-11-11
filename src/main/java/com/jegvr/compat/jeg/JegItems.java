package com.jegvr.compat.jeg;

import com.jegvr.compat.Versions;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.init.ModItems;

/**
 * Kinds of JEG items that only some supported versions have. The addon is compiled against the oldest JEG, where the
 * classes of the newer ones do not exist, so they are recognised by name. Only loaded when JEG is present.
 */
public final class JegItems {
    /** JEG 0.14: chainsaw and jackhammer. They "shoot" by mining what the player looks at, with no projectile. */
    private static final String HARVESTING_GUN = "ttv.migami.jeg.item.HarvestingGunItem";
    private static final String BOOK_OF_PSALMS = "ttv.migami.jeg.item.AnimatedBookOfPsalmsItem";

    private JegItems() {
    }

    public static boolean isHarvestingGun(Item item) {
        return Versions.jegHasHarvesting() && isA(item, HARVESTING_GUN);
    }

    /**
     * What JEG 0.14 added to the guns that have no melee attack ({@code MeleeHandler}): mining guns, the Book of Psalms
     * and the finger gun. False with an older JEG, which lets the finger gun hit.
     */
    public static boolean meleeExcludedSince014(ItemStack stack) {
        if (!Versions.jegHeatsPerShot()) {
            return false;
        }
        Item item = stack.getItem();
        return isA(item, HARVESTING_GUN) || isA(item, BOOK_OF_PSALMS) || stack.is(ModItems.FINGER_GUN.get());
    }

    private static boolean isA(Item item, String className) {
        for (Class<?> type = item.getClass(); type != null && type != Item.class; type = type.getSuperclass()) {
            if (type.getName().equals(className)) {
                return true;
            }
        }
        return false;
    }
}
