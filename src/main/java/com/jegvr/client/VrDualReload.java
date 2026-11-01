package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.mixin.input.ReloadHandlerAccessor;
import com.jegvr.network.C2SVrReloadOffhand;
import com.jegvr.network.VrNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import ttv.migami.jeg.client.KeyBinds;
import ttv.migami.jeg.client.handler.ReloadHandler;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.init.ModSyncedDataKeys;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.util.GunModifierHelper;

/**
 * JEG's reload key, with a gun in each hand in VR: it reloads the gun whose barrel points up, the main-hand gun first
 * when both do. If neither points up it stays JEG's main-hand reload; with a gun only in the off hand, that one.
 */
public final class VrDualReload {
    /** Barrel at least ~30° above the horizon. */
    private static final double UP = 0.5;

    private VrDualReload() {
    }

    /** Called before JEG handles a key event; true when we reloaded the off-hand gun and JEG must skip the event. */
    public static boolean onKeyPressed(int action) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || action != GLFW.GLFW_PRESS || !KeyBinds.KEY_RELOAD.isDown()
                || !(player.getOffhandItem().getItem() instanceof GunItem) || !VR.active()
                || !VrAim.serverHasAddon()) {
            return false;
        }
        boolean mainGun = player.getMainHandItem().getItem() instanceof GunItem;
        if (mainGun && (pointsUp(VR.mainHandAim()) || !pointsUp(VR.offHandAim()))) {
            return false;
        }
        reloadOffhand(player);
        return true;
    }

    private static boolean pointsUp(VR.Aim aim) {
        return aim != null && aim.dir().lengthSqr() > 1.0E-6 && aim.dir().normalize().y >= UP;
    }

    private static void reloadOffhand(LocalPlayer player) {
        ItemStack stack = player.getOffhandItem();
        GunItem gunItem = (GunItem) stack.getItem();
        Gun gun = gunItem.getModifiedGun(stack);
        CompoundTag tag = stack.getTag();
        if (ModSyncedDataKeys.RELOADING.getValue(player) || tag == null || tag.getBoolean("IgnoreAmmo")
                || tag.getInt("AmmoCount") >= GunModifierHelper.getModifiedAmmoCapacity(stack, gun)) {
            return;
        }
        // Mirror JEG's ReloadHandler.setReloading(true): predict the flag, start the reload animation, and record the
        // slot so JEG's slot check does not cancel it.
        tag.putBoolean("IsReloading", true);
        ModSyncedDataKeys.RELOADING.setValue(player, true);
        ((ReloadHandlerAccessor) ReloadHandler.get()).jegvr$setReloadingSlot(player.getInventory().selected);
        VrNetwork.CHANNEL.sendToServer(new C2SVrReloadOffhand());
    }
}
