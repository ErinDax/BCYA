package cn.erindax.bcya.mixin;

import cn.erindax.bcya.mask.slot.MaskSlots;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {

	@Unique
	private static final int BCYA_INVENTORY_START = 9;
	@Unique
	private static final int BCYA_INVENTORY_END = 46;

	private InventoryMenuMixin() {
		super(null, 0);
	}

	@Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
	private void bcya$quickEquipMask(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
		if (index < BCYA_INVENTORY_START || index >= BCYA_INVENTORY_END || index >= this.slots.size()) {
			return;
		}
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return;
		}
		ItemStack stack = slot.getItem();
		if (!MaskSlots.accepts(stack) || !MaskSlots.get(player).isEmpty()) {
			return;
		}
		ItemStack original = stack.copy();
		MaskSlots.set(player, stack.split(1));
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		MaskSlots.onEquipped(player);
		MaskSlots.notifyClientChanged(player);
		cir.setReturnValue(original);
	}
}
