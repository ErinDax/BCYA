package cn.erindax.bcya.command;

import cn.erindax.bcya.manage.WandWhitelist;
import cn.erindax.bcya.mask.MaskSwapper;
import cn.erindax.bcya.mask.net.MaskRulesHandler;
import cn.erindax.bcya.mask.net.MaskSkinHandler;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.function.Consumer;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class MaskCommands {

	private MaskCommands() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> mask() {
		return Commands.literal("mask")
			.requires(MaskCommands::canManage)
			.then(Commands.literal("rules").executes(ctx -> open(ctx.getSource(), MaskRulesHandler::openEditor)))
			.then(Commands.literal("voice").executes(ctx -> open(ctx.getSource(), MaskRulesHandler::openPresets)))
			.then(Commands.literal("skins").executes(ctx -> open(ctx.getSource(), MaskSkinHandler::openEditor)))
			.then(Commands.literal("swap").executes(ctx -> swap(ctx.getSource())));
	}

	private static boolean canManage(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		return player != null ? WandWhitelist.canManage(player) : source.hasPermission(2);
	}

	private static int open(CommandSourceStack source, Consumer<ServerPlayer> opener) throws CommandSyntaxException {
		opener.accept(source.getPlayerOrException());
		return 1;
	}

	private static int swap(CommandSourceStack source) {
		int swapped = MaskSwapper.swapAll(source.getServer(), source.getLevel().getRandom());
		if (swapped == 0) {
			source.sendFailure(Component.translatable("commands.bcya.mask.nothing"));
			return 0;
		}
		source.sendSuccess(() -> Component.translatable("commands.bcya.mask.swapped", swapped), true);
		return swapped;
	}
}
