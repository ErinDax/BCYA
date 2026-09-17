package cn.erindax.bcya.item;

import cn.erindax.bcya.mask.slot.MaskSlots;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MaskItem extends Item {

	private final int nameColor;
	private final ResourceLocation skinTexture;
	private final boolean slimModel;
	private final boolean renderOnHead;

	public MaskItem(int nameColor, ResourceLocation skinTexture, boolean slimModel, boolean renderOnHead,
			Properties properties) {
		super(properties);
		this.nameColor = nameColor;
		this.skinTexture = skinTexture;
		this.slimModel = slimModel;
		this.renderOnHead = renderOnHead;
	}

	public ResourceLocation getSkinTexture() {
		return skinTexture;
	}

	public boolean isSlimModel() {
		return slimModel;
	}

	public boolean rendersOnHead() {
		return renderOnHead;
	}

	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable(this.getDescriptionId(stack)).withStyle(Style.EMPTY.withColor(nameColor));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		ItemStack worn = MaskSlots.get(player);
		if (ItemStack.matches(held, worn)) {
			return InteractionResultHolder.fail(held);
		}
		if (level.isClientSide) {
			return InteractionResultHolder.success(held);
		}
		ItemStack toWear = player.isCreative() ? held.copyWithCount(1) : held.split(1);
		ItemStack remaining = held;
		if (!worn.isEmpty()) {
			if (held.isEmpty()) {
				remaining = worn.copy();
			} else if (!player.getInventory().add(worn.copy())) {
				player.drop(worn.copy(), false);
			}
		}
		MaskSlots.set(player, toWear);
		MaskSlots.playEquipSound(player);
		return InteractionResultHolder.consume(remaining);
	}
}
