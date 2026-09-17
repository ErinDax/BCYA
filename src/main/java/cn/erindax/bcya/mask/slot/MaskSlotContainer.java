package cn.erindax.bcya.mask.slot;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MaskSlotContainer implements Container {

	private final Player player;

	public MaskSlotContainer(Player player) {
		this.player = player;
	}

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return MaskSlots.get(player).isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		return slot == 0 ? MaskSlots.get(player) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack current = getItem(slot);
		if (current.isEmpty() || amount <= 0) {
			return ItemStack.EMPTY;
		}
		ItemStack removed = current.split(amount);
		MaskSlots.set(player, current);
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack current = getItem(slot);
		MaskSlots.set(player, ItemStack.EMPTY);
		return current;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (slot == 0) {
			MaskSlots.set(player, stack);
		}
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public void setChanged() {
	}

	@Override
	public boolean stillValid(Player viewer) {
		return true;
	}

	@Override
	public void clearContent() {
		MaskSlots.set(player, ItemStack.EMPTY);
	}
}
