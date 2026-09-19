package cn.erindax.bcya.check;

import cn.erindax.bcya.card.CardHandler;
import cn.erindax.bcya.card.SkillTable;
import cn.erindax.bcya.check.net.CheckPromptPayload;
import cn.erindax.bcya.check.net.CheckResultPayload;
import cn.erindax.bcya.check.net.CheckRollPayload;
import cn.erindax.bcya.check.net.KpCheckPayload;

import java.util.UUID;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CheckHandler {

	private CheckHandler() {
	}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(CheckPromptPayload.TYPE, CheckPromptPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(CheckResultPayload.TYPE, CheckResultPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(CheckRollPayload.TYPE, CheckRollPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(KpCheckPayload.TYPE, KpCheckPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(CheckRollPayload.TYPE,
			(payload, context) -> CheckManager.onRoll(context.player(), payload.requestId()));
		ServerPlayNetworking.registerGlobalReceiver(KpCheckPayload.TYPE, CheckHandler::onKpCheck);
		CheckLog.prepare();
	}

	private static void onKpCheck(KpCheckPayload payload, ServerPlayNetworking.Context context) {
		ServerPlayer kp = context.player();
		if (!kp.hasPermissions(2)) {
			kp.displayClientMessage(Component.translatable(CardHandler.MSG_NO_PERMISSION), true);
			return;
		}
		if (!SkillTable.isKnown(payload.skill())) {
			return;
		}
		ServerPlayer target;
		try {
			target = kp.server.getPlayerList().getPlayer(UUID.fromString(payload.targetUuid()));
		} catch (IllegalArgumentException exception) {
			return;
		}
		if (target == null) {
			kp.displayClientMessage(Component.translatable("commands.bcya.check.target_offline"), true);
			return;
		}
		int difficulty = Math.max(1, Math.min(4, payload.difficulty()));
		if (!CheckManager.beginKpCheck(kp, target, payload.skill(), difficulty)) {
			kp.displayClientMessage(Component.translatable("commands.bcya.check.no_card"), true);
		}
	}
}
