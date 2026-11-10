package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.compat.jeg.JegKeys;
import com.jegvr.config.VrClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import ttv.migami.jeg.client.handler.ReloadHandler;
import ttv.migami.jeg.init.ModSyncedDataKeys;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.GunItem;

import java.util.HashMap;
import java.util.Map;

/**
 * Phase H: while a gun plays one of its animations, the hands that represent the controllers let go of the controllers
 * and follow the animation, then return.
 * <ul>
 * <li>The hand holding that gun follows the gun's {@code right_arm} bone, measured from where the bone rests against the
 * hand ({@link Hand#rest}), so it only moves as much as the animation moves it.</li>
 * <li>The other hand goes to the {@code left_arm} bone, unless it holds a gun of its own or the animation is a shot:
 * then it stays with its controller. Whatever else it holds is hidden until it is back.</li>
 * <li>JEG's classic guns have no bones or animation files, only a movement of the whole gun while reloading: JEG's,
 * on a smaller scale because here it is near the eyes ({@link #applyClassicReload}). The gun's hand moves with it, and
 * the other hand goes under the barrel and pushes rounds in.</li>
 * <li>During reloads, inspections and draws the controller carries the gun and its hand with a delay ({@link #HEAVY}).
 * Not while firing: the gun must show where the shot goes.</li>
 * </ul>
 * A bone with scale 0 is an arm the animation does not show (the off arm of a one-handed gun): that hand stays free.
 * Bones are known once the gun is drawn, after Vivecraft draws the hand, so a following hand is drawn after both guns.
 * The {@code handsFollowAnimations} option turns the hands' part off.
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

    /** Where JEG's classic reload takes the gun at its peak, in metres and degrees of the controller's frame. */
    private static final float CLASSIC_RAISE = 0.08F;
    private static final float CLASSIC_BACK = 0.03F;
    private static final float CLASSIC_TILT_DEGREES = 45.0F;
    /** Where the other hand goes on a classic gun, and how it pushes the rounds in. */
    private static final float CLASSIC_SUPPORT_DOWN = 0.05F;
    private static final float CLASSIC_SUPPORT_FORWARD = 0.2F;
    private static final float CLASSIC_PUSH = 0.025F;
    private static final float CLASSIC_PUSH_PER_TICK = 0.35F;

    /** One side of Vivecraft's arm: from the arm part's origin to the middle of the fingertips. */
    private static final float ARM_TIP_X = 1.0F;
    private static final float ARM_TIP_Y = 10.0F;
    /** Vivecraft places its arm part at x = +-5, y = 2 ({@code VRArmRenderer.renderHand}). */
    private static final float VIVECRAFT_ARM_X = 5.0F;
    private static final float VIVECRAFT_ARM_Y = 2.0F;

    private static final class Hand {
        // Measured while drawing the current view.
        boolean hasHand;
        boolean hasController;
        final Matrix4f controller = new Matrix4f();
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
        hand.hasController = false;
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
        boolean allowed = VrClientConfig.handsFollowAnimations() && !VrPrecision.active();
        boolean holdsGun = stack.getItem() instanceof GunItem;
        boolean ownClassic = classicReloading(stack, main);
        LocalPlayer player = Minecraft.getInstance().player;
        boolean otherClassic = classicReloading(player.getItemInHand(main ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND), !main);
        boolean follows = holdsGun
                ? ownClassic || (own != null && hand.sawGrip)
                : otherClassic || (other != null && !VrGunAnimations.isFiring(other) && otherHand.sawSupport);
        hand.follow = approach(hand.follow, allowed && hand.sawHand && follows, dt / FOLLOW_IN_SECONDS, dt / FOLLOW_OUT_SECONDS);
        hand.animating = own != null;
        hand.heavy = approach(hand.heavy, allowed && (ownClassic || VrGunAnimations.isHeavy(own)), dt / HEAVY_FADE_SECONDS,
                dt / HEAVY_FADE_SECONDS);
        advanceHeavy(hand, main, dt);
    }

    private static boolean isClassicGun(ItemStack stack) {
        return stack.getItem() instanceof GunItem && !(stack.getItem() instanceof AnimatedGunItem);
    }

    private static boolean classicReloading(ItemStack stack, boolean main) {
        Player player = Minecraft.getInstance().player;
        return isClassicGun(stack) && player != null && ModSyncedDataKeys.RELOADING.getValue(player)
                && VrHands.reloadingHand() == (main ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }

    /** How far JEG's reload movement is, 0 to 1, for this hand's classic gun; it also eases out after the reload. */
    private static float classicProgress(ItemStack stack, boolean main) {
        if (!isClassicGun(stack) || VrHands.lastReloadHand() != (main ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND)) {
            return 0.0F;
        }
        return ReloadHandler.get().getReloadProgress(Minecraft.getInstance().getFrameTime());
    }

    /** The movement of a classic gun, about its controller. Does nothing for other guns or with no reload going on. */
    public static void applyClassicReload(PoseStack poseStack, ItemStack stack, boolean main) {
        float progress = classicProgress(stack, main);
        if (progress > 0.0F) {
            poseStack.last().pose().mul(classicReloadMatrix(progress));
            poseStack.last().normal().rotate(Axis.XP.rotationDegrees(CLASSIC_TILT_DEGREES * progress));
        }
    }

    private static Matrix4f classicReloadMatrix(float progress) {
        return new Matrix4f().translate(0.0F, CLASSIC_RAISE * progress, -CLASSIC_BACK * progress)
                .rotateX((float) Math.toRadians(CLASSIC_TILT_DEGREES * progress));
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
        if (!VR.active()) {
            return;
        }
        Hand hand = hand(main);
        if (hand.heavy > 0.0F) {
            poseStack.last().pose().mul(hand.heavyOffset);
        }
        hand.controller.set(poseStack.last().pose());
        hand.hasController = true;
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
        boolean fake = name.startsWith(JegKeys.FAKE_BONE_PREFIX);
        if (name.endsWith(JegKeys.RIGHT_ARM_BONE)) {
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
            drawHand(MAIN, OFF, true, player, poseStack, minecraft);
            drawHand(OFF, MAIN, false, player, poseStack, minecraft);
        }
        if (VR.isLeftEyePass()) {
            remember(MAIN);
            remember(OFF);
        }
    }

    // F10.4: the ghost hand of {@link VrGripHint} uses the frames measured while drawing the current view.

    /** A hand's controller frame in the view being drawn (Vivecraft's pose stack), or null before it is set up. */
    static Matrix4f controllerFrame(boolean main) {
        Hand hand = hand(main);
        return hand.hasController ? new Matrix4f(hand.controller) : null;
    }

    /** Where Vivecraft draws a hand's model in this view, or null when it does not (or the hand follows an animation). */
    static Matrix4f handFrame(boolean main) {
        Hand hand = hand(main);
        return hand.hasHand && hand.follow <= 0.0F ? new Matrix4f(hand.pose) : null;
    }

    /** The hand's model in its controller's frame (constant for a player), or null when not measured in this view. */
    static Matrix4f handInController(boolean main) {
        Hand hand = hand(main);
        return hand.hasHand && hand.hasController ? new Matrix4f(hand.controller).invert().mul(hand.pose) : null;
    }

    /** The free hand's model with its fingertips on the gun's support arm (JEG's {@code left_arm} bone), or null. */
    static Matrix4f handOnSupport(boolean gunMain, boolean freeMain) {
        Hand gun = hand(gunMain);
        Hand free = hand(freeMain);
        return gun.hasSupport && free.hasHand ? fingertipsOnBone(free, gun.support, false) : null;
    }

    /** Draws a hand's model at {@code pose} through {@code buffers}; the caller ends the batch. */
    static void drawHandModel(boolean main, PoseStack poseStack, Matrix4f pose, MultiBufferSource buffers) {
        Hand hand = hand(main);
        if (!hand.hasHand) {
            return;
        }
        poseStack.pushPose();
        poseStack.last().pose().set(pose);
        poseStack.last().normal().set(hand.normal);
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
        poseStack.popPose();
    }

    /** The player whose hand Vivecraft drew (for its skin), or null. */
    static AbstractClientPlayer handPlayer(boolean main) {
        Hand hand = hand(main);
        return hand.hasHand ? hand.player : null;
    }

    /** From one frame to another, eased, keeping the first one's scale. */
    static Matrix4f between(Matrix4f from, Matrix4f to, float amount) {
        return blend(from, to, amount);
    }

    private static void remember(Hand hand) {
        hand.sawHand = hand.hasHand;
        hand.sawGrip = hand.hasGrip;
        hand.sawSupport = hand.hasSupport;
    }

    private static void drawHand(Hand hand, Hand other, boolean main, LocalPlayer player, PoseStack poseStack,
                                 Minecraft minecraft) {
        if (!hand.hasHand) {
            return;
        }
        ItemStack stack = player.getItemInHand(main ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        boolean holdsGun = stack.getItem() instanceof GunItem;
        if (hand.follow <= 0.0F) {
            if (holdsGun && hand.hasGrip) {
                learnRest(hand, stack, player);
            }
            return;
        }
        ItemStack otherStack = player.getItemInHand(main ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        Matrix4f target = target(hand, other, stack, holdsGun, otherStack, main, player);
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

    private static Matrix4f target(Hand hand, Hand other, ItemStack stack, boolean holdsGun, ItemStack otherStack,
                                   boolean main, LocalPlayer player) {
        if (holdsGun && isClassicGun(stack)) {
            // Moves with the gun, about the controller.
            return new Matrix4f(hand.controller).mul(classicReloadMatrix(classicProgress(stack, main)))
                    .mul(new Matrix4f(hand.controller).invert()).mul(hand.pose);
        }
        if (!holdsGun && isClassicGun(otherStack)) {
            float time = player.tickCount + Minecraft.getInstance().getFrameTime();
            float push = CLASSIC_PUSH * Mth.sin(time * CLASSIC_PUSH_PER_TICK);
            return new Matrix4f(other.controller).mul(classicReloadMatrix(classicProgress(otherStack, !main)))
                    .translate(0.0F, -CLASSIC_SUPPORT_DOWN + push, -CLASSIC_SUPPORT_FORWARD)
                    .mul(new Matrix4f(hand.controller).invert()).mul(hand.pose);
        }
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
