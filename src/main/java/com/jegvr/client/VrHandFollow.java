package com.jegvr.client;

import com.jegvr.compat.VR;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import ttv.migami.jeg.item.GunItem;

import java.util.HashMap;
import java.util.Map;

/**
 * Phase H: while a gun plays one of its animations, the hands that represent the controllers let go of the controllers
 * and follow the animation, then return.
 * <ul>
 * <li>The hand holding that gun follows the gun's {@code right_arm} bone, measured from where the bone rests against the
 * hand ({@link Hand#rest}), so it only moves as much as the animation moves it.</li>
 * <li>The other hand goes to the {@code left_arm} bone, unless it holds a gun of its own: then it stays with its
 * controller. Whatever else it holds is hidden until it is back.</li>
 * <li>During reloads, inspections and draws the controller carries the gun and its hand with a delay ({@link #HEAVY}).
 * Not while firing: the gun must show where the shot goes.</li>
 * </ul>
 * A bone with scale 0 is an arm the animation does not show (the off arm of a one-handed gun): that hand stays free.
 * Bones are known once the gun is drawn, after Vivecraft draws the hand, so a following hand is drawn after both guns.
 */
public final class VrHandFollow {
    private static final float FOLLOW_IN_SECONDS = 0.12F;
    private static final float FOLLOW_OUT_SECONDS = 0.2F;
    private static final float HEAVY_FADE_SECONDS = 0.25F;
    /** Time constant of the delay with which the gun follows the controller. */
    private static final float HEAVY_LAG_SECONDS = 0.14F;
    /** Longest step of a frame, so a hitch does not make the hands jump. */
    private static final float MAX_FRAME_SECONDS = 0.1F;
    private static final float MIN_BONE_VOLUME = 1.0E-7F;
    private static final float PIXEL = 1.0F / 16.0F;

    /** One side of Vivecraft's arm: from the arm part's origin to the middle of the fingertips. */
    private static final float ARM_TIP_X = 1.0F;
    private static final float ARM_TIP_Y = 10.0F;
    /** Vivecraft places its arm part at x = +-5, y = 2 ({@code VRArmRenderer.renderHand}). */
    private static final float VIVECRAFT_ARM_X = 5.0F;
    private static final float VIVECRAFT_ARM_Y = 2.0F;

    private static final class Hand {
        // Measured while drawing the current view.
        boolean hasHand;
        final Matrix4f pose = new Matrix4f();
        final Matrix3f normal = new Matrix3f();
        PlayerRenderer renderer;
        boolean rightModel;
        AbstractClientPlayer player;
        int light;
        boolean hasGrip;
        final Matrix4f grip = new Matrix4f();
        boolean hasSupport;
        final Matrix4f support = new Matrix4f();

        // What the left eye saw last frame.
        boolean sawHand;
        boolean sawGrip;
        boolean sawSupport;

        // Advanced once per frame.
        float follow;
        float heavy;
        boolean animating;
        final Matrix4f heavyOffset = new Matrix4f();
        final Vector3f smoothPos = new Vector3f();
        final Quaternionf smoothRot = new Quaternionf();
        boolean smoothReady;

        /** Per gun, from the {@code right_arm} bone at rest to this hand: the gun's grip as the player holds it in VR. */
        final Map<Item, Matrix4f> rest = new HashMap<>();
    }

    private static final Hand MAIN = new Hand();
    private static final Hand OFF = new Hand();
    private static long lastFrameNanos;
    private static boolean drawing;

    private VrHandFollow() {
    }

    private static Hand hand(boolean main) {
        return main ? MAIN : OFF;
    }

    /** Vivecraft starts drawing the hands of a view. */
    public static void beginHands() {
        if (!VR.active()) {
            return;
        }
        if (VR.isLeftEyePass()) {
            advanceFrame();
        }
        clearView(MAIN);
        clearView(OFF);
    }

    private static void clearView(Hand hand) {
        hand.hasHand = false;
        hand.hasGrip = false;
        hand.hasSupport = false;
    }

    private static void advanceFrame() {
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0 ? 0.0F : Math.min((now - lastFrameNanos) * 1.0E-9F, MAX_FRAME_SECONDS);
        lastFrameNanos = now;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack mainStack = player.getMainHandItem();
        ItemStack offStack = player.getOffhandItem();
        String mainAnimation = followedAnimation(mainStack);
        String offAnimation = followedAnimation(offStack);
        advanceHand(MAIN, true, mainStack, mainAnimation, offAnimation, OFF, dt);
        advanceHand(OFF, false, offStack, offAnimation, mainAnimation, MAIN, dt);
    }

    private static String followedAnimation(ItemStack stack) {
        return VR.isBow(stack) ? null : VrGunAnimations.followedAnimation(stack);
    }

