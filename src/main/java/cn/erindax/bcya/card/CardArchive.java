package cn.erindax.bcya.card;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class CardArchive extends SavedData {

	private static final String DATA_NAME = "bcya_investigator_archive";
	private static final String TAG_CARDS = "Cards";

	private static final SavedData.Factory<CardArchive> FACTORY =
		new SavedData.Factory<>(CardArchive::new, CardArchive::load, DataFixTypes.LEVEL);

	private final Map<String, CompoundTag> cards = new LinkedHashMap<>();

	public static void prepare() {
		FACTORY.getClass();
	}

	public static CardArchive get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	private static CardArchive load(CompoundTag tag, HolderLookup.Provider registries) {
		CardArchive archive = new CardArchive();
		ListTag list = tag.getList(TAG_CARDS, Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag card = list.getCompound(i);
			String id = card.getString(CardData.CARD_ID);
			if (!id.isEmpty()) {
				archive.cards.put(id, card.copy());
			}
		}
		return archive;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (CompoundTag card : cards.values()) {
			list.add(card.copy());
		}
		tag.put(TAG_CARDS, list);
		return tag;
	}

	public void put(CompoundTag card) {
		String id = card.getString(CardData.CARD_ID);
		if (id.isEmpty()) {
			return;
		}
		cards.put(id, card.copy());
		setDirty();
	}

	public Collection<CompoundTag> all() {
		return cards.values();
	}
}
