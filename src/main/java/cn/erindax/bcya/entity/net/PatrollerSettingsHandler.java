package cn.erindax.bcya.entity.net;

import cn.erindax.bcya.entity.PatrolRecorder;
import cn.erindax.bcya.entity.PatrollerEntity;
import cn.erindax.bcya.entity.Patrollers;
import cn.erindax.bcya.manage.WandWhitelist;
import cn.erindax.bcya.skin.TextureStore;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.jetbrains.annotations.Nullable;

public final class PatrollerSettingsHandler {

	private PatrollerSettingsHandler() {
	}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(PatrollerSettingsPayload.TYPE, PatrollerSettingsPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(PatrollerListPayload.TYPE, PatrollerListPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(PatrollerUpdatePayload.TYPE, PatrollerUpdatePayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(PatrollerListActionPayload.TYPE, PatrollerListActionPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(PatrollerUpdatePayload.TYPE,
			(payload, context) -> update(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(PatrollerListActionPayload.TYPE,
			(payload, context) -> listAction(context.player(), payload));
	}

	public static void openSettings(ServerPlayer player, PatrollerEntity patroller, boolean fromList) {
		ServerPlayNetworking.send(player, new PatrollerSettingsPayload(
			patroller.getId(),
			patroller.getPatrolName(),
			patroller.getSkinName(),
			patroller.isSlim(),
			patroller.getPauseTicks() / 20,
			patroller.getDetectDiameter(),
			patroller.getStareTicks() / 20,
			patroller.isNameVisible(),
			patroller.showsHeldItems(),
			patroller.isPatrolling(),
			patroller.isHostile(),
			patroller.getWaypoints().size(),
			TextureStore.SKINS.listAvailable(),
			fromList));
	}

	public static void openList(ServerPlayer player) {
		List<PatrollerListPayload.Entry> entries = new ArrayList<>();
		for (PatrollerEntity p : Patrollers.all(player.server)) {
			BlockPos pos = p.blockPosition();
			entries.add(new PatrollerListPayload.Entry(p.getPatrolName(), p.level().dimension().location().toString(),
				pos.getX(), pos.getY(), pos.getZ(), p.getWaypoints().size(), p.getSkinName()));
		}
		entries.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
		ServerPlayNetworking.send(player, new PatrollerListPayload(entries));
	}

	@Nullable
	private static PatrollerEntity resolveByName(ServerPlayer player, String name) {
		if (!WandWhitelist.canManage(player)) {
			return null;
		}
		PatrollerEntity patroller = Patrollers.findByName(player.server, name);
		if (patroller == null) {
			player.sendSystemMessage(Component.translatable("commands.bcya.patrol.not_found", name));
		}
		return patroller;
	}

	private static void update(ServerPlayer player, PatrollerUpdatePayload payload) {
		PatrollerEntity patroller = Patrollers.findByName(player.server, payload.originalName());
		if (patroller == null || !WandWhitelist.canManage(player)) {
			return;
		}
		String name = payload.name().trim();
		if (!Patrollers.isValidName(name)) {
			player.sendSystemMessage(Component.translatable("commands.bcya.patrol.bad_name", name));
			return;
		}
		PatrollerEntity existing = Patrollers.findByName(player.server, name);
		if (existing != null && existing != patroller) {
			player.sendSystemMessage(Component.translatable("commands.bcya.patrol.name_taken", name));
			return;
		}
		String skin = payload.skin();
		if (!skin.isEmpty() && TextureStore.SKINS.load(skin, true) == null) {
			player.sendSystemMessage(Component.translatable("commands.bcya.patrol.skin_missing",
				TextureStore.SKINS.directory().resolve(skin + ".png").toString()));
			return;
		}

		patroller.setPatrolName(name);
		patroller.setPauseTicks(Math.max(0, payload.pauseSeconds()) * 20);
		patroller.setDetectDiameter(payload.detectDiameter());
		patroller.setStareTicks(Math.max(0, payload.stareSeconds()) * 20);
		patroller.setNameVisible(payload.nameVisible());
		patroller.setShowHeldItems(payload.showHeldItems());
		patroller.setPatrolling(payload.patrolling());
		patroller.setHostile(payload.aggressive());
		patroller.setSkin(skin, payload.slim());
		if (!skin.isEmpty()) {
			TextureStore.SKINS.sendToAll(PlayerLookup.tracking(patroller), skin);
		}
		player.displayClientMessage(Component.translatable("commands.bcya.patrol.saved", name), true);
		if (payload.fromList()) {
			openList(player);
		}
	}

	private static void listAction(ServerPlayer player, PatrollerListActionPayload payload) {
		if (!WandWhitelist.canManage(player)) {
			return;
		}
		switch (payload.action()) {
			case REFRESH -> openList(player);
			case EDIT -> {
				PatrollerEntity patroller = resolveByName(player, payload.name());
				if (patroller != null) {
					openSettings(player, patroller, true);
				}
			}
			case TELEPORT -> {
				PatrollerEntity patroller = resolveByName(player, payload.name());
				if (patroller != null) {
					player.teleportTo((ServerLevel) patroller.level(), patroller.getX(), patroller.getY(),
						patroller.getZ(), player.getYRot(), player.getXRot());
				}
			}
			case REMOVE -> {
				PatrollerEntity patroller = resolveByName(player, payload.name());
				if (patroller != null) {
					remove(player, patroller);
				}
				openList(player);
			}
			case RECORD -> {
				String name = payload.name().trim();
				if (!Patrollers.isValidName(name)) {
					player.sendSystemMessage(Component.translatable("commands.bcya.patrol.bad_name", name));
					return;
				}
				if (PatrolRecorder.current(player) != null) {
					player.sendSystemMessage(Component.translatable("commands.bcya.patrol.record_busy",
						PatrolRecorder.current(player).name()));
					return;
				}
				PatrolRecorder.start(player, name);
			}
		}
	}

	private static void remove(ServerPlayer player, PatrollerEntity patroller) {
		String name = patroller.getPatrolName();
		patroller.discard();
		player.sendSystemMessage(Component.translatable("commands.bcya.patrol.removed", name));
	}
}
