package cn.erindax.bcya.voice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

public final class MaskVoiceClientState {

	private static volatile Set<ResourceLocation> disabledMasks = Set.of();
	private static volatile Map<String, VoicePreset> presets = Map.of();
	private static volatile Map<ResourceLocation, String> assignments = Map.of();
	private static volatile Map<UUID, ResourceLocation> wearers = Map.of();
	@Nullable
	private static volatile VoicePreset fallback;
	private static volatile boolean hearOriginal;

	private MaskVoiceClientState() {
	}

	public static void apply(MaskVoiceSyncPayload payload) {
		List<VoicePreset> list = payload.presets();
		Map<String, VoicePreset> byId = new HashMap<>();
		for (VoicePreset preset : list) {
			byId.put(preset.id(), preset);
		}
		disabledMasks = Set.copyOf(payload.disabledMasks());
		presets = Map.copyOf(byId);
		assignments = Map.copyOf(payload.assignments());
		fallback = list.isEmpty() ? null : list.get(0);
	}

	public static void setWearers(Map<UUID, ResourceLocation> map) {
		wearers = Map.copyOf(map);
	}

	public static void setHearOriginal(boolean value) {
		hearOriginal = value;
	}

	public static void reset() {
		disabledMasks = Set.of();
		presets = Map.of();
		assignments = Map.of();
		wearers = Map.of();
		fallback = null;
		hearOriginal = false;
	}

	@Nullable
	public static VoicePreset presetFor(UUID speaker) {
		if (hearOriginal) {
			return null;
		}
		ResourceLocation mask = wearers.get(speaker);
		if (mask == null || disabledMasks.contains(mask)) {
			return null;
		}
		String id = assignments.get(mask);
		VoicePreset preset = id == null ? null : presets.get(id);
		return preset != null ? preset : fallback;
	}
}
