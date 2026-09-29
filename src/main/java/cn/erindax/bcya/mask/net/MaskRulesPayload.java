package cn.erindax.bcya.mask.net;

import cn.erindax.bcya.BcyaMod;
import cn.erindax.bcya.voice.VoicePreset;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MaskRulesPayload(List<ResourceLocation> voiceDisabled, List<ResourceLocation> swapBlacklist,
		List<VoicePreset> presets, Map<ResourceLocation, String> voice) implements CustomPacketPayload {

	private static final int MAX_MASKS = 256;

	public static final CustomPacketPayload.Type<MaskRulesPayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("mask_rules"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MaskRulesPayload> STREAM_CODEC = StreamCodec.composite(
		ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_MASKS)), MaskRulesPayload::voiceDisabled,
		ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_MASKS)), MaskRulesPayload::swapBlacklist,
		VoicePreset.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_MASKS)), MaskRulesPayload::presets,
		ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.stringUtf8(64), MAX_MASKS),
		MaskRulesPayload::voice,
		MaskRulesPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
