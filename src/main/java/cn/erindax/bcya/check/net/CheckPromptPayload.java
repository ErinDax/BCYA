package cn.erindax.bcya.check.net;

import cn.erindax.bcya.BcyaMod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CheckPromptPayload(CompoundTag data) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<CheckPromptPayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("check_prompt"));

	public static final StreamCodec<RegistryFriendlyByteBuf, CheckPromptPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.COMPOUND_TAG, CheckPromptPayload::data,
		CheckPromptPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
