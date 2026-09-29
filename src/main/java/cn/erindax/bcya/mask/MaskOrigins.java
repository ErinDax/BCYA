package cn.erindax.bcya.mask;

import cn.erindax.bcya.item.MaskItem;
import cn.erindax.bcya.item.ModItems;
import cn.erindax.bcya.mask.slot.MaskSlots;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

public final class MaskOrigins {

	public enum Result {
		RESTORED, NO_RECORD, ALREADY, NOT_WEARING, NOT_FOUND
	}

	public record Outcome(Result result, @Nullable MaskItem mask) {
	}

	private static final int WORN = -1;

	private record SlotRef(ServerPlayer player, int index) {

		ItemStack get() {
			return index == WORN ? MaskSlots.get(player) : player.getInventory().getItem(index);
		}

		void set(ItemStack stack) {
			if (index == WORN) {
				MaskSlots.set(player, stack);
			} else {
				player.getInventory().setItem(index, stack);
			}
		}
	}

	private MaskOrigins() {
	}

	@Nullable
	public static MaskItem resolve(ResourceLocation id) {
		for (MaskItem mask : ModItems.MASKS) {
			if (BuiltInRegistries.ITEM.getKey(mask).equals(id)) {
				return mask;
			}
		}
		return null;
	}

	public static Outcome restore(ServerPlayer player) {
		MaskOriginState.Origin origin = MaskOriginState.get(player.server).get(player.getUUID());
		MaskItem mask = origin == null ? null : resolve(origin.mask());
		if (mask == null) {
			return new Outcome(Result.NO_RECORD, null);
		}
		ItemStack worn = MaskSlots.get(player);
		if (worn.is(mask)) {
			return new Outcome(Result.ALREADY, mask);
		}
		SlotRef found = find(player, mask);
		if (found == null) {
			return new Outcome(Result.NOT_FOUND, mask);
		}
		boolean self = found.player() == player;
		if (!self && !MaskSlots.accepts(worn)) {
			return new Outcome(Result.NOT_WEARING, mask);
		}
		ItemStack target = found.get();
		found.set(worn);
		MaskSlots.set(player, target);
		MaskSlots.playEquipSound(player);
		if (!self) {
			found.player().displayClientMessage(Component.translatable("item.bcya.agate.taken"), true);
		}
		return new Outcome(Result.RESTORED, mask);
	}

	@Nullable
	private static SlotRef find(ServerPlayer player, MaskItem mask) {
		SlotRef own = findInInventory(player, mask);
		if (own != null) {
			return own;
		}
		for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
			if (other == player) {
				continue;
			}
			if (MaskSlots.get(other).is(mask)) {
				return new SlotRef(other, WORN);
			}
			SlotRef held = findInInventory(other, mask);
			if (held != null) {
				return held;
			}
		}
		return null;
	}

	@Nullable
	private static SlotRef findInInventory(ServerPlayer player, MaskItem mask) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).is(mask)) {
				return new SlotRef(player, i);
			}
		}
		return null;
	}
}
