package cn.erindax.bcya.mask;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import org.jetbrains.annotations.Nullable;

public class MaskOriginState extends SavedData {

	public record Origin(String name, ResourceLocation mask) {
	}

	private static final String DATA_NAME = "bcya_mask_origins";
	private static final String TAG_ORIGINS = "Origins";
	private static final String TAG_ID = "Id";
	private static final String TAG_NAME = "Name";
	private static final String TAG_MASK = "Mask";

	private static final SavedData.Factory<MaskOriginState> FACTORY =
		new SavedData.Factory<>(MaskOriginState::new, MaskOriginState::load, null);

	private final Map<UUID, Origin> origins = new HashMap<>();

	public static MaskOriginState get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	private static MaskOriginState load(CompoundTag tag, HolderLookup.Provider registries) {
		MaskOriginState state = new MaskOriginState();
		ListTag list = tag.getList(TAG_ORIGINS, Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			ResourceLocation mask = ResourceLocation.tryParse(entry.getString(TAG_MASK));
			if (entry.hasUUID(TAG_ID) && mask != null) {
				state.origins.put(entry.getUUID(TAG_ID), new Origin(entry.getString(TAG_NAME), mask));
			}
		}
		return state;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (Map.Entry<UUID, Origin> entry : origins.entrySet()) {
			CompoundTag item = new CompoundTag();
			item.putUUID(TAG_ID, entry.getKey());
			item.putString(TAG_NAME, entry.getValue().name());
			item.putString(TAG_MASK, entry.getValue().mask().toString());
			list.add(item);
		}
		tag.put(TAG_ORIGINS, list);
		return tag;
	}

	@Nullable
	public Origin get(UUID player) {
		return origins.get(player);
	}

	public void set(UUID player, String name, ResourceLocation mask) {
		origins.put(player, new Origin(name, mask));
		setDirty();
	}

	public boolean remove(UUID player) {
		if (origins.remove(player) == null) {
			return false;
		}
		setDirty();
		return true;
	}

	public Map<UUID, Origin> all() {
		return Collections.unmodifiableMap(origins);
	}
}
