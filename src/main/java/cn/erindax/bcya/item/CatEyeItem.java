package cn.erindax.bcya.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class CatEyeItem extends Item {

	public static final int HOTBAR_SLOT = 0;

	public CatEyeItem(Properties properties) {
		super(properties);
	}

	public static boolean isActive(Player player) {
		return player != null && player.getInventory().getItem(HOTBAR_SLOT).is(ModItems.CAT_EYE);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("item.bcya.cat_eye.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
