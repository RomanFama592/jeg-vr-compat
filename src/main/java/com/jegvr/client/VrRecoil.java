package com.jegvr.client;

import com.jegvr.compat.VR;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.common.GripType;
import ttv.migami.jeg.event.GunFireEvent;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.util.GunModifierHelper;

/**
 * VR replacement for JEG's camera recoil: each shot kicks the gun model up and back, vibrates the controllers and
 * deviates the aim of the following shots (see {@code C2SMessageShootVRMixin}). The camera never moves.
 */
public final class VrRecoil {
    private static final float KICK_PER_DEGREE = 1.5F;
    private static final float MAX_KICK = 25.0F;
    private static final float KICK_DECAY = 0.55F;
    private static final float BACK_PER_DEGREE = 0.004F;

    /** Gameplay recoil: how far follow-up shots climb (degrees per degree of gun recoil) and drift sideways. */
    private static final float AIM_CLIMB_PER_DEGREE = 0.6F;
    private static final float AIM_DRIFT_PER_DEGREE = 0.35F;
    private static final float MAX_AIM_CLIMB = 15.0F;
    private static final float MAX_AIM_DRIFT = 6.0F;
    /** Slower than the visual kick, so sustained fire keeps climbing. */
    private static final float AIM_DECAY = 0.8F;

    private static final RandomSource RANDOM = RandomSource.create();

    private static float kick;
    private static float prevKick;
    private static float aimClimb;
    private static float aimDrift;

    private VrRecoil() {
    }

    @SubscribeEvent
    public static void onGunFire(GunFireEvent.Post event) {
        if (!event.isClient() || !VR.active() || !(event.getStack().getItem() instanceof GunItem gunItem)) {
            return;
        }
        // JEG's client fire event is only posted for the main hand (the off hand goes through VrOffhandShooting).
        VrHands.markShot(InteractionHand.MAIN_HAND);
        Gun gun = gunItem.getModifiedGun(event.getStack());
        float angle = gun.getGeneral().getRecoilAngle() * (1.0F - GunModifierHelper.getRecoilModifier(event.getStack()));
        kick = Math.min(MAX_KICK, kick + angle * KICK_PER_DEGREE);
        // Post fires after the shot message is built, so this deviates the next shots, not this one.
        aimClimb = Math.min(MAX_AIM_CLIMB, aimClimb + angle * AIM_CLIMB_PER_DEGREE);
        aimDrift = Mth.clamp(aimDrift + (RANDOM.nextFloat() - 0.5F) * 2.0F * angle * AIM_DRIFT_PER_DEGREE,
                -MAX_AIM_DRIFT, MAX_AIM_DRIFT);

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
        aimClimb = aimClimb < 0.05F ? 0.0F : aimClimb * AIM_DECAY;
        aimDrift = Math.abs(aimDrift) < 0.05F ? 0.0F : aimDrift * AIM_DECAY;
    }

    /** Upward deviation of the next shot, in degrees (subtract from pitch). */
    public static float aimClimb() {
        return aimClimb;
    }

    /** Sideways deviation of the next shot, in degrees (add to yaw). */
    public static float aimDrift() {
        return aimDrift;
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
