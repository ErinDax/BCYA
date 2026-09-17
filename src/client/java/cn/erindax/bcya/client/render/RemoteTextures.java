package cn.erindax.bcya.client.render;

import cn.erindax.bcya.BcyaMod;
import cn.erindax.bcya.skin.TextureStore;

import com.mojang.blaze3d.platform.NativeImage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

public final class RemoteTextures {

	private record Entry(ResourceLocation id, byte[] png) {
	}

	private static final Map<String, Entry> TEXTURES = new HashMap<>();

	private RemoteTextures() {
	}

	public static boolean has(String kind, String name) {
		return TEXTURES.containsKey(key(kind, name));
	}

	@Nullable
	public static ResourceLocation get(String kind, String name) {
		Entry entry = TEXTURES.get(key(kind, name));
		if (entry == null) {
			return null;
		}
		TextureManager textures = Minecraft.getInstance().getTextureManager();
		AbstractTexture texture = textures.getTexture(entry.id(), null);
		if (!(texture instanceof DynamicTexture dynamic) || dynamic.getPixels() == null) {
			if (!upload(textures, kind, entry)) {
				return null;
			}
		}
		return entry.id();
	}

	public static ResourceLocation skin(String name) {
		ResourceLocation id = get(TextureStore.SKINS.kind(), name);
		return id != null ? id : DefaultPlayerSkin.getDefaultTexture();
	}

	public static void put(String kind, String name, byte[] png) {
		Entry entry = new Entry(BcyaMod.id("remote/" + kind + "/" + name.toLowerCase()), png);
		if (upload(Minecraft.getInstance().getTextureManager(), kind, entry)) {
			TEXTURES.put(key(kind, name), entry);
			MaskSkins.invalidate();
		}
	}

	public static void clear() {
		TextureManager textures = Minecraft.getInstance().getTextureManager();
		TEXTURES.values().forEach(entry -> textures.release(entry.id()));
		TEXTURES.clear();
		MaskSkins.invalidate();
	}

	private static String key(String kind, String name) {
		return kind + "/" + name;
	}

	private static boolean upload(TextureManager textures, String kind, Entry entry) {
		NativeImage image = decode(kind, entry);
		if (image == null) {
			return false;
		}
		textures.register(entry.id(), new DynamicTexture(image));
		return true;
	}

	@Nullable
	private static NativeImage decode(String kind, Entry entry) {
		NativeImage image;
		try {
			image = NativeImage.read(entry.png());
		} catch (IOException e) {
			BcyaMod.LOGGER.warn("Received invalid texture {}", entry.id(), e);
			return null;
		}
		if (!validSize(kind, image.getWidth(), image.getHeight())) {
			BcyaMod.LOGGER.warn("Texture {} has unsupported size {}x{}", entry.id(), image.getWidth(), image.getHeight());
			image.close();
			return null;
		}
		return image;
	}

	private static boolean validSize(String kind, int width, int height) {
		if (kind.equals(TextureStore.SKINS.kind())) {
			return width == 64 && (height == 64 || height == 32);
		}
		return width == height && width >= 8 && width <= 128;
	}
}
