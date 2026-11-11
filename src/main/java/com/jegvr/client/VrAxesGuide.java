package com.jegvr.client;

import com.jegvr.compat.VR;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * {@code /jegvr axes}: draws the axes of the gun in the main hand and a ring around each ({@link VrAxesGeometry}), at
 * the gun's place and turned with it, as its pose has it now. It goes away when the command is run again or the item
 * in the main hand changes.
 */
public final class VrAxesGuide {
    private static final float[][] COLORS = {{1.0F, 0.25F, 0.25F}, {0.3F, 1.0F, 0.3F}, {0.35F, 0.55F, 1.0F}};

    private static boolean shown;
    private static Item shownFor;

    private VrAxesGuide() {
    }

    /** Shows or hides the guide; returns whether it is shown now. */
    public static boolean toggle() {
        LocalPlayer player = Minecraft.getInstance().player;
        shown = !shown && player != null;
        shownFor = shown ? player.getMainHandItem().getItem() : null;
        return shown;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!shown || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !VR.isEyePass()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.getMainHandItem().getItem() != shownFor) {
            shown = false;
            return;
        }
        VR.Aim hand = VR.mainHandAim();
        Vec3[] axes = VR.handAxes(true);
        if (!VR.active() || hand == null || axes == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        VrGunConfig.Pose gun = VrGunConfig.poseFor(VrGunPose.heldLikeGun(stack) ? VrGunPose.gunId(stack) : "");
        // In the physical left hand the pose is the right-hand one mirrored across X: so is the guide.
        Vec3 x = VR.leftHanded() ? axes[0].scale(-1.0) : axes[0];
        float scale = VR.worldScale();
        Vec3 cam = event.getCamera().getPosition();
        Vec3 origin = hand.pos().subtract(cam);

        PoseStack pose = event.getPoseStack();
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (VrAxesGeometry.Segment segment : VrAxesGeometry.build()) {
            Vec3 from = toWorld(onGun(segment.from(), gun), origin, x, axes[1], axes[2], scale);
            Vec3 to = toWorld(onGun(segment.to(), gun), origin, x, axes[1], axes[2], scale);
            Vec3 dir = to.subtract(from).normalize();
            float[] color = COLORS[segment.axis()];
            float r = color[0];
            float g = color[1];
            float b = color[2];
            lines.vertex(matrix, (float) from.x, (float) from.y, (float) from.z).color(r, g, b, 1.0F)
                    .normal(normal, (float) dir.x, (float) dir.y, (float) dir.z).endVertex();
            lines.vertex(matrix, (float) to.x, (float) to.y, (float) to.z).color(r, g, b, 1.0F)
                    .normal(normal, (float) dir.x, (float) dir.y, (float) dir.z).endVertex();
        }
        // Not endBatch(type): with shaders it does nothing, and the lines would be drawn later, in the other eye's view.
        buffers.endBatch();
    }

    private static Vec3 onGun(Vec3 point, VrGunConfig.Pose gun) {
        return VrAxesGeometry.posed(point, gun.pitch, gun.yaw, gun.roll, gun.tx, gun.ty, gun.tz);
    }

    private static Vec3 toWorld(Vec3 local, Vec3 origin, Vec3 x, Vec3 y, Vec3 z, float scale) {
        return origin.add(x.scale(local.x * scale)).add(y.scale(local.y * scale)).add(z.scale(local.z * scale));
    }
}
