package cn.erindax.bcya.command;

import cn.erindax.bcya.item.MaskItem;
import cn.erindax.bcya.mask.MaskOriginState;
import cn.erindax.bcya.mask.MaskOrigins;
import cn.erindax.bcya.util.MaskUtil;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class MaskOriginCommands {

	private static final String ARG_TARGETS = "targets";
	private static final String ARG_MASK = "mask";

	private MaskOriginCommands() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> origin() {
		return Commands.literal("origin")
			.requires(source -> source.hasPermission(2))
			.then(Commands.literal("set")
				.then(Commands.argument(ARG_TARGETS, EntityArgument.players())
					.executes(ctx -> setWorn(ctx.getSource(), EntityArgument.getPlayers(ctx, ARG_TARGETS)))
					.then(Commands.argument(ARG_MASK, ResourceLocationArgument.id())
						.suggests(BcyaCommands::suggestMasks)
						.executes(ctx -> setMask(ctx.getSource(), EntityArgument.getPlayers(ctx, ARG_TARGETS),
							ResourceLocationArgument.getId(ctx, ARG_MASK))))))
			.then(Commands.literal("clear")
				.then(Commands.argument(ARG_TARGETS, GameProfileArgument.gameProfile())
					.executes(ctx -> clear(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, ARG_TARGETS)))))
			.then(Commands.literal("list")
				.executes(ctx -> list(ctx.getSource())));
	}

	private static int setWorn(CommandSourceStack source, Collection<ServerPlayer> players) {
		MaskOriginState state = MaskOriginState.get(source.getServer());
		List<ServerPlayer> recorded = new ArrayList<>();
		MaskItem last = null;
		for (ServerPlayer player : players) {
			MaskItem mask = MaskUtil.getWornMask(player);
			if (mask == null) {
				source.sendFailure(Component.translatable("commands.bcya.origin.no_mask", name(player)));
				continue;
			}
			state.set(player.getUUID(), player.getGameProfile().getName(), BuiltInRegistries.ITEM.getKey(mask));
			recorded.add(player);
			last = mask;
		}
		report(source, recorded, last);
		return recorded.size();
	}

	private static int setMask(CommandSourceStack source, Collection<ServerPlayer> players, ResourceLocation input) {
		Item item = BcyaCommands.resolveMask(input);
		if (!(item instanceof MaskItem mask)) {
			source.sendFailure(Component.translatable("commands.bcya.unknown_mask", input.toString()));
			return 0;
		}
		MaskOriginState state = MaskOriginState.get(source.getServer());
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(mask);
		for (ServerPlayer player : players) {
			state.set(player.getUUID(), player.getGameProfile().getName(), id);
		}
		report(source, List.copyOf(players), mask);
		return players.size();
	}

	private static void report(CommandSourceStack source, List<ServerPlayer> recorded, MaskItem last) {
		if (recorded.size() == 1) {
			Component player = name(recorded.get(0));
			Component mask = new ItemStack(last).getHoverName();
			source.sendSuccess(() -> Component.translatable("commands.bcya.origin.set", player, mask), true);
		} else if (!recorded.isEmpty()) {
			int count = recorded.size();
			source.sendSuccess(() -> Component.translatable("commands.bcya.origin.set_many", count), true);
		}
	}

	private static int clear(CommandSourceStack source, Collection<GameProfile> profiles) {
		MaskOriginState state = MaskOriginState.get(source.getServer());
		int removed = 0;
		for (GameProfile profile : profiles) {
			if (state.remove(profile.getId())) {
				removed++;
			}
		}
		if (removed == 0) {
			source.sendFailure(Component.translatable("commands.bcya.origin.clear_none"));
			return 0;
		}
		int count = removed;
		source.sendSuccess(() -> Component.translatable("commands.bcya.origin.cleared", count), true);
		return removed;
	}

	private static int list(CommandSourceStack source) {
		List<MaskOriginState.Origin> origins = new ArrayList<>(MaskOriginState.get(source.getServer()).all().values());
		if (origins.isEmpty()) {
			source.sendSuccess(() -> Component.translatable("commands.bcya.origin.list_empty"), false);
			return 0;
		}
		origins.sort(Comparator.comparing(MaskOriginState.Origin::name, String.CASE_INSENSITIVE_ORDER));
		int count = origins.size();
		source.sendSuccess(() -> Component.translatable("commands.bcya.origin.list_header", count), false);
		for (MaskOriginState.Origin origin : origins) {
			MaskItem mask = MaskOrigins.resolve(origin.mask());
			Component maskName = mask == null ? Component.literal(origin.mask().toString())
				: new ItemStack(mask).getHoverName();
			source.sendSuccess(() -> Component.translatable("commands.bcya.origin.list_entry", origin.name(), maskName),
				false);
		}
		return count;
	}

	private static Component name(ServerPlayer player) {
		return Component.literal(player.getGameProfile().getName());
	}
}
