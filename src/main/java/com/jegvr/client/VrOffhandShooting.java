package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.network.C2SVrShootOffhand;
import com.jegvr.network.VrNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ttv.migami.jeg.common.FireMode;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.util.GunEnchantmentHelper;
import ttv.migami.jeg.util.GunModifierHelper;

/**
 * Client side of the off-hand gun (base for dual wield, S.5): while {@link VrKeys#FIRE_OFFHAND} is held in VR, fires
 * the off-hand gun at its own rate through {@link C2SVrShootOffhand}. Only on servers with this addon. Automatic guns
 * fire while held; every other fire mode fires once per press.
 */
public final class VrOffhandShooting {
    private static int cooldown;
    private static boolean wasDown;

    private VrOffhandShooting() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean down = player != null && mc.screen == null && VrKeys.FIRE_OFFHAND.isDown();
        if (down && VR.active() && player.getOffhandItem().getItem() instanceof GunItem gunItem
                && VrAim.serverHasAddon()) {
            ItemStack stack = player.getOffhandItem();
            Gun gun = gunItem.getModifiedGun(stack);
            boolean automatic = FireMode.AUTOMATIC.equals(gun.getGeneral().getFireMode());
            if (cooldown == 0 && (automatic || !wasDown)) {
                fire(player, stack, gun);
            }
        }
        wasDown = down;
    }

    private static void fire(LocalPlayer player, ItemStack stack, Gun gun) {
        if (player.isSpectator() || (!Gun.hasAmmo(stack) && !player.isCreative())) {
            return;
        }
        float[] rotation = VrAim.offHandShotRotation();
        if (rotation == null) {
            return;
        }
        VrNetwork.CHANNEL.sendToServer(new C2SVrShootOffhand(rotation[0], rotation[1]));
        // Same rate JEG uses for the main hand (ShootingHandler.fire / ShootTracker.putCooldown).
        cooldown = Math.max(1, GunModifierHelper.getModifiedRate(stack, GunEnchantmentHelper.getRate(stack, gun)));

        float angle = gun.getGeneral().getRecoilAngle() * (1.0F - GunModifierHelper.getRecoilModifier(stack));
        VR.haptic(false, 0.06F, Mth.clamp(angle / 10.0F, 0.2F, 1.0F));
    }
}
