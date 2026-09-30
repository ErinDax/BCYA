package cn.erindax.bcya.mask;

import cn.erindax.bcya.mask.net.MaskSkinSyncPayload;
import cn.erindax.bcya.voice.MaskVoiceSyncPayload;
import cn.erindax.bcya.voice.VoicePreset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import org.jetbrains.annotations.Nullable;

public class MaskRulesState extends SavedData {

	public enum Rule {
		VOICE_DISABLED("VoiceDisabledMasks"),
		SWAP_BLACKLIST("SwapBlacklistMasks");

		private final String tagName;

		Rule(String tagName) {
			this.tagName = tagName;
		}
	}

	public static final int MAX_PRESETS = 24;

	private static final String DATA_NAME = "bcya_mask_rules";
	private static final String TAG_SKINS = "MaskSkins";
	private static final String TAG_MASK = "Mask";
	private static final String TAG_SKIN = "Skin";
	private static final String TAG_SLIM = "Slim";
	private static final String TAG_PRESETS = "VoicePresets";
	private static final String TAG_PRESETS_READY = "VoicePresetsReady";
	private static final String TAG_ASSIGNMENTS = "VoiceAssignments";
	private static final String TAG_PRESET = "Preset";

	private static final SavedData.Factory<MaskRulesState> FACTORY =
		new SavedData.Factory<>(MaskRulesState::new, MaskRulesState::load, null);

	private final Set<ResourceLocation> voiceDisabled = new HashSet<>();
	private final Set<ResourceLocation> swapBlacklist = new HashSet<>();
	private final Map<ResourceLocation, MaskSkinOverride> skinOverrides = new LinkedHashMap<>();
	private final List<VoicePreset> voicePresets = new ArrayList<>();
	private final Map<ResourceLocation, String> voiceAssignments = new LinkedHashMap<>();
	private boolean voicePresetsReady;

	public static MaskRulesState get(MinecraftServer server) {
		MaskRulesState state = server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
		if (!state.voicePresetsReady) {
			state.voicePresetsReady = true;
			state.voicePresets.add(VoicePreset.squeaky());
			state.setDirty();
		}
		return state;
	}

	private static MaskRulesState load(CompoundTag tag, HolderLookup.Provider registries) {
		MaskRulesState state = new MaskRulesState();
		for (Rule rule : Rule.values()) {
			ListTag list = tag.getList(rule.tagName, Tag.TAG_STRING);
			Set<ResourceLocation> target = state.setFor(rule);
			for (int i = 0; i < list.size(); i++) {
				ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
				if (id != null) {
					target.add(id);
				}
			}
		}
		ListTag skins = tag.getList(TAG_SKINS, Tag.TAG_COMPOUND);
		for (int i = 0; i < skins.size(); i++) {
			CompoundTag entry = skins.getCompound(i);
			ResourceLocation mask = ResourceLocation.tryParse(entry.getString(TAG_MASK));
			String skin = entry.getString(TAG_SKIN);
			if (mask != null) {
				state.skinOverrides.put(mask, new MaskSkinOverride(mask, skin, entry.getBoolean(TAG_SLIM)));
			}
		}
		ListTag presets = tag.getList(TAG_PRESETS, Tag.TAG_COMPOUND);
		for (int i = 0; i < presets.size() && state.voicePresets.size() < MAX_PRESETS; i++) {
			VoicePreset preset = VoicePreset.load(presets.getCompound(i));
			if (state.findPreset(preset.id()) == null) {
				state.voicePresets.add(preset);
			}
		}
		state.voicePresetsReady = tag.getBoolean(TAG_PRESETS_READY);
		ListTag assignments = tag.getList(TAG_ASSIGNMENTS, Tag.TAG_COMPOUND);
		for (int i = 0; i < assignments.size(); i++) {
			CompoundTag entry = assignments.getCompound(i);
			ResourceLocation mask = ResourceLocation.tryParse(entry.getString(TAG_MASK));
			String preset = entry.getString(TAG_PRESET);
			if (mask != null && state.findPreset(preset) != null) {
				state.voiceAssignments.put(mask, preset);
			}
		}
		return state;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		for (Rule rule : Rule.values()) {
			ListTag list = new ListTag();
			for (ResourceLocation id : new TreeSet<>(setFor(rule))) {
				list.add(StringTag.valueOf(id.toString()));
			}
			tag.put(rule.tagName, list);
		}
		ListTag skins = new ListTag();
		for (MaskSkinOverride override : skinOverrides.values()) {
			CompoundTag entry = new CompoundTag();
			entry.putString(TAG_MASK, override.mask().toString());
			entry.putString(TAG_SKIN, override.skin());
			entry.putBoolean(TAG_SLIM, override.slim());
			skins.add(entry);
		}
		tag.put(TAG_SKINS, skins);
		ListTag presets = new ListTag();
		for (VoicePreset preset : voicePresets) {
			presets.add(preset.save());
		}
		tag.put(TAG_PRESETS, presets);
		tag.putBoolean(TAG_PRESETS_READY, voicePresetsReady);
		ListTag assignments = new ListTag();
		for (Map.Entry<ResourceLocation, String> entry : voiceAssignments.entrySet()) {
			CompoundTag item = new CompoundTag();
			item.putString(TAG_MASK, entry.getKey().toString());
			item.putString(TAG_PRESET, entry.getValue());
			assignments.add(item);
		}
		tag.put(TAG_ASSIGNMENTS, assignments);
		return tag;
	}

