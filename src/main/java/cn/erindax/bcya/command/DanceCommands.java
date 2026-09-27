package cn.erindax.bcya.command;

import cn.erindax.bcya.dance.DanceChart;
import cn.erindax.bcya.dance.DanceCharts;
import cn.erindax.bcya.dance.DanceManager;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public final class DanceCommands {

	private static final String ARG_SONG = "song";

	private DanceCommands() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> dance() {
		return Commands.literal("dance")
			.requires(source -> source.hasPermission(2))
			.then(Commands.literal("true").executes(context -> toggle(context.getSource(), true)))
			.then(Commands.literal("false").executes(context -> toggle(context.getSource(), false)))
			.then(Commands.literal("start")
				.then(Commands.argument(ARG_SONG, StringArgumentType.greedyString())
					.suggests(DanceCommands::suggestSongs)
					.executes(context -> start(context.getSource(),
						StringArgumentType.getString(context, ARG_SONG).strip()))))
			.then(Commands.literal("stop").executes(context -> stop(context.getSource())))
			.then(Commands.literal("reload").executes(context -> reload(context.getSource())));
	}

	private static CompletableFuture<Suggestions> suggestSongs(CommandContext<CommandSourceStack> context,
			SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(DanceCharts.names(), builder);
	}

	private static int toggle(CommandSourceStack source, boolean enabled) {
		MinecraftServer server = source.getServer();
		boolean stopped = DanceManager.setEnabled(server, enabled);
		source.sendSuccess(() -> Component.translatable(enabled ? "commands.bcya.dance.on" : "commands.bcya.dance.off"),
			true);
		if (stopped) {
			source.sendSuccess(() -> Component.translatable("commands.bcya.dance.stopped"), true);
		}
		return 1;
	}

	private static int start(CommandSourceStack source, String song) {
		DanceChart chart = DanceCharts.get(song);
		if (chart == null) {
			source.sendFailure(Component.translatable("commands.bcya.dance.unknown", song));
			return 0;
		}
		DanceManager.StartResult result = DanceManager.start(source.getServer(), chart);
		switch (result.status()) {
			case OK -> {
				source.sendSuccess(() -> Component.translatable("commands.bcya.dance.started", chart.title(),
					result.players()), true);
				return result.players();
			}
			case DISABLED -> source.sendFailure(Component.translatable("commands.bcya.dance.not_enabled"));
			case BUSY -> source.sendFailure(Component.translatable("commands.bcya.dance.busy"));
			case NO_AUDIO -> source.sendFailure(Component.translatable("commands.bcya.dance.no_audio", chart.audio()));
			case NOBODY -> source.sendFailure(Component.translatable("commands.bcya.dance.nobody"));
		}
		return 0;
	}

	private static int stop(CommandSourceStack source) {
		if (!DanceManager.stop(source.getServer())) {
			source.sendFailure(Component.translatable("commands.bcya.dance.idle"));
			return 0;
		}
		source.sendSuccess(() -> Component.translatable("commands.bcya.dance.stopped"), true);
		return 1;
	}

	private static int reload(CommandSourceStack source) {
		int count = DanceCharts.reload();
		source.sendSuccess(() -> Component.translatable("commands.bcya.dance.reloaded", count,
			DanceCharts.directory().toString()), true);
		if (!DanceCharts.failed().isEmpty()) {
			source.sendFailure(Component.translatable("commands.bcya.dance.reload_failed",
				String.join("、", DanceCharts.failed())));
		}
		return count;
	}

}
