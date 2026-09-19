package cn.erindax.bcya.check.net;

import cn.erindax.bcya.BcyaMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record KpCheckPayload(String targetUuid, String skill, int difficulty) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<KpCheckPayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("kp_check"));

	public static final StreamCodec<RegistryFriendlyByteBuf, KpCheckPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, KpCheckPayload::targetUuid,
		ByteBufCodecs.STRING_UTF8, KpCheckPayload::skill,
		ByteBufCodecs.VAR_INT, KpCheckPayload::difficulty,
		KpCheckPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
