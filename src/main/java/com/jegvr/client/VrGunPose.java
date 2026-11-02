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

/** Orientation/position correction applied to a JEG gun held in a VR controller. */
public final class VrGunPose {
    /** Moves the model so its grip sits in Vivecraft's hand: forward (-Z) and down, in blocks of the controller frame. */
    private static final float GRIP_FORWARD = 0.06F;
    private static final float GRIP_DOWN = 0.08F;

    private VrGunPose() {
    }

    public static String gunId(ItemStack stack) {
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key == null ? "" : key.toString();
    }

    public static void apply(PoseStack poseStack, ItemStack stack, ItemDisplayContext context, boolean mainHand) {
        VrGunConfig.Pose pose = VrGunConfig.poseFor(gunId(stack));
        // The left hand mirrors yaw/roll and the lateral offset.
        float side = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1.0F : 1.0F;
        // Precision mode: turn the gun about the grip so its barrel (-Z) runs from the gun hand to the free hand.
        Vec3 twoHand = VrPrecision.localAimDir(mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        if (twoHand != null) {
            poseStack.mulPose(new Quaternionf().rotationTo(0.0F, 0.0F, -1.0F,
                    (float) twoHand.x, (float) twoHand.y, (float) twoHand.z));
        }
        poseStack.translate(pose.tx * side, pose.ty - GRIP_DOWN, pose.tz - GRIP_FORWARD);
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
