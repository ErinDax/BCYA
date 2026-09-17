package cn.erindax.bcya.mask.net;

import cn.erindax.bcya.BcyaMod;
import cn.erindax.bcya.mask.MaskSkinOverride;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MaskSkinUpdatePayload(List<MaskSkinOverride> overrides) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<MaskSkinUpdatePayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("mask_skin_update"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MaskSkinUpdatePayload> STREAM_CODEC =
		CustomPacketPayload.codec(MaskSkinUpdatePayload::write, MaskSkinUpdatePayload::new);

	private MaskSkinUpdatePayload(RegistryFriendlyByteBuf buf) {
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
