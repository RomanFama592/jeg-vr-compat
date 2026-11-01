package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.mixin.input.ReloadHandlerAccessor;
import com.jegvr.network.C2SVrReloadOffhand;
import com.jegvr.network.VrNetwork;
import com.jegvr.server.VrOffhand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import ttv.migami.jeg.client.KeyBinds;
import ttv.migami.jeg.client.handler.ReloadHandler;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.init.ModSyncedDataKeys;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.util.GunModifierHelper;

/**
 * Reloading in VR. JEG's reload key, and in VR also JEG's aim key (aiming down sights does not work in VR, see
 * {@code AimingHandlerVRMixin}), reload the gun whose barrel points up, the main-hand gun first when both do. If
 * neither points up it is the main-hand gun; with a gun only in the off hand, that one.
 */
public final class VrDualReload {
    /** Barrel at least ~30° above the horizon. */
    private static final double UP = 0.5;

    private static boolean aimWasDown;

    private VrDualReload() {
    }

    /** Called before JEG handles a key event; true when we reloaded the off-hand gun and JEG must skip the event. */
    public static boolean onKeyPressed(int action) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || action != GLFW.GLFW_PRESS || !KeyBinds.KEY_RELOAD.isDown() || !reloadsOffhand(player)) {
            return false;
        }
        reloadOffhand(player);
        return true;
    }

    /** The aim key, pressed in VR with a gun in hand, reloads instead. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean down = player != null && mc.screen == null && KeyBinds.getAimMapping().isDown() && VR.active();
        if (down && !aimWasDown) {
            if (reloadsOffhand(player)) {
                reloadOffhand(player);
            } else if (player.getMainHandItem().getItem() instanceof GunItem) {
                reloadMainHand(player);
            }
        }
        aimWasDown = down;
    }

    private static boolean reloadsOffhand(LocalPlayer player) {
        if (!VrOffhand.canDualWield(player.getOffhandItem()) || !VR.active() || !VrAim.serverHasAddon()) {
            return false;
        }
        boolean mainGun = player.getMainHandItem().getItem() instanceof GunItem;
        return !mainGun || (pointsUp(VR.offHandAim()) && !pointsUp(VR.mainHandAim()));
    }

    private static boolean pointsUp(VR.Aim aim) {
        return aim != null && aim.dir().lengthSqr() > 1.0E-6 && aim.dir().normalize().y >= UP;
    }

    /** What JEG's reload key does (ReloadHandler.onKeyPressed). */
    public static void reloadMainHand(LocalPlayer player) {
        CompoundTag tag = player.getMainHandItem().getTag();
        if (tag != null) {
            tag.putBoolean("IsReloading", true);
        }
        ReloadHandler.get().setReloading(true);
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
