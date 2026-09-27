package cn.erindax.bcya.skywalk;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;

public class SkywalkState extends SavedData {

	private static final String DATA_NAME = "bcya_skywalk";
	private static final String ENABLED = "enabled";
	private static final String DEPTH = "depth";
	private static final SavedData.Factory<SkywalkState> FACTORY =
		new SavedData.Factory<>(SkywalkState::new, SkywalkState::load, null);

	private boolean enabled;
	private int depth = Skywalk.DEFAULT_DEPTH;

	public static SkywalkState get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	private static SkywalkState load(CompoundTag tag, HolderLookup.Provider registries) {
		SkywalkState state = new SkywalkState();
		state.enabled = tag.getBoolean(ENABLED);
		if (tag.contains(DEPTH, Tag.TAG_INT)) {
			state.depth = Mth.clamp(tag.getInt(DEPTH), Skywalk.MIN_DEPTH, Skywalk.MAX_DEPTH);
		}
		return state;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		tag.putBoolean(ENABLED, enabled);
		tag.putInt(DEPTH, depth);
		return tag;
	}

	public boolean enabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		setDirty();
	}

	public int depth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = Mth.clamp(depth, Skywalk.MIN_DEPTH, Skywalk.MAX_DEPTH);
		setDirty();
	}
}
