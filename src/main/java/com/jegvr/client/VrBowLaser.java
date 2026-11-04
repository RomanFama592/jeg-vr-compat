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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import ttv.migami.jeg.common.ChargeTracker;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.util.GunEnchantmentHelper;
import ttv.migami.jeg.util.GunModifierHelper;

/**
 * F9.15: laser sight on JEG's compound bow. While the bow is drawn, a beam leaves the muzzle and follows the arrow's
 * predicted flight for up to {@link #LENGTH} blocks, fading out over the last {@link #FADE} blocks, and stops (with a
 * dot) at the first block or entity the arrow would hit.
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
    private static final double DOT = 0.04;
    /** Same muzzle offset {@link VrAim} reports to the server. */
    private static final double MUZZLE_FORWARD = 0.3;
    private static final float RED = 1.0F;
    private static final float GREEN = 0.1F;
    private static final float BLUE = 0.1F;

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
        Vec3 start = aim.pos().add(dir.scale(MUZZLE_FORWARD * VR.worldScale()));

        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        Vec3 hit = trace(player, start, dir.scale(speed), gravity, pose, lines);
        buffers.endBatch(RenderType.lines());
        if (hit != null) {
            Vec3 h = new Vec3(DOT, DOT, DOT).scale(VR.worldScale());
            float alpha = alphaAt(start.distanceTo(hit));
            LevelRenderer.addChainedFilledBoxVertices(pose, buffers.getBuffer(RenderType.debugFilledBox()),
                    hit.x - h.x, hit.y - h.y, hit.z - h.z, hit.x + h.x, hit.y + h.y, hit.z + h.z, RED, GREEN, BLUE, alpha);
            buffers.endBatch(RenderType.debugFilledBox());
        }
        pose.popPose();
    }

    /** Draws the flight tick by tick; returns where it hits something within the beam's length, or null. */
    private static Vec3 trace(LocalPlayer player, Vec3 start, Vec3 velocity, double gravity, PoseStack pose,
                              VertexConsumer lines) {
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
            Vec3 hit = firstHit(player, pos, end);
            Vec3 drawTo = hit != null ? hit : end;
            drawSegment(pose, lines, pos, drawTo, travelled);
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
    private static Vec3 firstHit(LocalPlayer player, Vec3 from, Vec3 to) {
        HitResult block = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        Vec3 end = block.getType() != HitResult.Type.MISS ? block.getLocation() : to;
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(player.level(), player, from, end,
                new AABB(from, end).inflate(1.0), VrBowLaser::canHit);
        if (entity != null) {
            return entity.getLocation();
        }
        return block.getType() != HitResult.Type.MISS ? end : null;
    }

    private static boolean canHit(Entity entity) {
        return !entity.isSpectator() && entity.isPickable();
    }

    /** One straight piece of the flight, split so each piece gets the alpha of its distance along the beam. */
    private static void drawSegment(PoseStack pose, VertexConsumer lines, Vec3 from, Vec3 to, double travelled) {
        double length = from.distanceTo(to);
        if (length < 1.0E-6) {
            return;
        }
        Vec3 normal = to.subtract(from).normalize();
        int pieces = Math.max(1, (int) Math.ceil(length / PIECE));
        Matrix4f matrix = pose.last().pose();
        Matrix3f normals = pose.last().normal();
        for (int i = 0; i < pieces; i++) {
            double t0 = (double) i / pieces;
            double t1 = (double) (i + 1) / pieces;
            Vec3 a = from.lerp(to, t0);
            Vec3 b = from.lerp(to, t1);
            vertex(lines, matrix, normals, a, normal, alphaAt(travelled + length * t0));
            vertex(lines, matrix, normals, b, normal, alphaAt(travelled + length * t1));
        }
    }

    private static void vertex(VertexConsumer lines, Matrix4f matrix, Matrix3f normals, Vec3 p, Vec3 n, float alpha) {
        lines.vertex(matrix, (float) p.x, (float) p.y, (float) p.z)
                .color(RED, GREEN, BLUE, alpha)
                .normal(normals, (float) n.x, (float) n.y, (float) n.z)
                .endVertex();
    }

    /** Fully visible up to LENGTH - FADE blocks, then fades to nothing at LENGTH. */
    private static float alphaAt(double distance) {
        double fadeStart = LENGTH - FADE;
        if (distance <= fadeStart) {
            return 1.0F;
        }
        return (float) Math.max(0.0, 1.0 - (distance - fadeStart) / FADE);
    }
}
