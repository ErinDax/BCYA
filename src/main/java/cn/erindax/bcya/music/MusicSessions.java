package cn.erindax.bcya.music;

import cn.erindax.bcya.music.net.MusicControlPayload;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

public final class MusicSessions {

	private static final double NOTIFY_MARGIN = 48.0;

	public static final class Session {
		private final UUID id;
		private final ResourceKey<Level> dimension;
		private final String track;
		private final int range;
		@Nullable
		private final UUID player;
		@Nullable
		private final BlockPos pos;
		private boolean paused;

		private Session(UUID id, ResourceKey<Level> dimension, String track, int range, @Nullable UUID player,
				@Nullable BlockPos pos) {
			this.id = id;
			this.dimension = dimension;
			this.track = track;
			this.range = range;
			this.player = player;
			this.pos = pos;
		}

		public UUID id() {
			return id;
		}

		public String track() {
			return track;
		}

		public boolean paused() {
			return paused;
		}
	}

	private static final Map<UUID, Session> SESSIONS = new HashMap<>();
	private static final Map<UUID, UUID> BY_PLAYER = new HashMap<>();
	private static final Map<GlobalPos, UUID> BY_BLOCK = new HashMap<>();

	private MusicSessions() {
	}

	@Nullable
	public static Session forPlayer(UUID playerId) {
		UUID id = BY_PLAYER.get(playerId);
		return id == null ? null : SESSIONS.get(id);
	}

	public static boolean playForPlayer(ServerPlayer player, String track, int range) {
		MusicStore.Entry entry = MusicStore.load(track);
		if (entry == null) {
			return false;
		}
		stopForPlayer(player.server, player.getUUID());
		ServerLevel level = player.serverLevel();
		Session session = new Session(UUID.randomUUID(), level.dimension(), track, range, player.getUUID(), null);
		SESSIONS.put(session.id, session);
		BY_PLAYER.put(player.getUUID(), session.id);
		broadcastPlay(level, session, entry, player.getId(), player.blockPosition(), player.position());
		return true;
	}

	public static boolean playForBlock(ServerLevel level, BlockPos pos, String track, int range) {
		MusicStore.Entry entry = MusicStore.load(track);
		if (entry == null) {
			return false;
		}
		stopForBlock(level, pos);
		Session session = new Session(UUID.randomUUID(), level.dimension(), track, range, null, pos.immutable());
		SESSIONS.put(session.id, session);
		BY_BLOCK.put(GlobalPos.of(level.dimension(), pos.immutable()), session.id);
		broadcastPlay(level, session, entry, MusicControlPayload.NO_ENTITY, pos, pos.getCenter());
		return true;
	}

	public static void pausePlayer(ServerPlayer player) {
		Session session = forPlayer(player.getUUID());
		if (session != null && !session.paused) {
			session.paused = true;
			broadcast(player.server, session, MusicControlPayload.simple(session.id, MusicControlPayload.Action.PAUSE));
		}
	}

	public static void resumePlayer(ServerPlayer player) {
		Session session = forPlayer(player.getUUID());
		if (session != null && session.paused) {
			session.paused = false;
			broadcast(player.server, session, MusicControlPayload.simple(session.id, MusicControlPayload.Action.RESUME));
		}
	}

	public static void stopForPlayer(MinecraftServer server, UUID playerId) {
		UUID id = BY_PLAYER.remove(playerId);
		if (id != null) {
			stop(server, id);
		}
	}

	public static void stopForBlock(ServerLevel level, BlockPos pos) {
		UUID id = BY_BLOCK.remove(GlobalPos.of(level.dimension(), pos.immutable()));
		if (id != null) {
			stop(level.getServer(), id);
		}
	}

	private static void stop(MinecraftServer server, UUID sessionId) {
		Session session = SESSIONS.remove(sessionId);
		if (session != null) {
			broadcast(server, session, MusicControlPayload.simple(session.id, MusicControlPayload.Action.STOP));
		}
	}

	private static void broadcastPlay(ServerLevel level, Session session, MusicStore.Entry entry, int entityId,
			BlockPos pos, Vec3 origin) {
		MusicControlPayload payload = new MusicControlPayload(session.id, MusicControlPayload.Action.PLAY, session.track,
			entry.version(), session.range, entityId, pos);
		double reach = session.range + NOTIFY_MARGIN;
		for (ServerPlayer target : level.players()) {
			if (target.position().distanceToSqr(origin) <= reach * reach) {
				ServerPlayNetworking.send(target, payload);
			}
		}
	}

	private static void broadcast(MinecraftServer server, Session session, MusicControlPayload payload) {
		ServerLevel level = server.getLevel(session.dimension);
		if (level == null) {
			return;
		}
		for (ServerPlayer target : level.players()) {
			ServerPlayNetworking.send(target, payload);
		}
	}
}
