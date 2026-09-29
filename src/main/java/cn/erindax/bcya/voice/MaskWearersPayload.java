package cn.erindax.bcya.voice;

import cn.erindax.bcya.BcyaMod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MaskWearersPayload(Map<UUID, ResourceLocation> wearers) implements CustomPacketPayload {

	private static final int MAX_PLAYERS = 4096;

	public static final CustomPacketPayload.Type<MaskWearersPayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("mask_wearers"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MaskWearersPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, ResourceLocation.STREAM_CODEC, MAX_PLAYERS),
		MaskWearersPayload::wearers,
		MaskWearersPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
