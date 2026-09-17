package cn.erindax.bcya.music;

import cn.erindax.bcya.BcyaMod;
import cn.erindax.bcya.item.ModComponents;
import cn.erindax.bcya.item.ModItems;
import cn.erindax.bcya.item.MusicNoteItem;
import cn.erindax.bcya.manage.WandWhitelist;
import cn.erindax.bcya.music.net.MusicUploadPayload;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.CRC32;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class MusicUploads {

	private static final int MAX_CHUNKS = MusicStore.MAX_BYTES / MusicUploadPayload.CHUNK_BYTES + 1;

	private static final class Assembly {
		private final String name;
		private final int version;
		private final byte[][] parts;
		private int received;
		private long size;

		private Assembly(String name, int version, int total) {
			this.name = name;
			this.version = version;
			this.parts = new byte[total][];
		}
	}

	private static final Map<UUID, Assembly> ASSEMBLIES = new HashMap<>();

	private MusicUploads() {
	}

	public static void drop(UUID player) {
		ASSEMBLIES.remove(player);
	}

	public static void receive(ServerPlayer player, MusicUploadPayload payload) {
		InteractionHand hand = payload.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
		if (!WandWhitelist.canManage(player)) {
			if (payload.index() == 0) {
				finish(player, hand, Component.translatable("item.bcya.music_note.upload_denied"));
			}
			return;
		}
		String name = payload.name().strip();
		if (!MusicStore.isValidName(name) || payload.total() <= 0 || payload.total() > MAX_CHUNKS
				|| payload.index() < 0 || payload.index() >= payload.total()) {
			if (payload.index() == 0) {
				finish(player, hand, Component.translatable("item.bcya.music_note.upload_invalid", name));
			}
			return;
		}
		Assembly assembly = ASSEMBLIES.get(player.getUUID());
		if (assembly == null || !assembly.name.equals(name) || assembly.version != payload.version()
				|| assembly.parts.length != payload.total()) {
			assembly = new Assembly(name, payload.version(), payload.total());
			ASSEMBLIES.put(player.getUUID(), assembly);
		}
		if (assembly.parts[payload.index()] == null) {
			assembly.parts[payload.index()] = payload.data();
			assembly.received++;
			assembly.size += payload.data().length;
		}
		if (assembly.size > MusicStore.MAX_BYTES) {
			ASSEMBLIES.remove(player.getUUID());
			finish(player, hand, Component.translatable("item.bcya.music_note.upload_invalid", name));
			return;
		}
		if (assembly.received < assembly.parts.length) {
			return;
		}
		ASSEMBLIES.remove(player.getUUID());
		byte[] data = concat(assembly.parts, (int) assembly.size);
		CRC32 crc = new CRC32();
		crc.update(data);
		if ((int) crc.getValue() != assembly.version || !MusicStore.looksValid(name, data)) {
			finish(player, hand, Component.translatable("item.bcya.music_note.upload_invalid", name));
			return;
		}
		try {
			MusicStore.save(name, data);
		} catch (IOException e) {
			BcyaMod.LOGGER.warn("Failed to save uploaded music {}", name, e);
			finish(player, hand, Component.translatable("item.bcya.music_note.upload_failed", e.getMessage()));
			return;
		}
		ItemStack held = player.getItemInHand(hand);
		if (held.is(ModItems.MUSIC_NOTE)) {
			held.set(ModComponents.MUSIC_TRACK, name);
		}
		finish(player, hand, Component.translatable("item.bcya.music_note.upload_ok", MusicNoteItem.displayName(name)));
	}

	private static void finish(ServerPlayer player, InteractionHand hand, Component message) {
		player.displayClientMessage(message, true);
		MusicHandler.openMenu(player, hand);
	}

	private static byte[] concat(byte[][] parts, int size) {
		byte[] data = new byte[size];
		int offset = 0;
		for (byte[] part : parts) {
			System.arraycopy(part, 0, data, offset, part.length);
			offset += part.length;
		}
		return data;
	}
}
