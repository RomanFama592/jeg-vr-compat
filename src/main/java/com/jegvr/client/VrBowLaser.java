package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.config.VrClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import ttv.migami.jeg.common.ChargeTracker;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.particles.LaserData;
import ttv.migami.jeg.util.GunEnchantmentHelper;
import ttv.migami.jeg.util.GunModifierHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * F9.15: laser sight on JEG's compound bow. While the bow is drawn, a beam leaves the muzzle and follows the arrow's
 * predicted flight for up to {@link #LENGTH} blocks, stopping at the first block or entity the arrow would hit.
 * <p>
 * It looks like JEG's laser pointer attachment: a thin solid bar, as thick as the attachment's in a VR hand, whose colour
 * and transparency along its length are those of {@code laser_pointer.png}, stretched over the beam: almost white and
 * solid at the muzzle, translucent red along it and transparent at the tip. On a block it ends in JEG's own red dot.
 * <p>
 * The prediction copies JEG's {@code ProjectileEntity}: the arrow starts with the bow's projectile speed (with its
 * enchantment and attachment modifiers) and every tick it moves by its velocity, then gravity (-0.04 blocks/tick,
 * modified by attachments) is added. The charge only changes damage, not speed.
 */
public final class VrBowLaser {
    private static final String BOW_ID = "jeg:compound_bow";
    /** JEG's base projectile gravity, in blocks per tick squared ({@code ProjectileEntity} constructor). */
    private static final double JEG_GRAVITY = -0.04;
    private static final double LENGTH = 32.0;
    /** The beam is drawn in pieces this long so the gradient and the arc look smooth. */
    private static final double PIECE = 0.25;
    /** Simulated ticks at most (a slow arrow pointing down could otherwise loop for long). */
    private static final int MAX_TICKS = 200;
    private static final double DOT = 0.03;
    private static final float DOT_ALPHA = 0.9F;
    /** Same muzzle offset {@link VrAim} reports to the server. */
    private static final double MUZZLE_FORWARD = 0.3;
    /** Half the bar's side in room-scale metres: the attachment's 0.16 pixels at the default gun scale. */
    private static final double HALF_WIDTH = 0.003;
    /** Half the bar's side per block from the eye, so it stays about a pixel wide in the distance instead of breaking up. */
    private static final double HALF_WIDTH_PER_BLOCK = 0.0006;
    private static final float RED = 1.0F;
    /** Sampled along the beam in {@code laser_pointer.png}: place from muzzle (0) to tip (1), green and blue, alpha. */
    private static final float[] PLACES = {0.0F, 0.03F, 0.06F, 0.1F, 0.13F, 0.23F, 0.35F, 0.55F, 0.68F, 0.74F, 0.84F, 0.9F, 1.0F};
    private static final float[] TINTS = {0.78F, 0.55F, 0.43F, 0.32F, 0.29F, 0.27F, 0.26F, 0.23F, 0.19F, 0.19F, 0.09F, 0.0F, 0.0F};
    private static final float[] ALPHAS = {1.0F, 1.0F, 1.0F, 0.76F, 0.64F, 0.58F, 0.51F, 0.42F, 0.33F, 0.22F, 0.09F, 0.05F, 0.0F};
    /** How far off the block's face JEG puts its dot ({@code ClientPlayHandler.handleLaser}). */
    private static final double DOT_LIFT = 0.005;

    private static final long[] LAST_DOT_TICK = {-1L, -1L};

    private VrBowLaser() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !VrClientConfig.bowLaser()
                || !VR.isEyePass()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !VR.active()) {
            return;
        }
        if (ChargeTracker.getChargeTime(player.getUUID()) > 0) {
            drawBeam(event, mc, player, InteractionHand.MAIN_HAND, VR.mainHandAim());
        }
        if (VrOffhandShooting.triggerTakenOver() && VrOffhandShooting.holdFire() > 0) {
            drawBeam(event, mc, player, InteractionHand.OFF_HAND, VR.offHandAim());
        }
    }

    /** The beam of the bow in {@code hand}, which is being drawn. */
    private static void drawBeam(RenderLevelStageEvent event, Minecraft mc, LocalPlayer player, InteractionHand hand,
                                 VR.Aim handAim) {
        ItemStack bow = player.getItemInHand(hand);
        if (!(bow.getItem() instanceof GunItem gunItem) || !BOW_ID.equals(VrGunPose.gunId(bow))) {
            return;
        }
        VR.Aim aim = VrPrecision.aim(hand, handAim);
        if (aim == null || aim.dir().lengthSqr() < 1.0E-6) {
            return;
        }
        Gun.Projectile projectile = gunItem.getModifiedGun(bow).getProjectile();
        double speed = GunModifierHelper.getModifiedProjectileSpeed(bow,
                projectile.getSpeed() * GunEnchantmentHelper.getProjectileSpeedModifier(bow));
        double gravity = projectile.isGravity() ? GunModifierHelper.getModifiedProjectileGravity(bow, JEG_GRAVITY) : 0.0;
        Vec3 dir = aim.dir().normalize();
        double worldScale = VR.worldScale();
        Vec3 start = aim.pos().add(dir.scale(MUZZLE_FORWARD * worldScale));

        List<Vec3> flight = new ArrayList<>();
        flight.add(start);
        HitResult hit = trace(player, start, dir.scale(speed), gravity, flight);

        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        // A shader pack lights the debug quads like anything else, so the beam would go dark at night; lightning is its own
        // full-bright program there. Both take the same vertices.
        RenderType beam = VR.shadersActive() ? RenderType.lightning() : RenderType.debugQuads();
        drawFlight(pose.last().pose(), buffers.getBuffer(beam), flight, cam, worldScale);
        buffers.endBatch(beam);
        if (hit instanceof BlockHitResult block) {
            jegDot(mc, hand, block);
        } else if (hit != null) {
            Vec3 at = hit.getLocation();
            double half = DOT * worldScale;
            LevelRenderer.addChainedFilledBoxVertices(pose, buffers.getBuffer(RenderType.debugFilledBox()),
                    at.x - half, at.y - half, at.z - half, at.x + half, at.y + half, at.z + half,
                    RED, TINTS[5], TINTS[5], DOT_ALPHA);
            buffers.endBatch(RenderType.debugFilledBox());
        }
        pose.popPose();
    }

    /** JEG's red dot on a block: a particle that lives for one tick, so it is added once per tick, as JEG does. */
    private static void jegDot(Minecraft mc, InteractionHand hand, BlockHitResult block) {
        long now = mc.level.getGameTime();
        if (LAST_DOT_TICK[hand.ordinal()] == now) {
            return;
        }
        LAST_DOT_TICK[hand.ordinal()] = now;
        Direction face = block.getDirection();
        Vec3 at = block.getLocation().add(face.getStepX() * DOT_LIFT, face.getStepY() * DOT_LIFT, face.getStepZ() * DOT_LIFT);
        mc.level.addParticle(new LaserData(face, block.getBlockPos()), false, at.x, at.y, at.z, 0.0, 0.0, 0.0);
    }

    /** Follows the flight tick by tick, adding each point to {@code flight}; returns what it hits within the beam's length. */
    private static HitResult trace(LocalPlayer player, Vec3 start, Vec3 velocity, double gravity, List<Vec3> flight) {
        Vec3 pos = start;
        Vec3 vel = velocity;
        double travelled = 0.0;
        for (int tick = 0; tick < MAX_TICKS && travelled < LENGTH; tick++) {
            double step = vel.length();
            if (step < 1.0E-6) {
                return null;
            }
            // The part of this tick's segment still inside the beam.
            Vec3 end = pos.add(vel.scale(Math.min(1.0, (LENGTH - travelled) / step)));
            HitResult hit = firstHit(player, pos, end);
            if (hit != null) {
                flight.add(hit.getLocation());
                return hit;
            }
            flight.add(end);
            travelled += pos.distanceTo(end);
            pos = end;
            vel = vel.add(0.0, gravity, 0.0);
        }
        return null;
    }

    /** First block or entity between two points, as JEG's projectile would meet it. */
    private static HitResult firstHit(LocalPlayer player, Vec3 from, Vec3 to) {
        BlockHitResult block = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        boolean hitsBlock = block.getType() != HitResult.Type.MISS;
        Vec3 end = hitsBlock ? block.getLocation() : to;
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(player.level(), player, from, end,
                new AABB(from, end).inflate(1.0), VrBowLaser::canHit);
        if (entity != null) {
            return entity;
        }
        return hitsBlock ? block : null;
    }

    private static boolean canHit(Entity entity) {
        return !entity.isSpectator() && entity.isPickable();
    }

    /** The whole beam; the gradient runs from its start to wherever it ends, as the texture does on JEG's. */
    private static void drawFlight(Matrix4f matrix, VertexConsumer quads, List<Vec3> flight, Vec3 cam, double worldScale) {
        double total = 0.0;
        for (int i = 1; i < flight.size(); i++) {
            total += flight.get(i - 1).distanceTo(flight.get(i));
        }
        if (total < 1.0E-6) {
            return;
        }
        double travelled = 0.0;
        for (int i = 1; i < flight.size(); i++) {
            Vec3 from = flight.get(i - 1);
            Vec3 to = flight.get(i);
            drawSegment(matrix, quads, from, to, travelled, total, cam, worldScale);
            travelled += from.distanceTo(to);
        }
    }

    /** One straight piece of the flight, split so the gradient and the width change smoothly along it. */
    private static void drawSegment(Matrix4f matrix, VertexConsumer quads, Vec3 from, Vec3 to, double travelled, double total,
                                    Vec3 cam, double worldScale) {
        double length = from.distanceTo(to);
        if (length < 1.0E-6) {
            return;
        }
        Vec3 along = to.subtract(from).normalize();
        Vec3 side = along.cross(Math.abs(along.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0)).normalize();
        Vec3 up = side.cross(along);
        int pieces = Math.max(1, (int) Math.ceil(length / PIECE));
        for (int i = 0; i < pieces; i++) {
            Vec3 a = from.lerp(to, (double) i / pieces);
            Vec3 b = from.lerp(to, (double) (i + 1) / pieces);
            float placeA = (float) ((travelled + length * i / pieces) / total);
            float placeB = (float) ((travelled + length * (i + 1) / pieces) / total);
            double half = HALF_WIDTH * worldScale + HALF_WIDTH_PER_BLOCK * a.distanceTo(cam);
            Vec3 s = side.scale(half);
            Vec3 u = up.scale(half);
            face(matrix, quads, a, b, s, u, placeA, placeB);
            face(matrix, quads, a, b, u, s.scale(-1.0), placeA, placeB);
            face(matrix, quads, a, b, s.scale(-1.0), u.scale(-1.0), placeA, placeB);
            face(matrix, quads, a, b, u.scale(-1.0), s, placeA, placeB);
        }
    }

    /** One long side of the bar: from {@code offset} + {@code width} to {@code offset} - {@code width} around the axis. */
    private static void face(Matrix4f matrix, VertexConsumer quads, Vec3 a, Vec3 b, Vec3 offset, Vec3 width, float placeA, float placeB) {
        vertex(matrix, quads, a.add(offset).add(width), placeA);
        vertex(matrix, quads, b.add(offset).add(width), placeB);
        vertex(matrix, quads, b.add(offset).subtract(width), placeB);
        vertex(matrix, quads, a.add(offset).subtract(width), placeA);
    }

    private static void vertex(Matrix4f matrix, VertexConsumer quads, Vec3 p, float place) {
        float tint = sample(TINTS, place);
        quads.vertex(matrix, (float) p.x, (float) p.y, (float) p.z).color(RED, tint, tint, sample(ALPHAS, place)).endVertex();
    }

    private static float sample(float[] values, float place) {
        for (int i = 1; i < PLACES.length; i++) {
            if (place <= PLACES[i]) {
                float t = (place - PLACES[i - 1]) / (PLACES[i] - PLACES[i - 1]);
                return values[i - 1] + (values[i] - values[i - 1]) * t;
            }
        }
        return values[values.length - 1];
    }
}
