package cn.erindax.bcya.mask;

import cn.erindax.bcya.item.ModItems;
import cn.erindax.bcya.mask.slot.MaskSlots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class MaskSwapper {

	private static final int WORN = -1;

	private record SlotRef(ServerPlayer player, int index) {

		void set(ItemStack stack) {
			if (index == WORN) {
				MaskSlots.set(player, stack);
			} else {
				player.getInventory().setItem(index, stack);
			}
		}
	}

	private MaskSwapper() {
	}

	public static int swapAll(MinecraftServer server, RandomSource random) {
		MaskRulesState rules = MaskRulesState.get(server);
		List<SlotRef> slots = new ArrayList<>();
		List<ItemStack> masks = new ArrayList<>();

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ItemStack worn = MaskSlots.get(player);
			if (isSwappable(worn, rules)) {
				slots.add(new SlotRef(player, WORN));
				masks.add(worn);
			}
			Inventory inventory = player.getInventory();
			for (int i = 0; i < inventory.getContainerSize(); i++) {
				ItemStack stack = inventory.getItem(i);
				if (isSwappable(stack, rules)) {
					slots.add(new SlotRef(player, i));
					masks.add(stack);
				}
			}
		}
		if (masks.size() < 2) {
			return 0;
		}

		for (int i = masks.size() - 1; i > 0; i--) {
			Collections.swap(masks, i, random.nextInt(i));
		}

		for (int i = 0; i < slots.size(); i++) {
			slots.get(i).set(masks.get(i));
		}
		return masks.size();
	}

	private static boolean isSwappable(ItemStack stack, MaskRulesState rules) {
		if (stack.isEmpty()) {
			return false;
		}
		Item item = stack.getItem();
		return ModItems.MASKS.contains(item)
			&& !rules.has(MaskRulesState.Rule.SWAP_BLACKLIST, BuiltInRegistries.ITEM.getKey(item));
	}
}
