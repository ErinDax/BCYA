package cn.erindax.bcya.item;

import cn.erindax.bcya.BcyaMod;
import cn.erindax.bcya.block.ModBlocks;
import cn.erindax.bcya.lock.LockType;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import java.util.List;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public final class ModItems {

	public static final MaskItem CAT_MASK = mask("cat_mask", 0x00b040, true);
	public static final MaskItem FOX_MASK = mask("fox_mask", 0xe96c2f, true);
	public static final MaskItem GOAT_MASK = mask("goat_mask", 0x4bf4d7, false);
	public static final MaskItem RABBIT_MASK = mask("rabbit_mask", 0x89add7, true);
	public static final MaskItem DOG_MASK = mask("dog_mask", 0xe8a662, true);
	public static final MaskItem WOLF_MASK = mask("wolf_mask", 0xff5959, true);
	public static final MaskItem BEAR_MASK = mask("bear_mask", 0xcf6ce7, true);
	public static final MaskItem LOGIC_CAT_MASK = mask("logic_cat_mask", 0x4a3730, true);
	public static final MaskItem CALICO_CAT_MASK = mask("calico_cat_mask", 0x5a5351, true);
	public static final MaskItem BLACK_CAT_COLLAR = mask("black_cat_collar", 0xbd2828, true, false);

	public static final List<MaskItem> MASKS = List.of(
		CAT_MASK, FOX_MASK, GOAT_MASK, RABBIT_MASK, DOG_MASK, WOLF_MASK, BEAR_MASK, LOGIC_CAT_MASK, CALICO_CAT_MASK,
		BLACK_CAT_COLLAR);

	public static final Item MAGIC_WAND = register("magic_wand",
		new MagicWandItem(new Item.Properties()
			.stacksTo(1)
			.rarity(Rarity.EPIC)));

	public static final Item BARRIER_PANEL = register("barrier_panel",
		new BlockItem(ModBlocks.BARRIER_PANEL, new Item.Properties()
			.rarity(Rarity.EPIC)));

	public static final Item CAT_EYE = register("cat_eye",
		new CatEyeItem(new Item.Properties()
			.stacksTo(1)
			.rarity(Rarity.RARE)));

	public static final Item KEY = register("key",
		new KeyItem(new Item.Properties()
			.stacksTo(16)));

	public static final Item PASSWORD_LOCK = register("password_lock",
		new LockItem(LockType.PASSWORD, new Item.Properties()
			.stacksTo(16)));

	public static final Item KEY_LOCK = register("key_lock",
		new LockItem(LockType.KEY, new Item.Properties()
			.stacksTo(16)));

	public static final Item MUSIC_NOTE = register("music_note",
		new MusicNoteItem(new Item.Properties()
			.stacksTo(1)
			.rarity(Rarity.RARE)));

	public static final Item ID_CARD = register("id_card",
		new IdCardItem(new Item.Properties()
			.stacksTo(1)));

	public static final CreativeModeTab BCYA_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
		BcyaMod.id("bcya"),
		FabricItemGroup.builder()
			.title(Component.translatable("itemGroup." + BcyaMod.MOD_ID + ".bcya"))
			.icon(() -> new ItemStack(CAT_MASK))
			.displayItems((parameters, output) -> {
				MASKS.forEach(output::accept);
				output.accept(MAGIC_WAND);
				output.accept(CAT_EYE);
				output.accept(KEY);
				output.accept(PASSWORD_LOCK);
				output.accept(KEY_LOCK);
				output.accept(MUSIC_NOTE);
				output.accept(ID_CARD);
				output.accept(BARRIER_PANEL);
			})
			.build());

	private ModItems() {
	}

	private static MaskItem mask(String name, int nameColor, boolean slimModel) {
		return mask(name, nameColor, slimModel, true);
	}

	private static MaskItem mask(String name, int nameColor, boolean slimModel, boolean renderOnHead) {
		ResourceLocation skin = BcyaMod.id("textures/entity/" + name + "_skin.png");
		return register(name, new MaskItem(nameColor, skin, slimModel, renderOnHead,
			new Item.Properties()
				.stacksTo(1)
				.rarity(Rarity.RARE)));
	}

	private static <T extends Item> T register(String name, T item) {
		return Registry.register(BuiltInRegistries.ITEM, BcyaMod.id(name), item);
	}

	public static void init() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.OP_BLOCKS).register(entries -> entries.accept(BARRIER_PANEL));
	}
}
