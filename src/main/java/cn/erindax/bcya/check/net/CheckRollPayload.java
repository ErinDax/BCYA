package cn.erindax.bcya.check.net;

import cn.erindax.bcya.BcyaMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CheckRollPayload(int requestId) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<CheckRollPayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("check_roll"));

	public static final StreamCodec<RegistryFriendlyByteBuf, CheckRollPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, CheckRollPayload::requestId,
		CheckRollPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
