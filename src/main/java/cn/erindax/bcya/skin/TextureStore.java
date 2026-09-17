package cn.erindax.bcya.skin;

import cn.erindax.bcya.BcyaMod;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import net.minecraft.server.level.ServerPlayer;

import org.jetbrains.annotations.Nullable;

public final class TextureStore {

	public static final TextureStore SKINS = new TextureStore("skins");
	public static final TextureStore KEYS = new TextureStore("keys");

	public static final int MAX_BYTES = 512 * 1024;
	private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_\\-]{1,32}");
	private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

	private final String kind;
	private final Map<String, byte[]> cache = new ConcurrentHashMap<>();

	private TextureStore(String kind) {
		this.kind = kind;
	}

	public String kind() {
		return kind;
	}

	public Path directory() {
		return FabricLoader.getInstance().getConfigDir().resolve("bcya").resolve(kind);
	}

	public static boolean isValidName(String name) {
		return NAME.matcher(name).matches();
	}

	@Nullable
	public byte[] load(String name, boolean refresh) {
		if (!isValidName(name)) {
			return null;
		}
		if (!refresh) {
			byte[] cached = cache.get(name);
			if (cached != null) {
				return cached;
			}
		}
		Path file = directory().resolve(name + ".png");
		try {
			if (!Files.isRegularFile(file) || Files.size(file) > MAX_BYTES) {
				return null;
			}
			byte[] data = Files.readAllBytes(file);
			if (data.length < PNG_MAGIC.length
				|| !Arrays.equals(Arrays.copyOf(data, PNG_MAGIC.length), PNG_MAGIC)) {
				return null;
			}
			cache.put(name, data);
			return data;
		} catch (IOException e) {
			BcyaMod.LOGGER.warn("Failed to read texture {}", file, e);
			return null;
		}
	}

	public void sendTo(ServerPlayer player, String name) {
		byte[] data = load(name, false);
		if (data != null) {
			ServerPlayNetworking.send(player, new TexturePayload(kind, name, data));
		}
	}

	public void sendToAll(Iterable<ServerPlayer> players, String name) {
		byte[] data = load(name, false);
		if (data == null) {
			return;
		}
		TexturePayload payload = new TexturePayload(kind, name, data);
		for (ServerPlayer player : players) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	public void sendAllTo(ServerPlayer player) {
		for (String name : listAvailable()) {
			sendTo(player, name);
		}
	}

	public List<String> listAvailable() {
		Path dir = directory();
		if (!Files.isDirectory(dir)) {
			return List.of();
		}
		try (Stream<Path> files = Files.list(dir)) {
			return files
				.map(p -> p.getFileName().toString())
				.filter(n -> n.endsWith(".png"))
				.map(n -> n.substring(0, n.length() - 4))
				.filter(TextureStore::isValidName)
				.sorted()
				.toList();
		} catch (IOException e) {
			BcyaMod.LOGGER.warn("Failed to list textures in {}", dir, e);
			return List.of();
		}
	}

	public void ensureDirectory() {
		try {
			Files.createDirectories(directory());
		} catch (IOException e) {
			BcyaMod.LOGGER.warn("Failed to create texture directory {}", directory(), e);
		}
	}
}
