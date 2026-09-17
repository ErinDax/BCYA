package cn.erindax.bcya.client.render;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class CardSkinRenderer {

	private static final float SIDE_ANGLE = 30.0F;
	private static final Map<String, Supplier<PlayerSkin>> SKIN_CACHE = new HashMap<>();
	private static final Map<UUID, CardPlayer> DUMMY_CACHE = new HashMap<>();

	private CardSkinRenderer() {
	}

	public static void render(GuiGraphics graphics, int x0, int y0, int x1, int y1,
			UUID ownerId, String ownerName, String skinValue, String skinSig) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return;
		}
		if (ownerId == null) {
			ownerId = new UUID(0L, 0L);
		}
		PlayerSkin skin = skinSupplier(ownerId, ownerName, skinValue, skinSig).get();

		CardPlayer dummy = DUMMY_CACHE.get(ownerId);
		if (dummy == null || dummy.level() != level) {
			dummy = new CardPlayer(level, new GameProfile(ownerId, ownerName == null ? "" : ownerName));
			DUMMY_CACHE.put(ownerId, dummy);
		}
		dummy.skin = skin;

		float yaw = 180.0F + SIDE_ANGLE;
		dummy.yBodyRot = yaw;
		dummy.yBodyRotO = yaw;
		dummy.setYRot(yaw);
		dummy.setXRot(0.0F);
		dummy.yHeadRot = yaw;
		dummy.yHeadRotO = yaw;

		graphics.enableScissor(x0, y0, x1, y1);
		float centerX = (x0 + x1) / 2.0F;
		float centerY = (y0 + y1) / 2.0F;
		InventoryScreen.renderEntityInInventory(graphics, centerX, centerY, 44.0F,
			new Vector3f(0.0F, 0.72F, 0.0F),
			new Quaternionf().rotateZ((float) Math.PI), new Quaternionf(), dummy);
		graphics.disableScissor();
	}

	private static Supplier<PlayerSkin> skinSupplier(UUID id, String name, String value, String signature) {
		String safeName = name == null ? "" : name;
		String safeValue = value == null ? "" : value;
		String safeSig = signature == null ? "" : signature;
		String cacheKey = id + "\u0000" + safeValue + "\u0000" + safeSig;
		return SKIN_CACHE.computeIfAbsent(cacheKey, key -> {
			GameProfile profile = new GameProfile(id, safeName);
			if (!safeValue.isEmpty()) {
				profile.getProperties().put("textures",
					new Property("textures", safeValue, safeSig.isEmpty() ? null : safeSig));
			}
			return Minecraft.getInstance().getSkinManager().lookupInsecure(profile);
		});
	}

	private static final class CardPlayer extends AbstractClientPlayer {
		private PlayerSkin skin;

		CardPlayer(ClientLevel level, GameProfile profile) {
			super(level, profile);
		}

		@Override
		public PlayerSkin getSkin() {
			return skin;
		}
	}
}
