package cn.erindax.bcya.client.music;

import cn.erindax.bcya.music.net.MusicDataPayload;
import cn.erindax.bcya.music.net.MusicRequestPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

public final class ClientMusic {

	private static final int MAX_CACHED_TRACKS = 12;

	private record Cached(int version, byte[] data) {
	}

	private static final class Assembly {
		private final int version;
		private final byte[][] parts;
		private int received;

		private Assembly(int version, int total) {
			this.version = version;
			this.parts = new byte[total][];
		}
	}

	private static final Map<String, Cached> CACHE = new LinkedHashMap<>(16, 0.75F, true);
	private static final Map<String, Assembly> PENDING = new HashMap<>();
	private static final Set<String> REQUESTED = new HashSet<>();

	private ClientMusic() {
	}

	public static boolean has(String track, int version) {
		Cached cached;
		synchronized (CACHE) {
			cached = CACHE.get(track);
		}
		return cached != null && cached.version == version;
	}

	@Nullable
	public static byte[] get(String track) {
		synchronized (CACHE) {
			Cached cached = CACHE.get(track);
			return cached == null ? null : cached.data;
		}
	}

	public static void request(String track) {
		if (REQUESTED.add(track)) {
			ClientPlayNetworking.send(new MusicRequestPayload(track));
		}
	}

	public static void receive(MusicDataPayload payload) {
		if (payload.total() <= 0 || payload.index() < 0 || payload.index() >= payload.total()) {
			return;
		}
		Assembly assembly = PENDING.get(payload.track());
		if (assembly == null || assembly.version != payload.version() || assembly.parts.length != payload.total()) {
			assembly = new Assembly(payload.version(), payload.total());
			PENDING.put(payload.track(), assembly);
		}
		if (assembly.parts[payload.index()] == null) {
			assembly.parts[payload.index()] = payload.data();
			assembly.received++;
		}
		if (assembly.received < assembly.parts.length) {
			return;
		}
		int length = 0;
		for (byte[] part : assembly.parts) {
			length += part.length;
		}
		byte[] data = new byte[length];
		int offset = 0;
		for (byte[] part : assembly.parts) {
			System.arraycopy(part, 0, data, offset, part.length);
			offset += part.length;
		}
		synchronized (CACHE) {
			CACHE.put(payload.track(), new Cached(assembly.version, data));
			while (CACHE.size() > MAX_CACHED_TRACKS) {
				String eldest = CACHE.keySet().iterator().next();
				if (MusicPlayer.isTrackInUse(eldest)) {
					break;
				}
				CACHE.remove(eldest);
			}
		}
		PENDING.remove(payload.track());
		REQUESTED.remove(payload.track());
		MusicPlayer.onTrackReady(payload.track());
	}

	public static void clear() {
		synchronized (CACHE) {
			CACHE.clear();
		}
		PENDING.clear();
		REQUESTED.clear();
	}
}
