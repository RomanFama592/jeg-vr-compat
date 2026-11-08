package com.jegvr.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Quaternionf;
import ttv.migami.jeg.item.FlashlightItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.TrumpetItem;

/** Orientation/position correction applied to a JEG gun held in a VR controller (values: {@link VrGunConfig}). */
public final class VrGunPose {
    private VrGunPose() {
    }

    /**
     * Items held like a gun in VR: JEG guns, and the trumpet (F9.11), a 3D model with its bell along -Z that is not a
     * {@link GunItem}, and the flashlight, likewise. Vivecraft's per-type transforms are meant for flat sprites and tools
     * and turn them sideways. Also JEG's grenades, molotov and water bomb while this addon throws them (F9.26): a sprite
     * standing upright on the hand, facing the player.
     */
    public static boolean heldLikeGun(ItemStack stack) {
        return stack.getItem() instanceof GunItem || stack.getItem() instanceof TrumpetItem
                || stack.getItem() instanceof FlashlightItem || (VrThrow.isThrowable(stack) && VrThrow.enabled());
    }

    public static String gunId(ItemStack stack) {
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key == null ? "" : key.toString();
    }

    public static void apply(PoseStack poseStack, ItemStack stack, ItemDisplayContext context, boolean mainHand) {
        VrGunConfig.Pose pose = VrGunConfig.poseFor(gunId(stack));
        boolean mirrored = VrGunMirror.mirrors(mainHand);
        VrHandFollow.applyClassicReload(poseStack, stack, mainHand);
        // The left hand mirrors yaw/roll and the lateral offset; a mirrored frame already does.
        float side = !mirrored && context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1.0F : 1.0F;
        // Precision mode: turn the gun about the grip so its barrel (-Z) runs from the gun hand to the free hand.
        Vec3 twoHand = VrPrecision.localAimDir(mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        if (twoHand != null) {
            poseStack.mulPose(new Quaternionf().rotationTo(0.0F, 0.0F, -1.0F,
                    (float) twoHand.x, (float) twoHand.y, (float) twoHand.z));
        }
        // Physical left hand: the right-hand pose in a mirrored frame lands where the left-hand pose did.
        if (mirrored) {
            VrGunMirror.mirror(poseStack);
        }
        poseStack.translate(pose.tx * side, pose.ty, pose.tz);
        // JEG only fires the main-hand gun, so only that one kicks.
        if (mainHand) {
            float partialTick = Minecraft.getInstance().getFrameTime();
            poseStack.translate(0.0F, 0.0F, VrRecoil.kickBack(partialTick));
            poseStack.mulPose(Axis.XP.rotationDegrees(VrRecoil.kickDegrees(partialTick)));
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(pose.yaw * side));
        poseStack.mulPose(Axis.XP.rotationDegrees(pose.pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pose.roll * side));
        if (pose.scale != 1.0F) {
            poseStack.scale(pose.scale, pose.scale, pose.scale);
        }
    }
}
