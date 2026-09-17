package cn.erindax.bcya.mask.net;

import cn.erindax.bcya.BcyaMod;
import cn.erindax.bcya.mask.MaskSkinOverride;

import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MaskSkinEditorPayload(List<String> skins, List<MaskSkinOverride> overrides) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<MaskSkinEditorPayload> TYPE =
		new CustomPacketPayload.Type<>(BcyaMod.id("mask_skin_editor"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MaskSkinEditorPayload> STREAM_CODEC =
		CustomPacketPayload.codec(MaskSkinEditorPayload::write, MaskSkinEditorPayload::new);

	private MaskSkinEditorPayload(RegistryFriendlyByteBuf buf) {
		this(buf.readList(FriendlyByteBuf::readUtf), buf.readList(MaskSkinOverride::read));
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeCollection(skins, FriendlyByteBuf::writeUtf);
		buf.writeCollection(overrides, (b, o) -> o.write(b));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
