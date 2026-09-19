package cn.erindax.bcya.check;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class CheckLog extends SavedData {

	private static final String DATA_NAME = "bcya_check_log";
	private static final String TAG_ENTRIES = "entries";
	private static final int MAX_ENTRIES = 500;

	private static final SavedData.Factory<CheckLog> FACTORY =
		new SavedData.Factory<>(CheckLog::new, CheckLog::load, DataFixTypes.LEVEL);

	private final List<CompoundTag> entries = new ArrayList<>();

	public static void prepare() {
		FACTORY.getClass();
	}

	public static CheckLog get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	private static CheckLog load(CompoundTag tag, HolderLookup.Provider registries) {
		CheckLog log = new CheckLog();
		ListTag list = tag.getList(TAG_ENTRIES, Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			log.entries.add(list.getCompound(i).copy());
		}
		return log;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (CompoundTag entry : entries) {
			list.add(entry.copy());
		}
		tag.put(TAG_ENTRIES, list);
		return tag;
	}

	public void append(CompoundTag entry) {
		entries.add(entry.copy());
		while (entries.size() > MAX_ENTRIES) {
			entries.remove(0);
		}
		setDirty();
	}

	public List<CompoundTag> entries() {
		return Collections.unmodifiableList(entries);
	}
}
