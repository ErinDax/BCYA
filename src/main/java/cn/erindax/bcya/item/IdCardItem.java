package cn.erindax.bcya.item;

import cn.erindax.bcya.card.CardData;
import cn.erindax.bcya.card.CardHandler;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class IdCardItem extends Item {

	public IdCardItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player instanceof ServerPlayer serverPlayer) {
			CardHandler.open(serverPlayer, hand);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		CompoundTag tag = CardData.read(stack);
		String investigator = tag.getString(CardData.INVESTIGATOR).trim();
		String owner = tag.getString(CardData.OWNER_NAME);
		if (owner.isEmpty()) {
			owner = "-";
		}
		boolean bound = !tag.getString(CardData.OWNER_UUID).isEmpty();
		if (!bound && investigator.isEmpty()) {
			tooltip.add(Component.translatable("item.bcya.id_card.blank").withStyle(ChatFormatting.DARK_GRAY));
		} else {
			tooltip.add(Component.translatable("item.bcya.id_card.named",
					investigator.isEmpty() ? Component.translatable("item.bcya.id_card.none") : investigator, owner)
				.withStyle(ChatFormatting.GRAY));
			if (tag.getBoolean(CardData.AWAKENED) && !tag.getString(CardData.ABILITY_NAME).isBlank()) {
				tooltip.add(Component.translatable("item.bcya.id_card.awakened", tag.getString(CardData.ABILITY_NAME))
					.withStyle(ChatFormatting.LIGHT_PURPLE));
			}
		}
		tooltip.add(Component.translatable("item.bcya.id_card.tooltip").withStyle(ChatFormatting.DARK_GRAY));
	}
}
