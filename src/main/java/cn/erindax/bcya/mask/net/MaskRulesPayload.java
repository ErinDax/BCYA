package cn.erindax.bcya.mask.net;

import cn.erindax.bcya.BcyaMod;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MaskRulesPayload(List<ResourceLocation> voiceDisabled, List<ResourceLocation> swapBlacklist)
		implements CustomPacketPayload {

	private static final int MAX_MASKS = 256;

	public static final CustomPacketPayload.Type<MaskRulesPayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("mask_rules"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MaskRulesPayload> STREAM_CODEC = StreamCodec.composite(
		ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_MASKS)), MaskRulesPayload::voiceDisabled,
		ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_MASKS)), MaskRulesPayload::swapBlacklist,
		MaskRulesPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
