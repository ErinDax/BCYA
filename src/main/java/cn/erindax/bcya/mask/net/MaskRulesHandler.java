package cn.erindax.bcya.mask.net;

import cn.erindax.bcya.item.MaskItem;
import cn.erindax.bcya.item.ModItems;
import cn.erindax.bcya.manage.WandWhitelist;
import cn.erindax.bcya.mask.MaskRulesState;
import cn.erindax.bcya.voice.MaskVoiceSyncPayload;
import cn.erindax.bcya.voice.VoicePreset;
import cn.erindax.bcya.voice.VoicePresetsPayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class MaskRulesHandler {

	private static final String FALLBACK_NAME = "方案";

	private MaskRulesHandler() {
	}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(MaskRulesPayload.TYPE, MaskRulesPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(MaskRulesPayload.TYPE, MaskRulesPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(VoicePresetsPayload.TYPE, VoicePresetsPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(VoicePresetsPayload.TYPE, VoicePresetsPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(MaskRulesPayload.TYPE,
			(payload, context) -> update(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(VoicePresetsPayload.TYPE,
			(payload, context) -> updatePresets(context.player(), payload));
	}

	public static void openEditor(ServerPlayer player) {
		if (!WandWhitelist.canManage(player)) {
			return;
		}
		MaskRulesState state = MaskRulesState.get(player.server);
		ServerPlayNetworking.send(player, new MaskRulesPayload(
			List.copyOf(state.getMasks(MaskRulesState.Rule.VOICE_DISABLED)),
			List.copyOf(state.getMasks(MaskRulesState.Rule.SWAP_BLACKLIST)),
			state.getVoicePresets(), state.getVoiceAssignments()));
	}

	public static void openPresets(ServerPlayer player) {
		if (!WandWhitelist.canManage(player)) {
			return;
		}
		ServerPlayNetworking.send(player, new VoicePresetsPayload(MaskRulesState.get(player.server).getVoicePresets()));
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
		Map<ResourceLocation, String> voice = new HashMap<>();
		for (ResourceLocation mask : masksOnly(List.copyOf(payload.voice().keySet()))) {
			voice.put(mask, payload.voice().get(mask));
		}
		state.setVoiceAssignments(voice);
		syncVoice(server);
		player.displayClientMessage(Component.translatable("screen.bcya.mask_rules.saved"), true);
	}

	private static void updatePresets(ServerPlayer player, VoicePresetsPayload payload) {
		if (!WandWhitelist.canManage(player)) {
			return;
		}
		List<VoicePreset> presets = new ArrayList<>();
		Set<String> ids = new HashSet<>();
		for (VoicePreset preset : payload.presets()) {
			if (presets.size() >= MaskRulesState.MAX_PRESETS) {
				break;
			}
			String id = VoicePreset.isValidId(preset.id()) && !ids.contains(preset.id()) ? preset.id() : VoicePreset.newId();
			ids.add(id);
			presets.add(new VoicePreset(id, preset.name(), preset.pitch(), preset.robot(), preset.radio(), preset.grit(),
				preset.echo(), preset.volume()).sanitized(FALLBACK_NAME + (presets.size() + 1)));
		}
		MinecraftServer server = player.server;
		MaskRulesState.get(server).setVoicePresets(presets);
		syncVoice(server);
		player.displayClientMessage(Component.translatable("screen.bcya.voice.saved"), true);
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
