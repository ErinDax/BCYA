package cn.erindax.bcya.card.net;

import cn.erindax.bcya.BcyaMod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CardOpenPayload(CompoundTag card, CompoundTag ownerInfo, boolean readOnly, boolean mainHand)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<CardOpenPayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("card_open"));

	public static final StreamCodec<RegistryFriendlyByteBuf, CardOpenPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.COMPOUND_TAG, CardOpenPayload::card,
		ByteBufCodecs.COMPOUND_TAG, CardOpenPayload::ownerInfo,
		ByteBufCodecs.BOOL, CardOpenPayload::readOnly,
		ByteBufCodecs.BOOL, CardOpenPayload::mainHand,
		CardOpenPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
