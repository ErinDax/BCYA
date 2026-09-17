package cn.erindax.bcya.client.render;

import cn.erindax.bcya.item.MaskItem;
import cn.erindax.bcya.mask.MaskSkinOverride;
import cn.erindax.bcya.skin.TextureStore;

import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class MaskSkins {

	private static final Map<MaskItem, PlayerSkin> BUILTIN = new IdentityHashMap<>();
	private static final Map<MaskItem, PlayerSkin> RESOLVED = new IdentityHashMap<>();
	private static Map<ResourceLocation, MaskSkinOverride> overrides = new HashMap<>();

	private MaskSkins() {
	}

	public static PlayerSkin of(MaskItem mask) {
		PlayerSkin cached = RESOLVED.get(mask);
		if (cached != null) {
			return cached;
		}
		MaskSkinOverride override = overrides.get(BuiltInRegistries.ITEM.getKey(mask));
		if (override == null) {
			return BUILTIN.computeIfAbsent(mask, m -> new PlayerSkin(
				m.getSkinTexture(), null, null, null,
				m.isSlimModel() ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE,
				true));
		}
		if (!RemoteTextures.has(TextureStore.SKINS.kind(), override.skin())) {
			return BUILTIN.computeIfAbsent(mask, m -> new PlayerSkin(
				m.getSkinTexture(), null, null, null,
				m.isSlimModel() ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE,
				true));
		}
		PlayerSkin skin = new PlayerSkin(RemoteTextures.skin(override.skin()), null, null, null,
			override.slim() ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE, true);
		RESOLVED.put(mask, skin);
		return skin;
	}

	public static void setOverrides(Collection<MaskSkinOverride> list) {
		Map<ResourceLocation, MaskSkinOverride> map = new HashMap<>();
		for (MaskSkinOverride override : list) {
			map.put(override.mask(), override);
		}
		overrides = map;
		invalidate();
	}

	public static void reset() {
		overrides = new HashMap<>();
		invalidate();
	}

	public static void invalidate() {
		RESOLVED.clear();
	}
}
