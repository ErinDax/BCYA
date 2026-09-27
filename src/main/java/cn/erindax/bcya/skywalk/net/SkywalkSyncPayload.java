package cn.erindax.bcya.skywalk.net;

import cn.erindax.bcya.BcyaMod;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SkywalkSyncPayload(boolean enabled, int depth) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SkywalkSyncPayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("skywalk_sync"));

	public static final StreamCodec<ByteBuf, SkywalkSyncPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, SkywalkSyncPayload::enabled,
		ByteBufCodecs.VAR_INT, SkywalkSyncPayload::depth,
		SkywalkSyncPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
