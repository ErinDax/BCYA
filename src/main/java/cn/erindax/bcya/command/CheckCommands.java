package cn.erindax.bcya.command;

import cn.erindax.bcya.check.CheckLog;
import cn.erindax.bcya.check.CheckManager;
import cn.erindax.bcya.check.CheckService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class CheckCommands {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private CheckCommands() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> check() {
		return Commands.literal("check")
			.then(Commands.argument("skill", SkillArgumentType.skill())
				.executes(CheckCommands::selfCheck)
				.then(Commands.argument("difficulty", DifficultyArgumentType.difficulty())
					.executes(CheckCommands::selfCheck)))
			.then(Commands.argument("player", EntityArgument.player())
				.requires(source -> source.hasPermission(2))
				.then(Commands.argument("skill", SkillArgumentType.skill())
					.executes(CheckCommands::kpCheck)
					.then(Commands.argument("difficulty", DifficultyArgumentType.difficulty())
						.executes(CheckCommands::kpCheck))));
	}

	public static LiteralArgumentBuilder<CommandSourceStack> kp() {
		return Commands.literal("kp")
			.requires(source -> source.hasPermission(2))
			.then(Commands.argument("player", EntityArgument.player())
				.then(Commands.argument("skill", SkillArgumentType.skill())
					.executes(CheckCommands::kpCheck)
					.then(Commands.argument("difficulty", DifficultyArgumentType.difficulty())
						.executes(CheckCommands::kpCheck))));
	}

	public static LiteralArgumentBuilder<CommandSourceStack> roll() {
		return Commands.literal("roll")
			.then(Commands.argument("dice", StringArgumentType.word())
				.executes(CheckCommands::freeRoll));
	}

	public static LiteralArgumentBuilder<CommandSourceStack> log() {
		return Commands.literal("log")
			.executes(CheckCommands::viewLog)
			.then(Commands.literal("export")
				.requires(source -> source.hasPermission(2))
				.executes(CheckCommands::exportLog));
	}

	private static int selfCheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String skill = SkillArgumentType.getSkill(context, "skill");
		int difficulty = DifficultyArgumentType.getOrDefault(context, "difficulty", 2);
		CheckService.Info info = CheckManager.selfCheck(player, skill, difficulty);
		if (info == null) {
			source.sendFailure(Component.translatable("commands.bcya.check.no_card"));
			return 0;
		}
		return 1;
	}

	private static int kpCheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer kp = source.getPlayer();
		ServerPlayer target = EntityArgument.getPlayer(context, "player");
		String skill = SkillArgumentType.getSkill(context, "skill");
		int difficulty = DifficultyArgumentType.getOrDefault(context, "difficulty", 2);
		if (!CheckManager.beginKpCheck(kp, target, skill, difficulty)) {
			source.sendFailure(Component.translatable("commands.bcya.check.no_card"));
			return 0;
		}
		if (kp == null) {
			source.sendSuccess(() -> Component.translatable(
				"commands.bcya.check.kp.sent", target.getName(), skill, difficulty), true);
		}
		return 1;
	}

	private static int freeRoll(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		String expression = StringArgumentType.getString(context, "dice").toLowerCase(Locale.ROOT).replace('ｄ', 'd');
		int count;
		int sides;
		try {
			int index = expression.indexOf('d');
			if (index < 0) {
				throw new NumberFormatException();
			}
			count = index == 0 ? 1 : Integer.parseInt(expression.substring(0, index));
			sides = Integer.parseInt(expression.substring(index + 1));
		} catch (NumberFormatException exception) {
			source.sendFailure(Component.translatable("commands.bcya.roll.invalid"));
			return 0;
		}
		if (count < 1 || count > 100 || sides < 2 || sides > 1000) {
			source.sendFailure(Component.translatable("commands.bcya.roll.invalid"));
			return 0;
		}
		ServerPlayer player = source.getPlayer();
		int[] dice = CheckManager.freeRoll(
			player != null ? player.serverLevel().getRandom() : source.getServer().overworld().getRandom(),
			count, sides);
		int sum = 0;
		for (int value : dice) {
			sum += value;
		}
		final int total = sum;
		String label = count + "d" + sides;
		source.sendSuccess(() -> Component.translatable(
			"commands.bcya.roll.result", label, CheckManager.diceString(dice), total), false);

		CompoundTag entry = new CompoundTag();
		entry.putString("skill", label);
		entry.putInt("level", 0);
		entry.putInt("check_value", 0);
		entry.putInt("modifier", 0);
		entry.putInt("pool", count);
		entry.putBoolean("blocked", false);
		entry.putBoolean("unskilled", false);
		entry.putInt("emotion", 0);
		entry.putInt("difficulty", 0);
		entry.putIntArray("dice", dice);
		entry.putInt("successes", total);
		entry.putBoolean("success", false);
		entry.putString("roller", player != null ? CheckManager.playerName(player) : "Server");
		entry.putString("kp", "");
		entry.putLong("time", System.currentTimeMillis());
		CheckLog.get(source.getServer()).append(entry);
		return count;
	}

	private static int viewLog(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		List<CompoundTag> entries = CheckLog.get(source.getServer()).entries();
		if (entries.isEmpty()) {
			source.sendSuccess(() -> Component.translatable("commands.bcya.log.empty"), false);
			return 0;
		}
		int show = Math.min(20, entries.size());
		source.sendSuccess(() -> Component.translatable("commands.bcya.log.header", show, entries.size()), false);
		for (int i = entries.size() - show; i < entries.size(); i++) {
			CompoundTag entry = entries.get(i);
			String time = new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(new Date(entry.getLong("time")));
			String difficulty = entry.getInt("difficulty") > 0 ? String.valueOf(entry.getInt("difficulty")) : "-";
			source.sendSuccess(() -> Component.translatable(
				"commands.bcya.log.entry", time, entry.getString("roller"),
				entry.getString("skill"), difficulty, entry.getInt("successes")), false);
		}
		return show;
	}

	private static int exportLog(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		MinecraftServer server = source.getServer();
		List<CompoundTag> entries = CheckLog.get(server).entries();
		Path dir = FabricLoader.getInstance().getConfigDir().resolve("bcya").resolve("check_export");
		try {
			Files.createDirectories(dir);
			String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(new Date());
			Path jsonPath = dir.resolve("checks_" + stamp + ".json");
			Path textPath = dir.resolve("checks_" + stamp + ".txt");
			writeLogJson(jsonPath, entries);
			writeLogText(textPath, entries);
			source.sendSuccess(() -> Component.translatable(
				"commands.bcya.log.export.done", entries.size(), dir.toString()), true);
		} catch (IOException exception) {
			source.sendFailure(Component.translatable(
				"commands.bcya.log.export.failed", exception.getMessage()));
			return 0;
		}
		return entries.size();
	}

	private static void writeLogJson(Path path, List<CompoundTag> entries) throws IOException {
		JsonArray array = new JsonArray();
		for (CompoundTag entry : entries) {
			JsonObject object = new JsonObject();
			object.addProperty("time", entry.getLong("time"));
			object.addProperty("roller", entry.getString("roller"));
			object.addProperty("kp", entry.getString("kp"));
			object.addProperty("skill", entry.getString("skill"));
			object.addProperty("difficulty", entry.getInt("difficulty"));
			object.addProperty("level", entry.getInt("level"));
			object.addProperty("check_value", entry.getInt("check_value"));
			object.addProperty("pool", entry.getInt("pool"));
			object.addProperty("successes", entry.getInt("successes"));
			object.addProperty("success", entry.getBoolean("success"));
			object.addProperty("outcome", entry.getString("outcome"));
			object.addProperty("blocked", entry.getBoolean("blocked"));
			object.addProperty("dice", CheckManager.diceString(entry.getIntArray("dice")));
			array.add(object);
		}
		try (Writer writer = Files.newBufferedWriter(path)) {
			GSON.toJson(array, writer);
		}
	}

	private static void writeLogText(Path path, List<CompoundTag> entries) throws IOException {
		StringBuilder builder = new StringBuilder();
		builder.append("调查员检定历史导出\n");
		builder.append("记录总数：").append(entries.size()).append("\n\n");
		for (CompoundTag entry : entries) {
			String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
				.format(new Date(entry.getLong("time")));
			builder.append('[').append(time).append("] ")
				.append(entry.getString("roller")).append("「").append(entry.getString("skill")).append("」");
			int difficulty = entry.getInt("difficulty");
			if (difficulty > 0) {
				builder.append(" 难度 ").append(difficulty);
			}
			builder.append(" 骰子：").append(CheckManager.diceString(entry.getIntArray("dice")))
				.append(" 成功数：").append(entry.getInt("successes"));
			if (difficulty > 0) {
				String outcome = entry.getString("outcome");
				if (outcome.isEmpty()) {
					builder.append(entry.getBoolean("success") ? " 成功" : " 失败");
				} else if ("critical_success".equals(outcome)) {
					builder.append(" 大成功");
				} else if ("critical_fail".equals(outcome)) {
					builder.append(" 大失败");
				} else if ("success".equals(outcome)) {
					builder.append(" 成功");
				} else {
					builder.append(" 失败");
				}
			}
			builder.append('\n');
		}
		Files.writeString(path, builder.toString());
	}
}
