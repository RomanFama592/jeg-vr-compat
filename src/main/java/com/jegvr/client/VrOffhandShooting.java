package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.mixin.input.KeyMappingAccessor;
import com.jegvr.network.C2SVrShootOffhand;
import com.jegvr.network.VrNetwork;
import com.jegvr.server.VrOffhand;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ttv.migami.jeg.client.handler.GunRenderingHandler;
import ttv.migami.jeg.common.FireMode;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.event.GunFireEvent;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.util.GunEnchantmentHelper;
import ttv.migami.jeg.util.GunModifierHelper;

/**
 * Client side of the off-hand gun (dual wield, S.5). With a gun in the off hand, in VR and on a server with this
 * addon, that hand's trigger (Vivecraft's teleport key, see {@code KeyMappingVRMixin}) or
 * {@link VrKeys#FIRE_OFFHAND} fires it at its own rate through {@link C2SVrShootOffhand}. Automatic guns fire while
 * held; every other fire mode fires once per press. Charged guns (bows) are not fired from the off hand, and the
 * trigger keeps its normal role while the main hand holds a Vivecraft bow (drawn with that trigger).
 */
public final class VrOffhandShooting {
    private static int cooldown;
    private static boolean wasDown;
    private static boolean triggerTakenOver;

    private VrOffhandShooting() {
    }

    /** True while the off-hand trigger fires the off-hand gun instead of teleporting. */
    public static boolean triggerTakenOver() {
        return triggerTakenOver;
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
        triggerTakenOver = player != null && VrOffhand.canDualWield(player.getOffhandItem())
                && !VR.isBow(player.getMainHandItem()) && VR.active() && VrAim.serverHasAddon();
        boolean down = triggerTakenOver && mc.screen == null && (VrKeys.FIRE_OFFHAND.isDown() || teleportPressed());
        if (down && player.getOffhandItem().getItem() instanceof GunItem gunItem) {
            ItemStack stack = player.getOffhandItem();
            Gun gun = gunItem.getModifiedGun(stack);
            boolean automatic = FireMode.AUTOMATIC.equals(gun.getGeneral().getFireMode());
            if (cooldown == 0 && (automatic || !wasDown)) {
                fire(player, stack, gun);
            }
        }
        wasDown = down;
    }

    private static boolean teleportPressed() {
        for (KeyMapping key : VR.teleportKeys()) {
            if (((KeyMappingAccessor) key).jegvr$isDownRaw()) {
                return true;
            }
        }
        return false;
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

        // What JEG's client does for a main-hand shot, without posting the event (our main-hand listeners would
        // treat it as a main-hand shot): the constructor starts the shoot animation of this stack, then the flash.
        new GunFireEvent.Post(player, stack);
        if (gun.getDisplay().getFlash() != null) {
            GunRenderingHandler.get().showMuzzleFlashForPlayer(player.getId());
        }
        VrHands.markShot(InteractionHand.OFF_HAND);

        float angle = gun.getGeneral().getRecoilAngle() * (1.0F - GunModifierHelper.getRecoilModifier(stack));
        VR.haptic(false, 0.06F, Mth.clamp(angle / 10.0F, 0.2F, 1.0F));
    }
}
