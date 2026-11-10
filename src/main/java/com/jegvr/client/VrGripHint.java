package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.config.VrClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.file.Path;

/**
 * F10.4: how a two-handed gun shows where the free hand can hold it (precision mode, {@link VrPrecision}).
 * <ul>
 *   <li>The first times a two-handed gun is taken and held for 3 seconds without changing it ({@code gripGhostShowsLeft},
 *   2 by default), a see-through copy of the free hand leaves it and goes to the grip, twice, showing where and how to
 *   hold the gun.</li>
 *   <li>After that, soft particles mark the grip while the free hand is near, and the free hand buzzes lightly while it
 *   is on the foregrip, until it grabs (then a firm buzz, from {@link VrPrecision}). Option {@code gripHint}.</li>
 * </ul>
 * Where the grip is, for each gun: the one taken with {@code /jegvr grip capture} (saved with the gun's pose, see
 * {@link VrGripPose}); else the free hand with its fingertips on the gun model's support arm, as JEG holds it; else a
 * fixed place ahead of the gun hand (guns without bones).
 */
public final class VrGripHint {
    /** The ghost shows once the same two-handed gun has been held this long without changing it (3 s). */
    private static final int HOLD_BEFORE_DEMO_TICKS = 60;
    /** One demonstration: the ghost appears on the hand, goes to the grip, stays a moment and fades. */
    private static final float TAP_TICKS = 32.0F;
    private static final int TAPS = 2;
    private static final float APPEAR = 0.12F;
    private static final float ARRIVE = 0.6F;
    private static final float LEAVE = 0.85F;
    private static final float GHOST_ALPHA = 0.5F;
    private static final float[] GHOST_TINT = {0.65F, 0.85F, 1.0F};

    private static final int PARTICLE_EVERY_TICKS = 3;
    private static final double PARTICLE_SPREAD = 0.025;
    private static final float PARTICLE_SIZE = 0.35F;
    private static final float BUZZ_SECONDS = 0.06F;
    private static final float BUZZ_AMPLITUDE = 0.12F;

    /** Guns without bones: the grip is this far down and ahead of the gun controller (room metres). */
    private static final float FALLBACK_DOWN = 0.05F;
    private static final float FALLBACK_FORWARD = 0.22F;
    private static final int TICKS_PER_SECOND = 20;

    /** The two-handed gun last held (null: none), to notice a new one being taken. */
    private static Item lastHoldable;
    /** Client tick since which {@link #lastHoldable} has been held; whether its lesson already showed. */
    private static long holdingSince;
    private static boolean lessonDone;
    /** {@code /jegvr grip ghost} asked for a demonstration: it starts once a two-handed gun is held (chat closed). */
    private static boolean ghostRequested;
    private static final int REQUESTED_DELAY_TICKS = 10;
    /** Client tick at which the ghost's demonstration starts (or started); -1 without one. */
    private static long demoStart = -1;
    /** Where the free controller holds the gun, in the world, from the last left-eye view; null when unknown. */
    private static Vec3 gripWorld;
    private static int captureTicksLeft = -1;
    private static int ticks;

