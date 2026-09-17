package cn.erindax.bcya.voice;

import cn.erindax.bcya.BcyaMod;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MaskVoiceSyncPayload(List<ResourceLocation> disabledMasks) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<MaskVoiceSyncPayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("mask_voice_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MaskVoiceSyncPayload> STREAM_CODEC = StreamCodec.composite(
		ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()),
		MaskVoiceSyncPayload::disabledMasks,
		MaskVoiceSyncPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
