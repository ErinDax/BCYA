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
import net.minecraft.world.entity.player.PlayerModelPart;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class CardSkinRenderer {

	private static final float SIDE_ANGLE = 30.0F;
	private static final Vector3f TRANSLATION = new Vector3f(0.0F, 0.9F, 0.0F);
	private static final Quaternionf BODY_ROT = new Quaternionf().rotateZ((float) Math.PI);
	private static final Quaternionf VIEW_ROT = new Quaternionf().rotateX((float) Math.toRadians(6.0F));
	private static final Map<String, Supplier<PlayerSkin>> SKIN_CACHE = new HashMap<>();

	private static CardPlayer dummy;

	private CardSkinRenderer() {
	}

	public static void render(GuiGraphics graphics, int x0, int y0, int x1, int y1,
			UUID ownerId, String ownerName, String skinValue, String skinSig) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null || x1 <= x0 || y1 <= y0) {
			return;
		}
		if (ownerId == null) {
			ownerId = new UUID(0L, 0L);
		}
		PlayerSkin skin = skinSupplier(ownerId, ownerName, skinValue, skinSig).get();

		if (dummy == null || dummy.level() != level) {
			dummy = new CardPlayer(level, new GameProfile(ownerId, ownerName == null ? "" : ownerName));
		}
		dummy.skin = skin;

		float yaw = 180.0F - SIDE_ANGLE;
		dummy.yBodyRot = yaw;
		dummy.yBodyRotO = yaw;
		dummy.setYRot(yaw);
		dummy.setXRot(0.0F);
		dummy.yHeadRot = yaw;
		dummy.yHeadRotO = yaw;

		float boxW = x1 - x0;
		float boxH = y1 - y0;
		float scale = Math.min(boxW / 1.35F, boxH / 2.12F);
		float centerX = (x0 + x1) / 2.0F;
		float centerY = (y0 + y1) / 2.0F;

		graphics.enableScissor(x0, y0, x1, y1);
		InventoryScreen.renderEntityInInventory(graphics, centerX, centerY, scale,
			TRANSLATION, BODY_ROT, VIEW_ROT, dummy);
		graphics.disableScissor();
	}

	public static void clear() {
		dummy = null;
		SKIN_CACHE.clear();
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
			this.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7E);
		}

		@Override
		public PlayerSkin getSkin() {
			return skin;
		}

		@Override
		public boolean isModelPartShown(PlayerModelPart part) {
			return part != PlayerModelPart.CAPE && super.isModelPartShown(part);
		}
	}
}
