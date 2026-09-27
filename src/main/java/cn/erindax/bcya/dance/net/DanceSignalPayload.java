package cn.erindax.bcya.dance.net;

import cn.erindax.bcya.BcyaMod;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DanceSignalPayload(int session, int signal) implements CustomPacketPayload {

	public static final int READY = 0;
	public static final int GO = 1;
	public static final int CLOSE = 2;

	public static final CustomPacketPayload.Type<DanceSignalPayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("dance_signal"));

	public static final StreamCodec<ByteBuf, DanceSignalPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, DanceSignalPayload::session,
		ByteBufCodecs.VAR_INT, DanceSignalPayload::signal,
		DanceSignalPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
