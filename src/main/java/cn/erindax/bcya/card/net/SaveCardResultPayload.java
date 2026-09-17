package cn.erindax.bcya.card.net;

import cn.erindax.bcya.BcyaMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SaveCardResultPayload(boolean success, String messageKey) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SaveCardResultPayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("card_save_result"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SaveCardResultPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, SaveCardResultPayload::success,
		ByteBufCodecs.STRING_UTF8, SaveCardResultPayload::messageKey,
		SaveCardResultPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
