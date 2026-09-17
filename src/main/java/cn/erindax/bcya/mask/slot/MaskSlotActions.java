package cn.erindax.bcya.mask.slot;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class MaskSlotActions {

	private MaskSlotActions() {
	}

	public static boolean click(Player player, AbstractContainerMenu menu) {
		ItemStack carried = menu.getCarried();
		ItemStack worn = MaskSlots.get(player);
		if (carried.isEmpty()) {
			if (worn.isEmpty()) {
				return false;
			}
			menu.setCarried(worn);
			MaskSlots.set(player, ItemStack.EMPTY);
			return true;
		}
		if (!MaskSlots.accepts(carried)) {
			return false;
		}
		if (worn.isEmpty()) {
			MaskSlots.set(player, carried.split(1));
			menu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
			MaskSlots.onEquipped(player);
			return true;
		}
		if (carried.getCount() != 1) {
			return false;
		}
		MaskSlots.set(player, carried);
		menu.setCarried(worn);
		MaskSlots.onEquipped(player);
		return true;
	}

	public static boolean quickMove(Player player) {
		ItemStack worn = MaskSlots.get(player);
		if (worn.isEmpty() || !player.getInventory().add(worn.copy())) {
			return false;
		}
		MaskSlots.set(player, ItemStack.EMPTY);
		return true;
	}
}
