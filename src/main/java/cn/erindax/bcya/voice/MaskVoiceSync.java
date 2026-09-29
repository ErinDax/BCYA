package cn.erindax.bcya.voice;

import cn.erindax.bcya.item.MaskItem;
import cn.erindax.bcya.util.MaskUtil;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class MaskVoiceSync {

	private static final int INTERVAL_TICKS = 10;

	private static Map<UUID, ResourceLocation> last = Map.of();
	private static int ticks;

	private MaskVoiceSync() {
	}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(MaskWearersPayload.TYPE, MaskWearersPayload.STREAM_CODEC);
		ServerTickEvents.END_SERVER_TICK.register(MaskVoiceSync::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			sender.sendPacket(new MaskWearersPayload(collect(server))));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			last = Map.of();
			ticks = 0;
		});
	}

	private static void tick(MinecraftServer server) {
		if (++ticks % INTERVAL_TICKS != 0) {
			return;
		}
		Map<UUID, ResourceLocation> current = collect(server);
		if (current.equals(last)) {
			return;
		}
		last = current;
		MaskWearersPayload payload = new MaskWearersPayload(current);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	private static Map<UUID, ResourceLocation> collect(MinecraftServer server) {
		Map<UUID, ResourceLocation> wearers = new HashMap<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			MaskItem mask = MaskUtil.getWornMask(player);
			if (mask != null) {
				wearers.put(player.getUUID(), BuiltInRegistries.ITEM.getKey(mask));
			}
		}
		return wearers;
	}
}