    private static void advanceHand(Hand hand, boolean main, ItemStack stack, String own, String other, Hand otherHand,
                                    float dt) {
        // Two-handed aiming already places both hands on the gun, and turns it about the grip.
        boolean free = !VrPrecision.active();
        boolean holdsGun = stack.getItem() instanceof GunItem;
        boolean follows = free && hand.sawHand
                && (holdsGun ? own != null && hand.sawGrip : other != null && otherHand.sawSupport);
        hand.follow = approach(hand.follow, follows, dt / FOLLOW_IN_SECONDS, dt / FOLLOW_OUT_SECONDS);
        hand.animating = own != null;
        hand.heavy = approach(hand.heavy, free && VrGunAnimations.isHeavy(own), dt / HEAVY_FADE_SECONDS,
                dt / HEAVY_FADE_SECONDS);
        advanceHeavy(hand, main, dt);
    }

    private static float approach(float value, boolean up, float stepUp, float stepDown) {
        return up ? Math.min(1.0F, value + stepUp) : Math.max(0.0F, value - stepDown);
    }

    /** The delayed pose lives in room space, so walking or turning with the stick adds no delay of its own. */
    private static void advanceHeavy(Hand hand, boolean main, float dt) {
        Matrix4f room = VR.roomHand(main);
        if (room == null) {
            hand.heavy = 0.0F;
            hand.smoothReady = false;
            hand.heavyOffset.identity();
            return;
        }
        Vector3f position = room.getTranslation(new Vector3f());
        Quaternionf rotation = room.getNormalizedRotation(new Quaternionf());
        if (hand.heavy <= 0.0F || !hand.smoothReady) {
            hand.smoothPos.set(position);
            hand.smoothRot.set(rotation);
            hand.smoothReady = true;
            hand.heavyOffset.identity();
            return;
        }
        float step = 1.0F - (float) Math.exp(-dt / HEAVY_LAG_SECONDS);
        hand.smoothPos.lerp(position, step);
        hand.smoothRot.slerp(rotation, step);
        Quaternionf inverse = new Quaternionf(rotation).conjugate();
        Vector3f offset = new Vector3f(hand.smoothPos).sub(position).rotate(inverse).mul(hand.heavy);
        Quaternionf turn = new Quaternionf().slerp(inverse.mul(hand.smoothRot), hand.heavy);
        hand.heavyOffset.translationRotateScale(offset, turn, 1.0F);
    }

    /** Vivecraft set the frame of a hand to its controller: the delay applies to all that hand draws. */
    public static void applyHeavy(PoseStack poseStack, boolean main) {
        if (VR.active() && hand(main).heavy > 0.0F) {
            poseStack.last().pose().mul(hand(main).heavyOffset);
        }
    }

    /** What a hand holds as Vivecraft draws it: a hand away following the animation holds nothing but its gun. */
    public static ItemStack heldItem(ItemStack stack, boolean main) {
        return hand(main).follow > 0.0F && !(stack.getItem() instanceof GunItem) ? ItemStack.EMPTY : stack;
    }

    /** Vivecraft is about to draw a hand here. Returns true to skip it: {@link #drawFollowing} draws it later. */
    public static boolean onVivecraftHand(boolean main, PlayerRenderer renderer, boolean rightModel, PoseStack poseStack,
                                          int light, AbstractClientPlayer player) {
        if (drawing || !VR.active()) {
            return false;
        }
        Hand hand = hand(main);
        hand.hasHand = true;
        hand.pose.set(poseStack.last().pose());
        hand.normal.set(poseStack.last().normal());
        hand.renderer = renderer;
        hand.rightModel = rightModel;
        hand.light = light;
        hand.player = player;
        return hand.follow > 0.0F;
    }

    /** JEG reached an arm bone of the gun being drawn; the stack holds that arm's frame. */
    public static void onArmBone(GeoBone bone, ModelPart part, PoseStack poseStack) {
        Hand hand = hand(VrHands.renderingHand() != InteractionHand.OFF_HAND);
        Matrix4f frame = new Matrix4f(poseStack.last().pose()).translate(part.x * PIXEL, part.y * PIXEL, part.z * PIXEL);
        if (Math.abs(frame.determinant3x3()) < MIN_BONE_VOLUME) {
            return;
        }
        String name = bone.getName();
        boolean fake = name.startsWith("fake_");
        if (name.endsWith("right_arm")) {
            if (!fake || !hand.hasGrip) {
                hand.grip.set(frame);
                hand.hasGrip = true;
            }
        } else if (!fake || !hand.hasSupport) {
            hand.support.set(frame);
            hand.hasSupport = true;
        }
    }

