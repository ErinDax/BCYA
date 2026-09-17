package cn.erindax.bcya.mask.slot.net;

import cn.erindax.bcya.mask.slot.MaskSlotActions;
import cn.erindax.bcya.mask.slot.MaskSlots;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

public final class MaskSlotHandler {

	private MaskSlotHandler() {
	}

	public static void init() {
		PayloadTypeRegistry.playC2S().register(MaskSlotClickPayload.TYPE, MaskSlotClickPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(MaskSlotCreativePayload.TYPE, MaskSlotCreativePayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(MaskSlotClickPayload.TYPE,
			(payload, context) -> click(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(MaskSlotCreativePayload.TYPE,
			(payload, context) -> creativeSet(context.player(), payload));
	}

	private static void click(ServerPlayer player, MaskSlotClickPayload payload) {
		InventoryMenu menu = player.inventoryMenu;
		if (player.containerMenu != menu || player.isSpectator()) {
			return;
		}
		boolean changed = payload.quickMove() ? MaskSlotActions.quickMove(player) : MaskSlotActions.click(player, menu);
		if (changed) {
			menu.broadcastChanges();
		}
	}

	private static void creativeSet(ServerPlayer player, MaskSlotCreativePayload payload) {
		if (!player.isCreative()) {
			return;
		}
		ItemStack stack = payload.stack();
		if (!stack.isEmpty() && (!MaskSlots.accepts(stack) || stack.getCount() != 1
				|| !stack.isItemEnabled(player.level().enabledFeatures()))) {
			return;
		}
		if (ItemStack.matches(MaskSlots.get(player), stack)) {
			return;
		}
		MaskSlots.set(player, stack);
		if (!stack.isEmpty()) {
			MaskSlots.playEquipSound(player);
		}
	}
}
