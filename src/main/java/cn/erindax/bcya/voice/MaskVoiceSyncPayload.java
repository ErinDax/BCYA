package cn.erindax.bcya.voice;

import cn.erindax.bcya.BcyaMod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MaskVoiceSyncPayload(List<ResourceLocation> disabledMasks, List<VoicePreset> presets,
		Map<ResourceLocation, String> assignments) implements CustomPacketPayload {

	private static final int MAX_ENTRIES = 256;

	public static final CustomPacketPayload.Type<MaskVoiceSyncPayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("mask_voice_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MaskVoiceSyncPayload> STREAM_CODEC = StreamCodec.composite(
		ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ENTRIES)), MaskVoiceSyncPayload::disabledMasks,
		VoicePreset.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ENTRIES)), MaskVoiceSyncPayload::presets,
		ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.stringUtf8(64), MAX_ENTRIES),
		MaskVoiceSyncPayload::assignments,
		MaskVoiceSyncPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
