package cn.erindax.bcya.mask.slot.net;

import cn.erindax.bcya.BcyaMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record MaskSlotCreativePayload(ItemStack stack) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<MaskSlotCreativePayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("mask_slot_creative"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MaskSlotCreativePayload> STREAM_CODEC =
		StreamCodec.composite(ItemStack.OPTIONAL_STREAM_CODEC, MaskSlotCreativePayload::stack, MaskSlotCreativePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
