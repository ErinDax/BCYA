package cn.erindax.bcya.command;

import cn.erindax.bcya.skywalk.Skywalk;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class SkywalkCommands {

	private static final String ARG_DEPTH = "depth";

	private SkywalkCommands() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> skywalk() {
		return Commands.literal("skywalk")
			.requires(source -> source.hasPermission(2))
			.then(Commands.literal("true").executes(context -> toggle(context.getSource(), true)))
			.then(Commands.literal("false").executes(context -> toggle(context.getSource(), false)))
			.then(Commands.literal("depth")
				.then(Commands.argument(ARG_DEPTH, IntegerArgumentType.integer(Skywalk.MIN_DEPTH, Skywalk.MAX_DEPTH))
					.executes(context -> depth(context.getSource(),
						IntegerArgumentType.getInteger(context, ARG_DEPTH)))));
	}

	private static int toggle(CommandSourceStack source, boolean enabled) {
		Skywalk.setEnabled(source.getServer(), enabled);
		source.sendSuccess(() -> Component.translatable(
			enabled ? "commands.bcya.skywalk.on" : "commands.bcya.skywalk.off"), true);
		return 1;
	}

	private static int depth(CommandSourceStack source, int depth) {
		Skywalk.setDepth(source.getServer(), depth);
		source.sendSuccess(() -> Component.translatable("commands.bcya.skywalk.depth", depth), true);
		return depth;
	}
}
