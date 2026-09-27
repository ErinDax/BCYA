package cn.erindax.bcya.skywalk;

import cn.erindax.bcya.skywalk.net.SkywalkSyncPayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class Skywalk {

	public static final float LOOK_DOWN_PITCH = 60.0F;
	public static final int DEFAULT_DEPTH = 9;
	public static final int MIN_DEPTH = 1;
	public static final int MAX_DEPTH = 64;

	private Skywalk() {
	}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(SkywalkSyncPayload.TYPE, SkywalkSyncPayload.STREAM_CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (ServerPlayNetworking.canSend(handler.player, SkywalkSyncPayload.TYPE)) {
				sender.sendPacket(payload(server));
			}
		});
	}

	public static boolean enabled(MinecraftServer server) {
		return SkywalkState.get(server).enabled();
	}

	public static int depth(MinecraftServer server) {
		return SkywalkState.get(server).depth();
	}

	public static void setEnabled(MinecraftServer server, boolean enabled) {
		SkywalkState.get(server).setEnabled(enabled);
		sync(server);
	}

	public static void setDepth(MinecraftServer server, int depth) {
		SkywalkState.get(server).setDepth(depth);
		sync(server);
	}

	private static SkywalkSyncPayload payload(MinecraftServer server) {
		SkywalkState state = SkywalkState.get(server);
		return new SkywalkSyncPayload(state.enabled(), state.depth());
	}

	private static void sync(MinecraftServer server) {
		SkywalkSyncPayload payload = payload(server);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (ServerPlayNetworking.canSend(player, SkywalkSyncPayload.TYPE)) {
				ServerPlayNetworking.send(player, payload);
			}
		}
	}

	public static boolean canHover(Player player) {
		return !player.onGround() && !player.isSpectator() && !player.getAbilities().flying && !player.isFallFlying()
			&& !player.isPassenger() && !player.onClimbable() && !player.isInWater() && !player.isInLava()
			&& !player.isSleeping() && !player.hasEffect(MobEffects.LEVITATION);
	}

	public static boolean hasAirBelow(Level level, Entity entity, int depth) {
		BlockPos top = BlockPos.containing(entity.getX(), entity.getY() - 0.001, entity.getZ());
		for (int offset = 0; offset < depth; offset++) {
			BlockPos pos = top.below(offset);
			BlockState state = level.getBlockState(pos);
			if (!state.getCollisionShape(level, pos).isEmpty() || !state.getFluidState().isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public static boolean isHovering(ServerPlayer player) {
		return enabled(player.server) && canHover(player) && player.getXRot() < LOOK_DOWN_PITCH
			&& hasAirBelow(player.level(), player, depth(player.server));
	}
}
