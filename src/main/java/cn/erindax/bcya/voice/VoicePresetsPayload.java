package cn.erindax.bcya.voice;

import cn.erindax.bcya.BcyaMod;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record VoicePresetsPayload(List<VoicePreset> presets) implements CustomPacketPayload {

	private static final int MAX_PRESETS = 64;

	public static final CustomPacketPayload.Type<VoicePresetsPayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("voice_presets"));

	public static final StreamCodec<RegistryFriendlyByteBuf, VoicePresetsPayload> STREAM_CODEC = StreamCodec.composite(
		VoicePreset.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_PRESETS)), VoicePresetsPayload::presets,
		VoicePresetsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
