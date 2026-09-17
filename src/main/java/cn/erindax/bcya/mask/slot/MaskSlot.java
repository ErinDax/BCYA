package cn.erindax.bcya.mask.slot;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MaskSlot extends Slot {

	public MaskSlot(Player player, int x, int y) {
		super(new MaskSlotContainer(player), 0, x, y);
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return MaskSlots.accepts(stack);
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}
}