	public List<VoicePreset> getVoicePresets() {
		return List.copyOf(voicePresets);
	}

	public void setVoicePresets(List<VoicePreset> presets) {
		voicePresets.clear();
		for (VoicePreset preset : presets) {
			if (voicePresets.size() < MAX_PRESETS && findPreset(preset.id()) == null) {
				voicePresets.add(preset);
			}
		}
		voiceAssignments.values().removeIf(id -> findPreset(id) == null);
		setDirty();
	}

	public Map<ResourceLocation, String> getVoiceAssignments() {
		return Map.copyOf(voiceAssignments);
	}

	public void setVoiceAssignments(Map<ResourceLocation, String> assignments) {
		voiceAssignments.clear();
		for (Map.Entry<ResourceLocation, String> entry : assignments.entrySet()) {
			if (findPreset(entry.getValue()) != null) {
				voiceAssignments.put(entry.getKey(), entry.getValue());
			}
		}
		setDirty();
	}

	@Nullable
	private VoicePreset findPreset(String id) {
		for (VoicePreset preset : voicePresets) {
			if (preset.id().equals(id)) {
				return preset;
			}
		}
		return null;
	}

	public boolean has(Rule rule, ResourceLocation maskId) {
		return setFor(rule).contains(maskId);
	}

	public boolean toggle(Rule rule, ResourceLocation maskId) {
		Set<ResourceLocation> set = setFor(rule);
		boolean nowActive;
		if (set.remove(maskId)) {
			nowActive = false;
		} else {
			set.add(maskId);
			nowActive = true;
		}
		setDirty();
		return nowActive;
	}

	public Set<ResourceLocation> getMasks(Rule rule) {
		return Collections.unmodifiableSet(setFor(rule));
	}

	public void set(Rule rule, Collection<ResourceLocation> masks) {
		Set<ResourceLocation> set = setFor(rule);
		set.clear();
		set.addAll(masks);
		setDirty();
	}

	@Nullable
	public MaskSkinOverride getSkinOverride(ResourceLocation maskId) {
		return skinOverrides.get(maskId);
	}

	public List<MaskSkinOverride> getSkinOverrides() {
		return new ArrayList<>(skinOverrides.values());
	}

	public void setSkinOverrides(List<MaskSkinOverride> overrides) {
		skinOverrides.clear();
		for (MaskSkinOverride override : overrides) {
			skinOverrides.put(override.mask(), override);
		}
		setDirty();
	}

	public MaskVoiceSyncPayload toVoicePayload() {
		return new MaskVoiceSyncPayload(new TreeSet<>(voiceDisabled).stream().toList(), getVoicePresets(),
			getVoiceAssignments());
	}

	public MaskSkinSyncPayload toSkinPayload() {
		return new MaskSkinSyncPayload(getSkinOverrides());
	}

	private Set<ResourceLocation> setFor(Rule rule) {
		return switch (rule) {
			case VOICE_DISABLED -> voiceDisabled;
			case SWAP_BLACKLIST -> swapBlacklist;
		};
	}
}
