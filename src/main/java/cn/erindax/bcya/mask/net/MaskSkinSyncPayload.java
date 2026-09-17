package cn.erindax.bcya.mask.net;

import cn.erindax.bcya.BcyaMod;
import cn.erindax.bcya.mask.MaskSkinOverride;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MaskSkinSyncPayload(List<MaskSkinOverride> overrides) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<MaskSkinSyncPayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("mask_skin_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MaskSkinSyncPayload> STREAM_CODEC =
		CustomPacketPayload.codec(MaskSkinSyncPayload::write, MaskSkinSyncPayload::new);

	private MaskSkinSyncPayload(RegistryFriendlyByteBuf buf) {
		this(buf.readList(MaskSkinOverride::read));
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeCollection(overrides, (b, o) -> o.write(b));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
