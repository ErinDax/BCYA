package cn.erindax.bcya.mask.net;

import cn.erindax.bcya.item.MaskItem;
import cn.erindax.bcya.item.ModItems;
import cn.erindax.bcya.manage.WandWhitelist;
import cn.erindax.bcya.mask.MaskRulesState;
import cn.erindax.bcya.voice.MaskVoiceSyncPayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class MaskRulesHandler {

	private MaskRulesHandler() {
	}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(MaskRulesPayload.TYPE, MaskRulesPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(MaskRulesPayload.TYPE, MaskRulesPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(MaskRulesPayload.TYPE,
			(payload, context) -> update(context.player(), payload));
	}

	public static void openEditor(ServerPlayer player) {
		if (!WandWhitelist.canManage(player)) {
			return;
		}
		MaskRulesState state = MaskRulesState.get(player.server);
		ServerPlayNetworking.send(player, new MaskRulesPayload(
			List.copyOf(state.getMasks(MaskRulesState.Rule.VOICE_DISABLED)),
			List.copyOf(state.getMasks(MaskRulesState.Rule.SWAP_BLACKLIST))));
	}

	public static void syncVoice(MinecraftServer server) {
		MaskVoiceSyncPayload payload = MaskRulesState.get(server).toVoicePayload();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	private static void update(ServerPlayer player, MaskRulesPayload payload) {
		if (!WandWhitelist.canManage(player)) {
			return;
		}
		MinecraftServer server = player.server;
		MaskRulesState state = MaskRulesState.get(server);
		state.set(MaskRulesState.Rule.VOICE_DISABLED, masksOnly(payload.voiceDisabled()));
		state.set(MaskRulesState.Rule.SWAP_BLACKLIST, masksOnly(payload.swapBlacklist()));
		syncVoice(server);
		player.displayClientMessage(Component.translatable("screen.bcya.mask_rules.saved"), true);
	}

	private static List<ResourceLocation> masksOnly(List<ResourceLocation> ids) {
		List<ResourceLocation> result = new ArrayList<>();
		for (MaskItem mask : ModItems.MASKS) {
			ResourceLocation id = BuiltInRegistries.ITEM.getKey(mask);
			if (ids.contains(id)) {
				result.add(id);
			}
		}
		return result;
	}
}
