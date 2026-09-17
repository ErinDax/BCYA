package cn.erindax.bcya.mask.slot;

import cn.erindax.bcya.item.MaskItem;

import java.util.function.Consumer;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class MaskSlots {

	private static Consumer<Player> clientCreativeSync = player -> {
	};

	private MaskSlots() {
	}

	public static ItemStack get(Player player) {
		return player.getAttachedOrCreate(ModAttachments.MASK);
	}

	public static void set(Player player, ItemStack stack) {
		player.setAttached(ModAttachments.MASK, stack.isEmpty() ? ItemStack.EMPTY : stack);
	}

	public static boolean accepts(ItemStack stack) {
		return stack.getItem() instanceof MaskItem;
	}

	public static void setClientCreativeSync(Consumer<Player> sync) {
		clientCreativeSync = sync;
	}

	public static void onEquipped(Player player) {
		if (!player.level().isClientSide) {
			playEquipSound(player);
		}
	}

	public static void notifyClientChanged(Player player) {
		if (player.level().isClientSide && player.isCreative()) {
			clientCreativeSync.accept(player);
		}
	}

	public static void playEquipSound(Player player) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
			SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
	}
}
