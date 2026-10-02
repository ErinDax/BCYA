package cn.erindax.bcya.command;

import cn.erindax.bcya.BcyaMod;
import cn.erindax.bcya.item.ModItems;
import cn.erindax.bcya.manage.WandWhitelist;
import cn.erindax.bcya.mask.MaskRulesState;
import cn.erindax.bcya.mask.net.MaskRulesHandler;
import cn.erindax.bcya.skin.TextureStore;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

public final class BcyaCommands {

	private static final String ROOT = "BCYA";
	private static final String MASKER_VBL = "Masker_vbl";
	private static final String MASKER_HBL = "Masker_hbl";
	private static final String RELOAD = "reload";
	private static final String ARG_MASK = "mask";

	private BcyaCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(root(ROOT));
			dispatcher.register(root("bcya"));
		});
	}

	private static LiteralArgumentBuilder<CommandSourceStack> root(String name) {
		return Commands.literal(name)
			.then(maskRuleToggle(MASKER_VBL, MaskRulesState.Rule.VOICE_DISABLED, "commands.bcya.masker_vbl")
				.requires(source -> source.hasPermission(2)))
			.then(maskRuleToggle(MASKER_HBL, MaskRulesState.Rule.SWAP_BLACKLIST, "commands.bcya.masker_hbl")
				.requires(source -> source.hasPermission(2)))
			.then(MaskOriginCommands.origin())
			.then(WandCommands.wand())
			.then(PatrolCommands.build().requires(source -> source.hasPermission(2)))
			.then(InvestigatorCommands.build().requires(source -> source.hasPermission(2)))
			.then(Commands.literal(RELOAD)
				.requires(source -> source.hasPermission(2))
				.executes(ctx -> reload(ctx.getSource())))
			.then(btrpg())
			.then(CheckCommands.check())
			.then(CheckCommands.kp())
			.then(CheckCommands.roll())
			.then(CheckCommands.log())
			.then(DanceCommands.dance())
			.then(SkywalkCommands.skywalk());
	}

	private static LiteralArgumentBuilder<CommandSourceStack> btrpg() {
		return InvestigatorCommands.attachArchive(Commands.literal("btrpg"))
			.then(CheckCommands.check())
			.then(CheckCommands.kp())
			.then(CheckCommands.roll())
			.then(CheckCommands.log());
	}

	private static int reload(CommandSourceStack source) {
		if (!WandWhitelist.reload()) {
			source.sendFailure(Component.translatable("commands.bcya.reload.failed", WandWhitelist.file().toString()));
		} else {
			int count = WandWhitelist.size();
			source.sendSuccess(() -> Component.translatable("commands.bcya.reload.done", count), true);
		}
		List<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
		int keys = reloadTextures(source, TextureStore.KEYS, players);
		int skins = reloadTextures(source, TextureStore.SKINS, players);
		source.sendSuccess(() -> Component.translatable("commands.bcya.reload.textures", keys, skins), true);
		return 1;
	}

	private static int reloadTextures(CommandSourceStack source, TextureStore store, List<ServerPlayer> players) {
		store.refresh();
		TextureStore.Scan scan = store.scan();
		for (String name : scan.names()) {
			store.sendToAll(players, name);
		}
		for (TextureStore.Problem problem : scan.problems()) {
			Component reason = problem.reason().equals("size")
				? Component.translatable("commands.bcya.reload.texture." + store.kind() + "_size", problem.width(),
					problem.height())
				: Component.translatable("commands.bcya.reload.texture." + problem.reason(), TextureStore.MAX_NAME);
			source.sendFailure(Component.translatable("commands.bcya.reload.texture.skipped",
				store.directory().getFileName() + "/" + problem.file(), reason));
		}
		return scan.names().size();
	}

	private static LiteralArgumentBuilder<CommandSourceStack> maskRuleToggle(String literal,
			MaskRulesState.Rule rule, String keyPrefix) {
		return Commands.literal(literal)
			.then(Commands.argument(ARG_MASK, ResourceLocationArgument.id())
				.suggests(BcyaCommands::suggestMasks)
				.executes(ctx -> toggleRule(ctx.getSource(), rule, keyPrefix,
					ResourceLocationArgument.getId(ctx, ARG_MASK))));
	}

	static CompletableFuture<Suggestions> suggestMasks(CommandContext<CommandSourceStack> ctx,
			SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggestResource(
			ModItems.MASKS.stream().map(BuiltInRegistries.ITEM::getKey), builder);
	}

	private static int toggleRule(CommandSourceStack source, MaskRulesState.Rule rule, String keyPrefix,
			ResourceLocation input) {
		Item mask = resolveMask(input);
		if (mask == null) {
			source.sendFailure(Component.translatable("commands.bcya.unknown_mask", input.toString()));
			return 0;
		}

		MinecraftServer server = source.getServer();
		MaskRulesState state = MaskRulesState.get(server);
		boolean active = state.toggle(rule, BuiltInRegistries.ITEM.getKey(mask));

		if (rule == MaskRulesState.Rule.VOICE_DISABLED) {
			MaskRulesHandler.syncVoice(server);
		}

		Component maskName = new ItemStack(mask).getHoverName();
		String key = keyPrefix + (active ? ".on" : ".off");
		source.sendSuccess(() -> Component.translatable(key, maskName), true);
		return 1;
	}

	@Nullable
	static Item resolveMask(ResourceLocation input) {
		ResourceLocation id = ResourceLocation.DEFAULT_NAMESPACE.equals(input.getNamespace())
			? BcyaMod.id(input.getPath())
			: input;
		for (Item mask : ModItems.MASKS) {
			if (BuiltInRegistries.ITEM.getKey(mask).equals(id)) {
				return mask;
			}
		}
		return null;
	}
}
