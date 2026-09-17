package cn.erindax.bcya.music;

import cn.erindax.bcya.BcyaMod;
import cn.erindax.bcya.music.net.MusicDataPayload;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.jetbrains.annotations.Nullable;

public final class MusicStore {

	public static final int MAX_BYTES = 24 * 1024 * 1024;
	public static final int CHUNK_BYTES = 256 * 1024;

	private static final Pattern NAME = Pattern.compile("[^\\\\/:*?\"<>|\\p{Cntrl}]{1,64}");
	private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();
	private static final Map<UUID, Deque<MusicDataPayload>> QUEUES = new HashMap<>();

	public record Entry(byte[] data, int version, long modified, long size) {
	}

	private MusicStore() {
	}

	public static Path directory() {
		return FabricLoader.getInstance().getConfigDir().resolve("bcya").resolve("music");
	}

	public static boolean isSupported(String name) {
		String lower = name.toLowerCase(Locale.ROOT);
		return lower.endsWith(".ogg") || lower.endsWith(".mp3");
	}

	public static boolean isValidName(String name) {
		return NAME.matcher(name).matches() && !name.equals(".") && !name.equals("..") && isSupported(name);
	}

	public static boolean looksValid(String name, byte[] data) {
		if (data.length < 4 || data.length > MAX_BYTES) {
			return false;
		}
		if (name.toLowerCase(Locale.ROOT).endsWith(".ogg")) {
			return data[0] == 'O' && data[1] == 'g' && data[2] == 'g' && data[3] == 'S';
		}
		boolean id3 = data[0] == 'I' && data[1] == 'D' && data[2] == '3';
		boolean sync = (data[0] & 0xFF) == 0xFF && (data[1] & 0xE0) == 0xE0;
		return id3 || sync;
	}

	public static void save(String name, byte[] data) throws IOException {
		Files.createDirectories(directory());
		Files.write(directory().resolve(name), data);
		CACHE.remove(name);
	}

	public static List<String> listAvailable() {
		Path dir = directory();
		if (!Files.isDirectory(dir)) {
			return List.of();
		}
		try (Stream<Path> files = Files.list(dir)) {
			return files
				.filter(Files::isRegularFile)
				.map(p -> p.getFileName().toString())
				.filter(MusicStore::isValidName)
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.toList();
		} catch (IOException e) {
			BcyaMod.LOGGER.warn("Failed to list music in {}", dir, e);
			return List.of();
		}
	}

	@Nullable
	public static Entry load(String name) {
		if (!isValidName(name)) {
			return null;
		}
		Path file = directory().resolve(name);
		try {
			if (!Files.isRegularFile(file)) {
				CACHE.remove(name);
				return null;
			}
			BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
			long modified = attributes.lastModifiedTime().toMillis();
			long size = attributes.size();
			if (size > MAX_BYTES) {
				BcyaMod.LOGGER.warn("Music file {} is larger than {} bytes and will be ignored", file, MAX_BYTES);
				return null;
			}
			Entry cached = CACHE.get(name);
			if (cached != null && cached.modified() == modified && cached.size() == size) {
				return cached;
			}
			byte[] data = Files.readAllBytes(file);
			CRC32 crc = new CRC32();
			crc.update(data);
			Entry entry = new Entry(data, (int) crc.getValue(), modified, size);
			CACHE.put(name, entry);
			return entry;
		} catch (IOException e) {
			BcyaMod.LOGGER.warn("Failed to read music {}", file, e);
			return null;
		}
	}

	public static void sendTo(ServerPlayer player, String name) {
		Entry entry = load(name);
		if (entry == null) {
			return;
		}
		byte[] data = entry.data();
		int total = Math.max(1, (data.length + CHUNK_BYTES - 1) / CHUNK_BYTES);
		Deque<MusicDataPayload> queue = QUEUES.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
		for (int i = 0; i < total; i++) {
			int from = i * CHUNK_BYTES;
			int to = Math.min(data.length, from + CHUNK_BYTES);
			queue.add(new MusicDataPayload(name, entry.version(), i, total, Arrays.copyOfRange(data, from, to)));
		}
	}

	public static void dropQueue(UUID player) {
		QUEUES.remove(player);
	}

	public static void tick(MinecraftServer server) {
		if (QUEUES.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<UUID, Deque<MusicDataPayload>>> it = QUEUES.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Deque<MusicDataPayload>> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			MusicDataPayload next = entry.getValue().poll();
			if (player != null && next != null) {
				ServerPlayNetworking.send(player, next);
			}
			if (player == null || entry.getValue().isEmpty()) {
				it.remove();
			}
		}
	}

	public static void ensureDirectory() {
		try {
			Files.createDirectories(directory());
		} catch (IOException e) {
			BcyaMod.LOGGER.warn("Failed to create music directory {}", directory(), e);
		}
	}
}
