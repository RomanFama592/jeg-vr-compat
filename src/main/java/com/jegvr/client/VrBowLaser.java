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

/**
 * F9.15: laser sight on JEG's compound bow. While the bow is drawn, a beam leaves the muzzle and follows the arrow's
 * predicted flight for up to {@link #LENGTH} blocks, fading out over the last {@link #FADE} blocks, and stops at the
 * first block or entity the arrow would hit.
 * <p>
 * It looks like JEG's laser pointer attachment: a thin solid bar, as thick as the attachment's in a VR hand, almost white
 * at the muzzle and then translucent red (the colours of {@code laser_pointer.png}), and on a block JEG's own red dot.
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
    private static final double FADE = 5.0;
    /** The beam is drawn in pieces this long so the fade and the arc look smooth. */
    private static final double PIECE = 0.25;
    /** Simulated ticks at most (a slow arrow pointing down could otherwise loop for long). */
    private static final int MAX_TICKS = 200;
    private static final double DOT = 0.03;
    /** Same muzzle offset {@link VrAim} reports to the server. */
    private static final double MUZZLE_FORWARD = 0.3;
    /** Half the bar's side in room-scale metres: the attachment's 0.16 pixels at the default gun scale. */
    private static final double HALF_WIDTH = 0.003;
    /** Half the bar's side per block from the eye, so it stays about a pixel wide in the distance instead of breaking up. */
    private static final double HALF_WIDTH_PER_BLOCK = 0.0006;
    /** How far from the muzzle the beam goes from almost white to its red. */
    private static final double HOT_LENGTH = 0.4;
    private static final float RED = 1.0F;
    private static final float TINT_HOT = 0.78F;
    private static final float TINT = 0.27F;
    private static final float ALPHA_NEAR = 0.65F;
    private static final float ALPHA_FAR = 0.45F;
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

        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        VertexConsumer quads = buffers.getBuffer(RenderType.debugQuads());
        HitResult hit = trace(player, start, dir.scale(speed), gravity, cam, worldScale, pose.last().pose(), quads);
        buffers.endBatch(RenderType.debugQuads());
        if (hit instanceof BlockHitResult block) {
            jegDot(mc, hand, block);
        } else if (hit != null) {
            Vec3 at = hit.getLocation();
            double half = DOT * worldScale;
            LevelRenderer.addChainedFilledBoxVertices(pose, buffers.getBuffer(RenderType.debugFilledBox()),
                    at.x - half, at.y - half, at.z - half, at.x + half, at.y + half, at.z + half,
                    RED, TINT, TINT, alphaAt(start.distanceTo(at)));
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

    /** Draws the flight tick by tick; returns what it hits within the beam's length, or null. */
    private static HitResult trace(LocalPlayer player, Vec3 start, Vec3 velocity, double gravity, Vec3 cam, double worldScale,
                                   Matrix4f matrix, VertexConsumer quads) {
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
            drawSegment(matrix, quads, pos, hit != null ? hit.getLocation() : end, travelled, cam, worldScale);
            if (hit != null) {
                return hit;
            }
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

    /** One straight piece of the flight, split so each piece gets the look of its distance along the beam. */
    private static void drawSegment(Matrix4f matrix, VertexConsumer quads, Vec3 from, Vec3 to, double travelled, Vec3 cam,
                                    double worldScale) {
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
            double distance = travelled + length * i / pieces;
            double half = HALF_WIDTH * worldScale + HALF_WIDTH_PER_BLOCK * a.distanceTo(cam);
            Vec3 s = side.scale(half);
            Vec3 u = up.scale(half);
            float tint = tintAt(distance);
            float alpha = alphaAt(distance);
            face(matrix, quads, a, b, s, u, tint, alpha);
            face(matrix, quads, a, b, u, s.scale(-1.0), tint, alpha);
            face(matrix, quads, a, b, s.scale(-1.0), u.scale(-1.0), tint, alpha);
            face(matrix, quads, a, b, u.scale(-1.0), s, tint, alpha);
        }
    }

    /** One long side of the bar: from {@code offset} + {@code width} to {@code offset} - {@code width} around the axis. */
    private static void face(Matrix4f matrix, VertexConsumer quads, Vec3 a, Vec3 b, Vec3 offset, Vec3 width, float tint, float alpha) {
        vertex(matrix, quads, a.add(offset).add(width), tint, alpha);
        vertex(matrix, quads, b.add(offset).add(width), tint, alpha);
        vertex(matrix, quads, b.add(offset).subtract(width), tint, alpha);
        vertex(matrix, quads, a.add(offset).subtract(width), tint, alpha);
    }

    private static void vertex(Matrix4f matrix, VertexConsumer quads, Vec3 p, float tint, float alpha) {
        quads.vertex(matrix, (float) p.x, (float) p.y, (float) p.z).color(RED, tint, tint, alpha).endVertex();
    }

    /** Green and blue of the colour: nearly white at the muzzle, then red. */
    private static float tintAt(double distance) {
        return distance >= HOT_LENGTH ? TINT : (float) (TINT_HOT + (TINT - TINT_HOT) * distance / HOT_LENGTH);
    }

    /** Solid at the muzzle, translucent along the beam, and fading to nothing from LENGTH - FADE to LENGTH. */
    private static float alphaAt(double distance) {
        float alpha = distance >= HOT_LENGTH ? ALPHA_NEAR + (ALPHA_FAR - ALPHA_NEAR) * (float) (distance / LENGTH)
                : 1.0F + (ALPHA_NEAR - 1.0F) * (float) (distance / HOT_LENGTH);
        double fadeStart = LENGTH - FADE;
        return distance <= fadeStart ? alpha : (float) Math.max(0.0, alpha * (1.0 - (distance - fadeStart) / FADE));
    }
}
