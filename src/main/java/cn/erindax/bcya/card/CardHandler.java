package cn.erindax.bcya.card;

import cn.erindax.bcya.card.net.CardOpenPayload;
import cn.erindax.bcya.card.net.SaveCardPayload;
import cn.erindax.bcya.card.net.SaveCardResultPayload;
import cn.erindax.bcya.item.ModItems;

import com.mojang.authlib.properties.Property;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class CardHandler {

	public static final String MSG_SAVED = "item.bcya.id_card.saved";
	public static final String MSG_NO_CARD = "item.bcya.id_card.no_card";
	public static final String MSG_READONLY = "item.bcya.id_card.readonly";
	public static final String MSG_NEED_INVESTIGATOR = "item.bcya.id_card.need_investigator";

	private CardHandler() {
	}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(CardOpenPayload.TYPE, CardOpenPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(SaveCardResultPayload.TYPE, SaveCardResultPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(SaveCardPayload.TYPE, SaveCardPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SaveCardPayload.TYPE,
			(payload, context) -> onSave(context.player(), payload));
	}

	public static void open(ServerPlayer player, InteractionHand hand) {
		if (player.isSpectator()) {
			return;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (!stack.is(ModItems.ID_CARD)) {
			return;
		}
		CompoundTag card = CardData.read(stack);
		CompoundTag ownerInfo = buildOwnerInfo(player, card);
		boolean hasOwner = ownerInfo.getBoolean("has_owner");
		boolean readOnly = hasOwner && !ownerInfo.getString("uuid").equals(player.getUUID().toString());
		ServerPlayNetworking.send(player, new CardOpenPayload(card, ownerInfo, readOnly,
			hand == InteractionHand.MAIN_HAND));
	}

	private static void onSave(ServerPlayer player, SaveCardPayload payload) {
		if (player.isSpectator()) {
			return;
		}
		InteractionHand hand = payload.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
		ItemStack stack = player.getItemInHand(hand);
		if (!stack.is(ModItems.ID_CARD)) {
			sendResult(player, false, MSG_NO_CARD);
			return;
		}
		CompoundTag existing = CardData.read(stack);
		String existingOwner = existing.getString(CardData.OWNER_UUID);
		if (!existingOwner.isEmpty() && !existingOwner.equals(player.getUUID().toString())) {
			sendResult(player, false, MSG_READONLY);
			return;
		}

		CompoundTag incoming = payload.card().copy();
		incoming.putString(CardData.INVESTIGATOR, incoming.getString(CardData.INVESTIGATOR).trim());
		if (incoming.getString(CardData.INVESTIGATOR).isEmpty()) {
			sendResult(player, false, MSG_NEED_INVESTIGATOR);
			return;
		}

		CompoundTag normalized = CardData.normalize(incoming);
		if (existingOwner.isEmpty()) {
			normalized.putString(CardData.CARD_ID, UUID.randomUUID().toString());
			normalized.putString(CardData.OWNER_UUID, player.getUUID().toString());
			normalized.putString(CardData.OWNER_NAME, playerName(player));
			captureSkin(player, normalized);
		} else {
			normalized.putString(CardData.CARD_ID, existing.getString(CardData.CARD_ID));
			normalized.putString(CardData.OWNER_UUID, existingOwner);
			normalized.putString(CardData.OWNER_NAME, existing.getString(CardData.OWNER_NAME));
			normalized.putString(CardData.OWNER_SKIN, existing.getString(CardData.OWNER_SKIN));
			normalized.putString(CardData.OWNER_SKIN_SIG, existing.getString(CardData.OWNER_SKIN_SIG));
		}

		CardData.write(stack, normalized);
		player.inventoryMenu.broadcastChanges();
		player.containerMenu.broadcastChanges();
		CardArchive.get(player.server).put(normalized);
		sendResult(player, true, MSG_SAVED);
	}

	private static void sendResult(ServerPlayer player, boolean success, String messageKey) {
		ServerPlayNetworking.send(player, new SaveCardResultPayload(success, messageKey));
	}

	private static String playerName(ServerPlayer player) {
		String name = player.getGameProfile().getName();
		return name == null ? player.getName().getString() : name;
	}

	private static void captureSkin(ServerPlayer player, CompoundTag card) {
		Property textures = firstTexture(player);
		if (textures != null) {
			card.putString(CardData.OWNER_SKIN, textures.value());
			card.putString(CardData.OWNER_SKIN_SIG, textures.signature() == null ? "" : textures.signature());
		}
	}

	private static Property firstTexture(ServerPlayer player) {
		for (Property property : player.getGameProfile().getProperties().get("textures")) {
			return property;
		}
		return null;
	}

	private static CompoundTag buildOwnerInfo(ServerPlayer viewer, CompoundTag card) {
		CompoundTag info = new CompoundTag();
		String uuidString = card.getString(CardData.OWNER_UUID);
		boolean hasOwner = !uuidString.isEmpty();
		UUID ownerId = viewer.getUUID();
		if (hasOwner) {
			try {
				ownerId = UUID.fromString(uuidString);
			} catch (IllegalArgumentException ignored) {
				hasOwner = false;
				ownerId = viewer.getUUID();
			}
		}
		String name = hasOwner ? card.getString(CardData.OWNER_NAME) : playerName(viewer);
		String skin = card.getString(CardData.OWNER_SKIN);
		String skinSig = card.getString(CardData.OWNER_SKIN_SIG);

		ServerPlayer online = viewer.server.getPlayerList().getPlayer(ownerId);
		if (online != null) {
			Property textures = firstTexture(online);
			if (textures != null) {
				skin = textures.value();
				skinSig = textures.signature() == null ? "" : textures.signature();
			}
			name = online.getGameProfile().getName();
		}

		info.putBoolean("has_owner", hasOwner);
		info.putString("uuid", ownerId.toString());
		info.putString("name", name == null ? "" : name);
		info.putString("skin", skin == null ? "" : skin);
		info.putString("skin_sig", skinSig == null ? "" : skinSig);
		return info;
	}
}
