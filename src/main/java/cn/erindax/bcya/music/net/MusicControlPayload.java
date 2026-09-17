package cn.erindax.bcya.music.net;

import cn.erindax.bcya.BcyaMod;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MusicControlPayload(UUID session, Action action, String track, int version, int range, int entityId,
		BlockPos pos) implements CustomPacketPayload {

	public enum Action {
		PLAY, PAUSE, RESUME, STOP
	}

	public static final int NO_ENTITY = -1;

	public static final CustomPacketPayload.Type<MusicControlPayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("music_control"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MusicControlPayload> STREAM_CODEC =
		CustomPacketPayload.codec(MusicControlPayload::write, MusicControlPayload::new);

	public static MusicControlPayload simple(UUID session, Action action) {
		return new MusicControlPayload(session, action, "", 0, 0, NO_ENTITY, BlockPos.ZERO);
	}

	private MusicControlPayload(RegistryFriendlyByteBuf buf) {
		this(buf.readUUID(), Action.values()[buf.readVarInt()], buf.readUtf(), buf.readInt(), buf.readVarInt(),
			buf.readInt(), buf.readBlockPos());
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeUUID(session);
		buf.writeVarInt(action.ordinal());
		buf.writeUtf(track);
		buf.writeInt(version);
		buf.writeVarInt(range);
		buf.writeInt(entityId);
		buf.writeBlockPos(pos);
	}

	public boolean followsEntity() {
		return entityId != NO_ENTITY;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
