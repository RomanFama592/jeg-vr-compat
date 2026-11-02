package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.config.VrClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.item.GunItem;

/**
 * F9.14: Vivecraft's hand-locked HUD (and hotbar) sits on the off-hand controller. With a gun in that hand, slide it
 * back along the forearm, towards the elbow, so the gun neither covers it nor cuts through it. Without a gun, with the
 * HUD locked to the head, or with a menu open, Vivecraft's position is kept.
 */
public final class VrHud {
    /** Room-scale metres (multiplied by Vivecraft's world scale) from Vivecraft's spot towards the elbow. */
    private static final double BACK = 0.16;
    /** And a bit up, off the sleeve. */
    private static final double UP = 0.03;

    private VrHud() {
    }

    public static Vec3 moveOffGun(Vec3 guiPos) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null || !VrClientConfig.hotbarOnForearm() || !VR.hudOnHand()
                || !(player.getOffhandItem().getItem() instanceof GunItem)) {
            return guiPos;
        }
        VR.Aim hand = VR.offHandAim();
        if (hand == null || hand.dir().lengthSqr() < 1.0E-6) {
            return guiPos;
        }
        double scale = VR.worldScale();
        return guiPos.subtract(hand.dir().normalize().scale(BACK * scale)).add(0.0, UP * scale, 0.0);
    }
}
