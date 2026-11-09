package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.compat.jeg.JegKeys;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * F10.2: JEG's ammo box block drawn at the hip (or in front of the belly) while a gun is held, in place of the guide
 * box. Its lid opens when a magazine can be taken and closes when not (gun full, no ammo, reloading, hand busy).
 * Only looks: the box the hand must enter is still {@link VrZones#pouch}.
 */
final class VrAmmoBox {
    /** Width of the drawn box (lid included), metres in the room. */
    private static final float WIDTH = 0.24F;
    /** The model's lid is 14/16 of a block wide; its height is 9/16, centred on the zone. */
    private static final float MODEL_WIDTH = 14.0F / 16.0F;
    private static final float MODEL_HALF_HEIGHT = 4.5F / 16.0F;
    /** The lid: everything above the body's top, hinged on the back edge. */
    private static final float LID_BOTTOM = 7.0F / 16.0F;
    private static final float HINGE_Z = 11.0F / 16.0F;
    private static final float LID_OPEN_DEGREES = 110.0F;
    /** The lid opens or closes in this many ticks. */
    private static final float LID_STEP = 1.0F / 4.0F;
    private static final float EPS = 1.0E-3F;
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

    private static float lid;
    private static float lidBefore;
    private static boolean wasVisible;
    private static boolean wasOpen;

    private static BakedModel cachedModel;
    private static final List<BakedQuad> BODY = new ArrayList<>();
    private static final List<BakedQuad> LID = new ArrayList<>();

    private VrAmmoBox() {
    }

    /** Once per client tick: moves the lid towards open or closed, with a short sound when it changes. */
    static void tick(boolean visible, boolean open) {
        lidBefore = lid;
        if (!visible) {
            lid = 0.0F;
            lidBefore = 0.0F;
        } else {
            lid = Mth.clamp(lid + (open ? LID_STEP : -LID_STEP), 0.0F, 1.0F);
            if (wasVisible && open != wasOpen && Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 0.35F, open ? 1.6F : 1.3F);
            }
        }
        wasVisible = visible;
        wasOpen = visible && open;
    }

    /**
     * Draws the box centred in {@code zone}, its front facing {@code outward} (away from the body). The pose is in world
     * coordinates already moved by -camera. Returns where the open box's glow goes (the top of its body), or null when
     * JEG's block is missing.
     */
    static Vec3 draw(PoseStack pose, MultiBufferSource buffers, AABB zone, Vec3 outward, float partialTick) {
        if (zone == null || outward == null || outward.lengthSqr() < EPS || !loadModel()) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        Vec3 center = zone.getCenter();
        float scale = WIDTH * VR.worldScale() / MODEL_WIDTH;
        int light = mc.level == null ? 0xF000F0 : LevelRenderer.getLightColor(mc.level, BlockPos.containing(center));
        VertexConsumer consumer = buffers.getBuffer(Sheets.cutoutBlockSheet());

        pose.pushPose();
        pose.translate(center.x, center.y, center.z);
        // Model north (its front, -z) points outward; +y stays up.
        pose.mulPose(VrGunHud.facing(outward.normalize().scale(-1.0), UP));
        pose.scale(scale, scale, scale);
        pose.translate(-0.5F, -MODEL_HALF_HEIGHT, -0.5F);
        for (BakedQuad quad : BODY) {
            consumer.putBulkData(pose.last(), quad, 1.0F, 1.0F, 1.0F, light, OverlayTexture.NO_OVERLAY);
        }
        float open = Mth.lerp(partialTick, lidBefore, lid);
        pose.pushPose();
        pose.translate(0.0F, LID_BOTTOM, HINGE_Z);
        pose.mulPose(Axis.XP.rotationDegrees(LID_OPEN_DEGREES * smooth(open)));
        pose.translate(0.0F, -LID_BOTTOM, -HINGE_Z);
        for (BakedQuad quad : LID) {
            consumer.putBulkData(pose.last(), quad, 1.0F, 1.0F, 1.0F, light, OverlayTexture.NO_OVERLAY);
        }
        pose.popPose();
        pose.popPose();
        return center.add(0.0, (LID_BOTTOM - MODEL_HALF_HEIGHT) * scale, 0.0);
    }

    /** The lid is (nearly) open: the glow may show. */
    static boolean lidOpen() {
        return lid > 0.5F;
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    /** Splits JEG's block model into body and lid, again whenever the models are reloaded (resource packs). */
    private static boolean loadModel() {
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(JegKeys.AMMO_BOX_BLOCK));
        if (block == null || block == Blocks.AIR) {
            return false;
        }
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        }
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        if (model != cachedModel) {
            cachedModel = model;
            BODY.clear();
            LID.clear();
            RandomSource random = RandomSource.create(42L);
            List<BakedQuad> quads = new ArrayList<>(model.getQuads(state, null, random));
            for (Direction direction : Direction.values()) {
                quads.addAll(model.getQuads(state, direction, random));
            }
            for (BakedQuad quad : quads) {
                (isLid(quad) ? LID : BODY).add(quad);
            }
        }
        return !BODY.isEmpty();
    }

    /**
     * A lid face lies wholly at or above the body's top and is either higher (lid sides, top, handle) or the lid's
     * underside. The body's own top sits at the same height but faces up, and stays with the body.
     */
    private static boolean isLid(BakedQuad quad) {
        int[] data = quad.getVertices();
        int stride = data.length / 4;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float y = Float.intBitsToFloat(data[i * stride + 1]);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        return minY >= LID_BOTTOM - EPS && (maxY > LID_BOTTOM + EPS || quad.getDirection() == Direction.DOWN);
    }
}
