package cn.erindax.bcya.item;

import cn.erindax.bcya.mask.MaskOrigins;

import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class AgateItem extends Item {

	private static final int COOLDOWN_TICKS = 20;

	public AgateItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
		}
		MaskOrigins.Outcome outcome = MaskOrigins.restore(serverPlayer);
		if (outcome.result() != MaskOrigins.Result.RESTORED) {
			serverPlayer.displayClientMessage(Component.translatable(
				"item.bcya.agate." + outcome.result().name().toLowerCase(Locale.ROOT)), true);
			return InteractionResultHolder.fail(stack);
		}
		serverPlayer.displayClientMessage(Component.translatable("item.bcya.agate.restored",
			new ItemStack(outcome.mask()).getHoverName()), true);
		serverPlayer.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
		serverPlayer.awardStat(Stats.ITEM_USED.get(this));
		stack.consume(1, serverPlayer);
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("item.bcya.agate.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
