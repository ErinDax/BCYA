package cn.erindax.bcya.client.lock;

import cn.erindax.bcya.BcyaMod;
import cn.erindax.bcya.client.render.IconBillboard;
import cn.erindax.bcya.lock.LockType;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;

public final class LockRenderer {

	private static final ResourceLocation KEY_LOCK = BcyaMod.id("textures/item/key_lock.png");
	private static final ResourceLocation PASSWORD_LOCK = BcyaMod.id("textures/item/password_lock.png");
	private static final double MAX_DISTANCE_SQR = 48.0 * 48.0;
	private static final float SIZE = 0.4F;

	private LockRenderer() {
	}

	public static void init() {
		WorldRenderEvents.AFTER_ENTITIES.register(LockRenderer::render);
	}

	private static void render(WorldRenderContext context) {
		if (ClientLocks.all().isEmpty()) {
			return;
		}
		ClientLevel level = context.world();
		Camera camera = context.camera();
		Vec3 cam = camera.getPosition();
		PoseStack poseStack = context.matrixStack();
		MultiBufferSource buffers = context.consumers();
		if (level == null || poseStack == null || buffers == null) {
			return;
		}
		for (Map.Entry<BlockPos, LockType> entry : ClientLocks.all().entrySet()) {
			BlockPos pos = entry.getKey();
			if (pos.distToCenterSqr(cam) > MAX_DISTANCE_SQR) {
				continue;
			}
			ResourceLocation texture = entry.getValue() == LockType.KEY ? KEY_LOCK : PASSWORD_LOCK;
			VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
			for (Vec3 anchor : anchors(level.getBlockState(pos), pos)) {
				IconBillboard.draw(poseStack, consumer, camera, anchor.subtract(cam), SIZE);
			}
		}
	}

	private static List<Vec3> anchors(BlockState state, BlockPos pos) {
		List<Vec3> result = new ArrayList<>(2);
		Vec3 center = pos.getCenter();
		if (state.getBlock() instanceof DoorBlock) {
			Vec3 facing = Vec3.atLowerCornerOf(state.getValue(DoorBlock.FACING).getNormal());
			result.add(center.add(facing.scale(0.6)).add(0.0, 0.5, 0.0));
			result.add(center.subtract(facing.scale(0.6)).add(0.0, 0.5, 0.0));
		} else if (state.getBlock() instanceof TrapDoorBlock) {
			result.add(center);
		} else if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			Direction other = ChestBlock.getConnectedDirection(state);
			result.add(center.add(Vec3.atLowerCornerOf(other.getNormal()).scale(0.5)).add(0.0, 0.75, 0.0));
		} else {
			result.add(center.add(0.0, 0.75, 0.0));
		}
		return result;
	}
}
