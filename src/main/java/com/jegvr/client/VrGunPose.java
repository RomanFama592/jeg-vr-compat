package com.jegvr.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

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

    public static void apply(PoseStack poseStack, ItemStack stack, ItemDisplayContext context) {
        VrGunConfig.Pose pose = VrGunConfig.poseFor(gunId(stack));
        // The left hand mirrors yaw/roll and the lateral offset.
        float side = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1.0F : 1.0F;
        poseStack.translate(pose.tx * side, pose.ty - GRIP_DOWN, pose.tz - GRIP_FORWARD);
        poseStack.mulPose(Axis.YP.rotationDegrees(pose.yaw * side));
        poseStack.mulPose(Axis.XP.rotationDegrees(pose.pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pose.roll * side));
        if (pose.scale != 1.0F) {
            poseStack.scale(pose.scale, pose.scale, pose.scale);
        }
    }
}
