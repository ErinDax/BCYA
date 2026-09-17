package cn.erindax.bcya.mask.slot.net;

import cn.erindax.bcya.BcyaMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MaskSlotClickPayload(boolean quickMove) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<MaskSlotClickPayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("mask_slot_click"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MaskSlotClickPayload> STREAM_CODEC =
		StreamCodec.composite(ByteBufCodecs.BOOL, MaskSlotClickPayload::quickMove, MaskSlotClickPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
