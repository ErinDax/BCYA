package cn.erindax.bcya.card.net;

import cn.erindax.bcya.BcyaMod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SaveCardPayload(CompoundTag card, boolean mainHand) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SaveCardPayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("card_save"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SaveCardPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.COMPOUND_TAG, SaveCardPayload::card,
		ByteBufCodecs.BOOL, SaveCardPayload::mainHand,
		SaveCardPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