    private VrGripHint() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !VR.active()) {
            lastHoldable = null;
            demoStart = -1;
            gripWorld = null;
            captureTicksLeft = -1;
            return;
        }
        while (VrKeys.CAPTURE_GRIP.consumeClick()) {
            player.displayClientMessage(capture(), false);
        }
        tickCapture(player);

        InteractionHand gunHand = VrPrecision.active() ? null : VrPrecision.holdableHand(player);
        Item holdable = gunHand == null ? null : player.getItemInHand(gunHand).getItem();
        if (holdable != lastHoldable) {
            // Another gun (or none): the wait starts again, and a lesson on its way stops.
            lastHoldable = holdable;
            holdingSince = player.tickCount;
            lessonDone = false;
            demoStart = -1;
        }
        if (ghostRequested && holdable != null) {
            ghostRequested = false;
            showGhost(REQUESTED_DELAY_TICKS);
        }
        int left = VrClientConfig.gripGhostShowsLeft();
        if (holdable != null && !lessonDone && left > 0 && player.tickCount - holdingSince >= HOLD_BEFORE_DEMO_TICKS) {
            lessonDone = true;
            showGhost(0);
            VrClientConfig.setGripGhostShowsLeft(left - 1);
        }
        if (demoStart >= 0 && player.tickCount - demoStart > TAPS * TAP_TICKS) {
            demoStart = -1;
        }
        if (gunHand == null) {
            return;
        }
        VrClientConfig.GripHint hint = VrClientConfig.gripHint();
        boolean onGrip = VrPrecision.handOnForegrip(gunHand);
        if (hint != VrClientConfig.GripHint.OFF && onGrip) {
            VrGunHand.haptic(VrGunHand.other(gunHand), BUZZ_SECONDS, BUZZ_AMPLITUDE);
        }
        if (hint == VrClientConfig.GripHint.PARTICLES_AND_BUZZ && demoStart < 0
                && ++ticks % PARTICLE_EVERY_TICKS == 0
                && VrPrecision.handDistance(gunHand) <= VrClientConfig.gripHintRange() * VR.worldScale()) {
            spawnParticle(mc, gunHand, onGrip);
        }
    }

    /**
     * {@code /jegvr grip ghost}: shows the demonstration without counting it. The command runs with the chat open, when
     * no gun counts as held, so it waits for the chat to close and a two-handed gun to be in hand.
     */
    public static void requestGhost() {
        ghostRequested = true;
    }

    /** Shows the ghost hand's demonstration after {@code delayTicks}. */
    private static void showGhost(int delayTicks) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            demoStart = player.tickCount + delayTicks;
        }
    }

    private static void spawnParticle(Minecraft mc, InteractionHand gunHand, boolean onGrip) {
        Vec3 at = gripWorld != null ? gripWorld : VrPrecision.foregrip(gunHand);
        if (at == null || mc.level == null) {
            return;
        }
        double spread = PARTICLE_SPREAD * VR.worldScale();
        float[] rgb = onGrip ? VrGlow.GREEN : VrGlow.BLUE;
        mc.level.addParticle(new DustParticleOptions(new Vector3f(rgb[0], rgb[1], rgb[2]), PARTICLE_SIZE),
                at.x + (mc.level.random.nextDouble() - 0.5) * 2.0 * spread,
                at.y + (mc.level.random.nextDouble() - 0.5) * 2.0 * spread,
                at.z + (mc.level.random.nextDouble() - 0.5) * 2.0 * spread, 0.0, 0.0, 0.0);
    }

    /**
     * Called after Vivecraft drew the hands of a view, with its pose stack (world axes, relative to the eye): finds the
     * grip of the gun in hand and, during a demonstration, draws the ghost hand on its way there.
     */
    public static void drawGhost(PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        InteractionHand gunHand = player == null || !VR.active() || VrPrecision.active() ? null
                : VrPrecision.holdableHand(player);
        if (gunHand == null) {
            if (VR.isLeftEyePass()) {
                gripWorld = null;
            }
            return;
        }
        boolean gunMain = gunHand == InteractionHand.MAIN_HAND;
        boolean freeMain = !gunMain;
        Matrix4f grip = gripController(gunMain, freeMain, player.getItemInHand(gunHand));
        if (grip == null) {
            return;
        }
        if (VR.isLeftEyePass()) {
            Vector3f position = grip.getTranslation(new Vector3f());
            gripWorld = mc.gameRenderer.getMainCamera().getPosition().add(position.x, position.y, position.z);
        }
        float time = player.tickCount + mc.getFrameTime() - demoStart;
        if (demoStart < 0 || time < 0.0F || time >= TAPS * TAP_TICKS) {
            return;
        }
        Matrix4f live = VrHandFollow.handFrame(freeMain);
        Matrix4f inController = VrHandFollow.handInController(freeMain);
        AbstractClientPlayer owner = VrHandFollow.handPlayer(freeMain);
        if (live == null || inController == null || owner == null) {
            return;
        }
        float phase = (time % TAP_TICKS) / TAP_TICKS;
        float move = Mth.clamp((phase - APPEAR) / (ARRIVE - APPEAR), 0.0F, 1.0F);
        float alpha = phase < APPEAR ? phase / APPEAR : phase > LEAVE ? (1.0F - phase) / (1.0F - LEAVE) : 1.0F;
        Matrix4f ghost = VrHandFollow.between(live, new Matrix4f(grip).mul(inController), move);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VrHandFollow.drawHandModel(freeMain, poseStack, ghost,
                ghostBuffers(buffers, owner.getSkinTextureLocation(), alpha * GHOST_ALPHA));
        // Not endBatch(type): with shaders it does nothing, and the ghost would be drawn in the other eye's view.
        buffers.endBatch();
    }

    /** Where the free controller holds the gun in {@code gunMain}'s hand, in the view's frame, or null. */
    private static Matrix4f gripController(boolean gunMain, boolean freeMain, ItemStack stack) {
        Matrix4f gun = VrHandFollow.controllerFrame(gunMain);
        if (gun == null) {
            return null;
        }
        VrGunConfig.Grip grip = VrGunPose.heldLikeGun(stack) ? VrGunConfig.poseFor(VrGunPose.gunId(stack)).grip : null;
        if (grip != null) {
            return VrGripPose.apply(gun, grip, VrGunMirror.mirrors(gunMain));
        }
        Matrix4f onSupport = VrHandFollow.handOnSupport(gunMain, freeMain);
        Matrix4f inController = VrHandFollow.handInController(freeMain);
        if (onSupport != null && inController != null) {
            return onSupport.mul(inController.invert());
        }
        return gun.translate(0.0F, -FALLBACK_DOWN, -FALLBACK_FORWARD);
    }

    /** The hand's own skin, see-through, pale blue and unlit, whatever render type the hand asks for. */
    private static MultiBufferSource ghostBuffers(MultiBufferSource buffers, ResourceLocation skin, float alpha) {
        return type -> new Ghost(buffers.getBuffer(RenderType.entityTranslucent(skin)), alpha);
    }

    // --- Debug: take the grip of the held gun from the free hand. ---

    /** {@code /jegvr grip capture}: takes the grip in {@code seconds}, buzzing the free hand every second. */
    public static void startCapture(int seconds) {
        captureTicksLeft = Math.max(1, seconds) * TICKS_PER_SECOND;
    }

    private static void tickCapture(LocalPlayer player) {
        if (captureTicksLeft < 0) {
            return;
        }
        captureTicksLeft--;
        InteractionHand gunHand = VrGunHand.of(player);
        if (captureTicksLeft == 0) {
            captureTicksLeft = -1;
            player.displayClientMessage(capture(), false);
        } else if (captureTicksLeft % TICKS_PER_SECOND == 0 && gunHand != null) {
            VrGunHand.haptic(VrGunHand.other(gunHand), 0.03F, 0.4F);
        }
    }

    /**
     * Stores where the free controller is now, relative to the gun controller, as the held gun's grip, and saves the
     * file the calibration commands edit (in development, the one shipped in the jar). Returns the message to show.
     */
    public static Component capture() {
        LocalPlayer player = Minecraft.getInstance().player;
        InteractionHand gunHand = player == null ? null : VrGunHand.of(player);
        ItemStack stack = gunHand == null ? ItemStack.EMPTY : player.getItemInHand(gunHand);
        if (!VrGunPose.heldLikeGun(stack)) {
            return Component.translatable("jeg_vr_compat.command.grip.nogun");
        }
        boolean gunMain = gunHand == InteractionHand.MAIN_HAND;
        Matrix4f gun = VrHandFollow.controllerFrame(gunMain);
        Matrix4f free = VrHandFollow.controllerFrame(!gunMain);
        if (gun == null || free == null) {
            return Component.translatable("jeg_vr_compat.command.grip.novr");
        }
        String id = VrGunPose.gunId(stack);
        VrGunConfig.Grip grip = VrGripPose.capture(gun, free, VrGunMirror.mirrors(gunMain));
        VrGunConfig.setGrip(id, grip);
        Path saved = VrGunConfig.save();
        if (saved == null) {
            return Component.translatable("jeg_vr_compat.command.grip.failed");
        }
        return Component.translatable("jeg_vr_compat.command.grip.captured", id,
                String.format("x=%.3f y=%.3f z=%.3f", grip.x, grip.y, grip.z), saved.toString());
    }

    /** {@code /jegvr grip reset}: the held gun's grip goes back to the automatic one. */
    public static Component resetGrip() {
        LocalPlayer player = Minecraft.getInstance().player;
        InteractionHand gunHand = player == null ? null : VrGunHand.of(player);
        ItemStack stack = gunHand == null ? ItemStack.EMPTY : player.getItemInHand(gunHand);
        if (!VrGunPose.heldLikeGun(stack)) {
            return Component.translatable("jeg_vr_compat.command.grip.nogun");
        }
        String id = VrGunPose.gunId(stack);
        VrGunConfig.setGrip(id, null);
        Path saved = VrGunConfig.save();
        return saved == null ? Component.translatable("jeg_vr_compat.command.grip.failed")
                : Component.translatable("jeg_vr_compat.command.grip.reset", id);
    }

    /** Tints and fades the hand's vertices, and lights them fully so the ghost reads in the dark. */
    private static final class Ghost implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float alpha;

        Ghost(VertexConsumer delegate, float alpha) {
            this.delegate = delegate;
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            delegate.color(Math.round(r * GHOST_TINT[0]), Math.round(g * GHOST_TINT[1]), Math.round(b * GHOST_TINT[2]),
                    Math.round(a * alpha));
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            delegate.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            delegate.uv2(LightTexture.FULL_BRIGHT & 0xFFFF, LightTexture.FULL_BRIGHT >> 16 & 0xFFFF);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            delegate.defaultColor(r, g, b, a);
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }
    }
}
