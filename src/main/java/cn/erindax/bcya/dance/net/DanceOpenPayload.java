package cn.erindax.bcya.dance.net;

import cn.erindax.bcya.BcyaMod;
import cn.erindax.bcya.dance.DanceChart;
import cn.erindax.bcya.dance.DanceStanding;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DanceOpenPayload(int session, DanceChart chart, int audioVersion, List<DanceStanding> players)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<DanceOpenPayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("dance_open"));

	public static final StreamCodec<RegistryFriendlyByteBuf, DanceOpenPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, DanceOpenPayload::session,
		DanceChart.STREAM_CODEC, DanceOpenPayload::chart,
		ByteBufCodecs.INT, DanceOpenPayload::audioVersion,
		DanceStanding.LIST_CODEC, DanceOpenPayload::players,
		DanceOpenPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
