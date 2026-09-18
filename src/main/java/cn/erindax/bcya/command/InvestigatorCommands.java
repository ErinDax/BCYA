package cn.erindax.bcya.command;

import cn.erindax.bcya.card.CardArchive;
import cn.erindax.bcya.card.CardData;
import cn.erindax.bcya.card.CardHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class InvestigatorCommands {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private InvestigatorCommands() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("investigator")
			.then(Commands.literal("list").executes(InvestigatorCommands::list))
			.then(Commands.literal("export").executes(InvestigatorCommands::export))
			.then(Commands.literal("emotion")
				.then(Commands.argument("player", EntityArgument.player())
					.then(Commands.argument("value", IntegerArgumentType.integer(0, CardData.EMOTION_MAX))
						.executes(InvestigatorCommands::setEmotion))));
	}

	private static int setEmotion(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer target = EntityArgument.getPlayer(context, "player");
		int value = IntegerArgumentType.getInteger(context, "value");
		ItemStack stack = CardData.findCard(target);
		if (stack.isEmpty()) {
			source.sendFailure(Component.translatable("commands.bcya.investigator.emotion.no_card", target.getName()));
			return 0;
		}
		CompoundTag card = CardData.read(stack);
		CardData.setEmotion(card, value);
		CardHandler.commitCard(target, stack, card);
		source.sendSuccess(() -> Component.translatable(
			"commands.bcya.investigator.emotion.done", target.getName(), CardData.emotion(card)), true);
		return CardData.emotion(card);
	}

	private static int list(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		Collection<CompoundTag> cards = CardArchive.get(source.getServer()).all();
		if (cards.isEmpty()) {
			source.sendSuccess(() -> Component.translatable("commands.bcya.investigator.list.empty"), false);
			return 0;
		}
		source.sendSuccess(() -> Component.translatable("commands.bcya.investigator.list.header", cards.size()), false);
		for (CompoundTag card : cards) {
			String investigator = card.getString(CardData.INVESTIGATOR);
			String owner = card.getString(CardData.OWNER_NAME);
			source.sendSuccess(() -> Component.translatable("commands.bcya.investigator.list.entry", investigator, owner),
				false);
		}
		return cards.size();
	}

	private static int export(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		Collection<CompoundTag> cards = CardArchive.get(source.getServer()).all();
		Path dir = FabricLoader.getInstance().getConfigDir().resolve("bcya").resolve("investigator_export");
		try {
			Files.createDirectories(dir);
			String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(new Date());
			Path jsonPath = dir.resolve("cards_" + stamp + ".json");
			Path textPath = dir.resolve("cards_" + stamp + ".txt");
			writeJson(jsonPath, cards);
			writeText(textPath, cards);
			source.sendSuccess(() -> Component.translatable("commands.bcya.investigator.export.done", cards.size(),
				dir.toString()), true);
		} catch (IOException exception) {
			source.sendFailure(Component.translatable("commands.bcya.investigator.export.failed", exception.getMessage()));
			return 0;
		}
		return cards.size();
	}

	private static void writeJson(Path path, Collection<CompoundTag> cards) throws IOException {
		JsonArray array = new JsonArray();
		for (CompoundTag card : cards) {
			JsonObject object = new JsonObject();
			object.addProperty("investigator", card.getString(CardData.INVESTIGATOR));
			object.addProperty("player", card.getString(CardData.OWNER_NAME));
			object.addProperty("emotion", CardData.emotion(card));
			object.addProperty("emotion_deep", card.getString(CardData.EMOTION_DEEP));
			object.addProperty("awakened", card.getBoolean(CardData.AWAKENED));
			object.addProperty("ability_name", card.getString(CardData.ABILITY_NAME));
			object.addProperty("hp", CardData.hp(card));
			object.addProperty("mp", CardData.mp(card));

			JsonObject abilities = new JsonObject();
			for (String name : CardData.ABILITY_NAMES) {
				abilities.addProperty(name, CardData.ability(card, name));
			}
			object.add("abilities", abilities);

			JsonObject skills = new JsonObject();
			for (Map.Entry<String, List<String>> entry : CardData.SKILL_CATEGORIES.entrySet()) {
				JsonObject category = new JsonObject();
				for (String name : entry.getValue()) {
					category.addProperty(name, CardData.skill(card, name));
				}
				skills.add(entry.getKey(), category);
			}
			object.add("skills", skills);
			array.add(object);
		}
		try (Writer writer = Files.newBufferedWriter(path)) {
			GSON.toJson(array, writer);
		}
	}

	private static void writeText(Path path, Collection<CompoundTag> cards) throws IOException {
		StringBuilder builder = new StringBuilder();
		builder.append("调查员档案导出\n");
		builder.append("档案总数：").append(cards.size()).append("\n\n");
		for (CompoundTag card : cards) {
			builder.append("调查员：").append(card.getString(CardData.INVESTIGATOR))
				.append(" / 玩家：").append(card.getString(CardData.OWNER_NAME)).append('\n');
			builder.append("HP：").append(CardData.hp(card))
				.append("  MP：").append(CardData.mp(card)).append('\n');
			builder.append("情绪值：").append(CardData.emotion(card))
				.append("  情绪最深处：").append(card.getString(CardData.EMOTION_DEEP)).append('\n');
			builder.append("觉醒：").append(card.getBoolean(CardData.AWAKENED) ? "是" : "否");
			if (card.getBoolean(CardData.AWAKENED)) {
				builder.append("  能力：").append(card.getString(CardData.ABILITY_NAME));
			}
			builder.append('\n');
			builder.append("能力值：");
			for (String name : CardData.ABILITY_NAMES) {
				builder.append(name).append(' ').append(CardData.ability(card, name)).append("  ");
			}
			builder.append('\n');
			for (Map.Entry<String, List<String>> entry : CardData.SKILL_CATEGORIES.entrySet()) {
				builder.append(entry.getKey()).append("：");
				for (String name : entry.getValue()) {
					builder.append(name).append(' ').append(CardData.skill(card, name)).append("  ");
				}
				builder.append('\n');
			}
			builder.append("----------------------------------------\n");
		}
		Files.writeString(path, builder.toString());
	}
}
