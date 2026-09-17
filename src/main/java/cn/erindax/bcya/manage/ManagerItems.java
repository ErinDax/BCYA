package cn.erindax.bcya.manage;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.ItemLike;

public final class ManagerItems {

	private ManagerItems() {
	}

	public static ItemStack filler() {
		ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
		stack.set(DataComponents.HIDE_TOOLTIP, Unit.INSTANCE);
		return stack;
	}

	public static ItemStack button(ItemLike icon, String nameKey, String loreKey) {
		ItemStack stack = new ItemStack(icon);
		stack.set(DataComponents.CUSTOM_NAME, plain(Component.translatable(nameKey).withStyle(ChatFormatting.YELLOW)));
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			plain(Component.translatable(loreKey).withStyle(ChatFormatting.GRAY)))));
		return stack;
	}

	public static ItemStack head(ServerPlayer player) {
		ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
		stack.set(DataComponents.PROFILE, new ResolvableProfile(player.getGameProfile()));
		stack.set(DataComponents.CUSTOM_NAME, plain(Component.literal(player.getGameProfile().getName())
			.withStyle(ChatFormatting.AQUA)));
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			plain(Component.translatable("screen.bcya.players.open_inventory").withStyle(ChatFormatting.GRAY)))));
		return stack;
	}

	private static Component plain(Component component) {
		return component.copy().withStyle(style -> style.withItalic(false));
	}
}
