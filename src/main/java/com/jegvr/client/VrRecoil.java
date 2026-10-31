package com.jegvr.client;

import com.jegvr.compat.VR;
import net.minecraft.util.Mth;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.common.GripType;
import ttv.migami.jeg.event.GunFireEvent;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.util.GunModifierHelper;

/**
 * VR replacement for JEG's camera recoil: each shot kicks the gun model up and back and vibrates the controllers.
 * The camera never moves.
 */
public final class VrRecoil {
    private static final float KICK_PER_DEGREE = 1.5F;
    private static final float MAX_KICK = 25.0F;
    private static final float KICK_DECAY = 0.55F;
    private static final float BACK_PER_DEGREE = 0.004F;

    private static float kick;
    private static float prevKick;

    private VrRecoil() {
    }

    @SubscribeEvent
    public static void onGunFire(GunFireEvent.Post event) {
        if (!event.isClient() || !VR.active() || !(event.getStack().getItem() instanceof GunItem gunItem)) {
            return;
        }
        Gun gun = gunItem.getModifiedGun(event.getStack());
        float angle = gun.getGeneral().getRecoilAngle() * (1.0F - GunModifierHelper.getRecoilModifier(event.getStack()));
        kick = Math.min(MAX_KICK, kick + angle * KICK_PER_DEGREE);

        float strength = Mth.clamp(angle / 10.0F, 0.2F, 1.0F);
        VR.haptic(true, 0.06F, strength);
        if (!GripType.ONE_HANDED.equals(gun.getGeneral().getGripType())) {
            VR.haptic(false, 0.05F, strength * 0.6F);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        prevKick = kick;
        kick = kick < 0.05F ? 0.0F : kick * KICK_DECAY;
    }

    /** Muzzle-up rotation of the held gun, in degrees. */
    public static float kickDegrees(float partialTick) {
        return Mth.lerp(partialTick, prevKick, kick);
    }

    /** Backwards push of the held gun, in blocks of the controller frame. */
    public static float kickBack(float partialTick) {
        return kickDegrees(partialTick) * BACK_PER_DEGREE;
    }
}
