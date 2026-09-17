package cn.erindax.bcya.mask.slot;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;

public final class MaskSlotEvents {

	private MaskSlotEvents() {
	}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) {
				dropOnDeath(player);
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> migrateHelmet(handler.player));
	}

	private static void dropOnDeath(ServerPlayer player) {
		if (player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
			return;
		}
		ItemStack mask = MaskSlots.get(player);
		if (mask.isEmpty()) {
			return;
		}
		player.drop(mask.copy(), true, false);
		MaskSlots.set(player, ItemStack.EMPTY);
	}

	private static void migrateHelmet(ServerPlayer player) {
		ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
		if (!MaskSlots.accepts(helmet)) {
			return;
		}
		ItemStack moved = helmet.copy();
		player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
		if (MaskSlots.get(player).isEmpty()) {
			MaskSlots.set(player, moved);
		} else {
			player.getInventory().placeItemBackInInventory(moved);
		}
	}
}
