package cn.erindax.bcya.mask;

import cn.erindax.bcya.mask.net.MaskSkinSyncPayload;
import cn.erindax.bcya.voice.MaskVoiceSyncPayload;

import java.util.ArrayList;
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

	private static final String DATA_NAME = "bcya_mask_rules";
	private static final String TAG_SKINS = "MaskSkins";
	private static final String TAG_MASK = "Mask";
	private static final String TAG_SKIN = "Skin";
	private static final String TAG_SLIM = "Slim";

	private static final SavedData.Factory<MaskRulesState> FACTORY =
		new SavedData.Factory<>(MaskRulesState::new, MaskRulesState::load, null);

	private final Set<ResourceLocation> voiceDisabled = new HashSet<>();
	private final Set<ResourceLocation> swapBlacklist = new HashSet<>();
	private final Map<ResourceLocation, MaskSkinOverride> skinOverrides = new LinkedHashMap<>();

	public static MaskRulesState get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
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
			if (mask != null && !skin.isEmpty()) {
				state.skinOverrides.put(mask, new MaskSkinOverride(mask, skin, entry.getBoolean(TAG_SLIM)));
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
		return tag;
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
			if (!override.skin().isEmpty()) {
				skinOverrides.put(override.mask(), override);
			}
		}
		setDirty();
	}

	public MaskVoiceSyncPayload toVoicePayload() {
		return new MaskVoiceSyncPayload(new TreeSet<>(voiceDisabled).stream().toList());
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
