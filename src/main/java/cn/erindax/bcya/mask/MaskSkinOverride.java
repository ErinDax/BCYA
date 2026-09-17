package cn.erindax.bcya.mask;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record MaskSkinOverride(ResourceLocation mask, String skin, boolean slim) {

	public static MaskSkinOverride read(FriendlyByteBuf buf) {
		return new MaskSkinOverride(buf.readResourceLocation(), buf.readUtf(), buf.readBoolean());
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeResourceLocation(mask);
		buf.writeUtf(skin);
		buf.writeBoolean(slim);
	}
}
