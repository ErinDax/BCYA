package cn.erindax.bcya.mask.net;

import cn.erindax.bcya.item.ModItems;
import cn.erindax.bcya.manage.WandWhitelist;
import cn.erindax.bcya.mask.MaskRulesState;
import cn.erindax.bcya.mask.MaskSkinOverride;
import cn.erindax.bcya.skin.TextureStore;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class MaskSkinHandler {

	private MaskSkinHandler() {
	}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(MaskSkinSyncPayload.TYPE, MaskSkinSyncPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(MaskSkinEditorPayload.TYPE, MaskSkinEditorPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(MaskSkinUpdatePayload.TYPE, MaskSkinUpdatePayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(MaskSkinUpdatePayload.TYPE,
			(payload, context) -> update(context.player(), payload));
	}

	public static void sendAllTo(ServerPlayer player) {
		MaskRulesState state = MaskRulesState.get(player.server);
		for (MaskSkinOverride override : state.getSkinOverrides()) {
			TextureStore.SKINS.sendTo(player, override.skin());
		}
		ServerPlayNetworking.send(player, state.toSkinPayload());
	}

	public static void openEditor(ServerPlayer player) {
		if (!WandWhitelist.canManage(player)) {
			return;
		}
		ServerPlayNetworking.send(player, new MaskSkinEditorPayload(
			TextureStore.SKINS.listAvailable(), MaskRulesState.get(player.server).getSkinOverrides()));
	}

	private static void update(ServerPlayer player, MaskSkinUpdatePayload payload) {
		if (!WandWhitelist.canManage(player)) {
			return;
		}
		MinecraftServer server = player.server;
		List<MaskSkinOverride> accepted = new ArrayList<>();
		for (MaskSkinOverride override : payload.overrides()) {
			if (!ModItems.MASKS.contains(BuiltInRegistries.ITEM.get(override.mask()))) {
				continue;
			}
			if (override.skin().isEmpty()) {
				continue;
			}
			if (TextureStore.SKINS.load(override.skin(), true) == null) {
				player.sendSystemMessage(Component.translatable("commands.bcya.patrol.skin_missing",
					TextureStore.SKINS.directory().resolve(override.skin() + ".png").toString()));
				continue;
			}
			accepted.add(override);
		}

		MaskRulesState state = MaskRulesState.get(server);
		state.setSkinOverrides(accepted);

		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		for (MaskSkinOverride override : accepted) {
			TextureStore.SKINS.sendToAll(players, override.skin());
		}
		MaskSkinSyncPayload sync = state.toSkinPayload();
		for (ServerPlayer target : players) {
			ServerPlayNetworking.send(target, sync);
		}
		player.displayClientMessage(Component.translatable("screen.bcya.mask_skins.saved"), true);
	}
}
