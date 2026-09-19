package cn.erindax.bcya.check;

import cn.erindax.bcya.BcyaMod;
import cn.erindax.bcya.card.CardData;
import cn.erindax.bcya.check.net.CheckPromptPayload;
import cn.erindax.bcya.check.net.CheckResultPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

public final class CheckManager {

	public static final String KEY_BROADCAST = "commands.bcya.check.broadcast";
	public static final String KEY_BROADCAST_BLOCKED = "commands.bcya.check.broadcast.blocked";
	public static final String KEY_SUCCESS = "screen.bcya.check.outcome.success";
	public static final String KEY_FAIL = "screen.bcya.check.outcome.fail";
	public static final String KEY_KP_SENT = "commands.bcya.check.kp.sent";

	private static final Map<UUID, Pending> PENDING = new HashMap<>();
	private static int nextId = 1;

	private CheckManager() {
	}

	private record Pending(int requestId, int difficulty, ServerPlayer kp, String kpName,
			String targetName, CheckService.Info info) {
	}

	public static CheckService.Info inspect(ServerPlayer player, String skill) {
		ItemStack card = CardData.findCard(player);
		if (card.isEmpty()) {
			return null;
		}
		return CheckService.inspect(CardData.read(card), skill);
	}

	public static CheckService.Info selfCheck(ServerPlayer player, String skill, int difficulty) {
		CheckService.Info info = inspect(player, skill);
		if (info == null) {
			return null;
		}
		CheckService.Outcome outcome = CheckService.roll(info, difficulty, player.serverLevel().getRandom());
		CompoundTag data = outcome.toTag();
		data.putInt("request_id", nextId++);
		data.putString("kp", "");
		data.putString("roller", playerName(player));
		ServerPlayNetworking.send(player, new CheckResultPayload(data));
		broadcast(player.server, data);
		record(player.server, data);
		return info;
	}

	public static boolean beginKpCheck(@Nullable ServerPlayer kp, ServerPlayer target, String skill, int difficulty) {
		CheckService.Info info = inspect(target, skill);
		if (info == null) {
			return false;
		}
		int id = nextId++;
		String kpName = kp != null ? playerName(kp) : "Server";
		PENDING.put(target.getUUID(),
			new Pending(id, difficulty, kp, kpName, playerName(target), info));
		CompoundTag data = info.toTag();
		data.putInt("request_id", id);
		data.putInt("difficulty", difficulty);
		data.putString("kp", kpName);
		data.putString("roller", playerName(target));
		ServerPlayNetworking.send(target, new CheckPromptPayload(data));
		if (kp != null) {
			kp.displayClientMessage(Component.translatable(KEY_KP_SENT, targetName(target), skill, difficulty), true);
		}
		return true;
	}

	public static void onRoll(ServerPlayer target, int requestId) {
		UUID id = target.getUUID();
		Pending pending = PENDING.get(id);
		if (pending == null || pending.requestId() != requestId) {
			return;
		}
		PENDING.remove(id);

		CheckService.Outcome outcome = CheckService.roll(
			pending.info(), pending.difficulty(), target.serverLevel().getRandom());
		CompoundTag data = outcome.toTag();
		data.putInt("request_id", requestId);
		data.putString("kp", pending.kpName());
		data.putString("roller", pending.targetName());

		ServerPlayNetworking.send(target, new CheckResultPayload(data));
		ServerPlayer kp = pending.kp();
		if (kp != null && kp != target && !kp.isRemoved()) {
			ServerPlayNetworking.send(kp, new CheckResultPayload(data));
		}
		broadcast(target.server, data);
		record(target.server, data);
	}

	public static int[] freeRoll(RandomSource random, int count, int sides) {
		int[] dice = new int[count];
		for (int i = 0; i < count; i++) {
			dice[i] = random.nextInt(sides) + 1;
		}
		return dice;
	}

	public static void record(MinecraftServer server, CompoundTag data) {
		CompoundTag entry = data.copy();
		entry.putLong("time", System.currentTimeMillis());
		CheckLog.get(server).append(entry);
		server.getCommandStorage().set(BcyaMod.id("check"), data.copy());
	}

	private static void broadcast(MinecraftServer server, CompoundTag data) {
		boolean blocked = data.getBoolean("blocked");
		String roller = data.getString("roller");
		String skill = data.getString("skill");
		Component message;
		if (blocked) {
			message = Component.translatable(KEY_BROADCAST_BLOCKED, roller, skill);
		} else {
			int difficulty = data.getInt("difficulty");
			int pool = data.getInt("pool");
			int successes = data.getInt("successes");
			message = Component.translatable(KEY_BROADCAST, roller, skill, difficulty, pool,
				diceString(data.getIntArray("dice")), successes,
				Component.translatable(outcomeKey(data)));
		}
		server.getPlayerList().broadcastSystemMessage(message, false);
	}

	public static String outcomeKey(CompoundTag data) {
		String outcome = data.getString("outcome");
		if (!outcome.isEmpty()) {
			return "screen.bcya.check.outcome." + outcome;
		}
		return data.getBoolean("success") ? KEY_SUCCESS : KEY_FAIL;
	}

	public static String diceString(int[] dice) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < dice.length; i++) {
			if (i > 0) {
				builder.append(", ");
			}
			builder.append(dice[i]);
		}
		return builder.toString();
	}

	public static String playerName(ServerPlayer player) {
		String name = player.getGameProfile().getName();
		return name == null ? player.getName().getString() : name;
	}

	public static String targetName(ServerPlayer player) {
		return playerName(player);
	}
}