    /** Both guns are drawn: draw the hands that are following an animation. */
    public static void drawFollowing(PoseStack poseStack) {
        if (!VR.active()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != null) {
            drawHand(MAIN, OFF, player.getMainHandItem(), player, poseStack, minecraft);
            drawHand(OFF, MAIN, player.getOffhandItem(), player, poseStack, minecraft);
        }
        if (VR.isLeftEyePass()) {
            remember(MAIN);
            remember(OFF);
        }
    }

    private static void remember(Hand hand) {
        hand.sawHand = hand.hasHand;
        hand.sawGrip = hand.hasGrip;
        hand.sawSupport = hand.hasSupport;
    }

    private static void drawHand(Hand hand, Hand other, ItemStack stack, LocalPlayer player, PoseStack poseStack,
                                 Minecraft minecraft) {
        if (!hand.hasHand) {
            return;
        }
        boolean holdsGun = stack.getItem() instanceof GunItem;
        if (hand.follow <= 0.0F) {
            if (holdsGun && hand.hasGrip) {
                learnRest(hand, stack, player);
            }
            return;
        }
        Matrix4f target = target(hand, other, stack, holdsGun);
        poseStack.pushPose();
        poseStack.last().pose().set(target == null ? hand.pose : blend(hand.pose, target, hand.follow));
        poseStack.last().normal().set(hand.normal);
        minecraft.gameRenderer.lightTexture().turnOnLightLayer();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        drawing = true;
        try {
            if (hand.rightModel) {
                hand.renderer.renderRightHand(poseStack, buffers, hand.light, hand.player);
            } else {
                hand.renderer.renderLeftHand(poseStack, buffers, hand.light, hand.player);
            }
        } finally {
            drawing = false;
        }
        buffers.endBatch();
        minecraft.gameRenderer.lightTexture().turnOffLightLayer();
        poseStack.popPose();
    }

    /** Measured whenever the gun rests in the hand, so it also follows changes to the gun's pose. */
    private static void learnRest(Hand hand, ItemStack stack, LocalPlayer player) {
        if (hand.animating || hand.heavy > 0.0F || player.swinging || VrPrecision.active() || VrRecoil.isKicking()) {
            return;
        }
        hand.rest.computeIfAbsent(stack.getItem(), item -> new Matrix4f()).set(hand.grip).invert().mul(hand.pose);
    }

    private static Matrix4f target(Hand hand, Hand other, ItemStack stack, boolean holdsGun) {
        if (holdsGun) {
            if (!hand.hasGrip) {
                return null;
            }
            Matrix4f rest = hand.rest.get(stack.getItem());
            return rest != null ? new Matrix4f(hand.grip).mul(rest) : fingertipsOnBone(hand, hand.grip, true);
        }
        return other.hasSupport ? fingertipsOnBone(hand, other.support, false) : null;
    }

    /**
     * Vivecraft's hand with its own size and with its fingertips where JEG's arm has them, along that arm. JEG's arm is
     * larger, and bigger still next to a gun scaled for VR: only the rotation and the fingertips are taken from it.
     */
    private static Matrix4f fingertipsOnBone(Hand hand, Matrix4f bone, boolean rightBone) {
        Vector3f tip = bone.transformPosition((rightBone ? -ARM_TIP_X : ARM_TIP_X) * PIXEL, ARM_TIP_Y * PIXEL, 0.0F,
                new Vector3f());
        Matrix4f frame = new Matrix4f(bone).normalize3x3();
        if (frame.determinant3x3() < 0.0F) {
            // A mirrored gun: its left arm is the physical right one, which Vivecraft's model already is.
            frame.scale(-1.0F, 1.0F, 1.0F);
        }
        frame.scale(scaleOf(hand.pose));
        float side = hand.rightModel ? -1.0F : 1.0F;
        Vector3f toTip = frame.transformDirection(new Vector3f(side * (VIVECRAFT_ARM_X + ARM_TIP_X) * PIXEL,
                (VIVECRAFT_ARM_Y + ARM_TIP_Y) * PIXEL, 0.0F));
        return frame.setTranslation(tip.x - toTip.x, tip.y - toTip.y, tip.z - toTip.z);
    }

    private static float scaleOf(Matrix4f matrix) {
        return matrix.getColumn(0, new Vector3f()).length();
    }

    private static Matrix4f blend(Matrix4f from, Matrix4f to, float follow) {
        float amount = follow * follow * (3.0F - 2.0F * follow);
        Vector3f position = from.getTranslation(new Vector3f()).lerp(to.getTranslation(new Vector3f()), amount);
        Quaternionf rotation = new Matrix4f(from).normalize3x3().getNormalizedRotation(new Quaternionf());
        rotation.slerp(new Matrix4f(to).normalize3x3().getNormalizedRotation(new Quaternionf()), amount);
        return new Matrix4f().translationRotateScale(position, rotation, scaleOf(from));
    }
}
