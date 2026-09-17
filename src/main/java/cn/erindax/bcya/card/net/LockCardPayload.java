package cn.erindax.bcya.card.net;

import cn.erindax.bcya.BcyaMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record LockCardPayload(boolean locked, boolean mainHand) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<LockCardPayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("card_lock"));

	public static final StreamCodec<RegistryFriendlyByteBuf, LockCardPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, LockCardPayload::locked,
		ByteBufCodecs.BOOL, LockCardPayload::mainHand,
		LockCardPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
