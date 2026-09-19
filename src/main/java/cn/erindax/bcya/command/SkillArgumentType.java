package cn.erindax.bcya.command;

import cn.erindax.bcya.BcyaMod;
import cn.erindax.bcya.card.SkillTable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;

import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.network.chat.Component;

public final class SkillArgumentType implements ArgumentType<String> {

	private static final SkillArgumentType INSTANCE = new SkillArgumentType();
	private static final List<String> EXAMPLES = List.copyOf(SkillTable.ABILITY_OF.keySet());
	private static final SimpleCommandExceptionType ERROR = new SimpleCommandExceptionType(
		Component.translatable("commands.bcya.check.unknown_skill", ""));

	private SkillArgumentType() {
	}

	public static void register() {
		ArgumentTypeRegistry.registerArgumentType(
			BcyaMod.id("skill"), SkillArgumentType.class,
			SingletonArgumentInfo.contextFree(SkillArgumentType::skill));
	}

	public static SkillArgumentType skill() {
		return INSTANCE;
	}

	public static String getSkill(CommandContext<?> context, String name) {
		return context.getArgument(name, String.class);
	}

	@Override
	public String parse(StringReader reader) throws CommandSyntaxException {
		int start = reader.getCursor();
		while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
			reader.skip();
		}
		if (reader.getCursor() == start) {
			throw ERROR.createWithContext(reader);
		}
		String value = reader.getString().substring(start, reader.getCursor());
		if (!SkillTable.isKnown(value)) {
			throw new SimpleCommandExceptionType(
				Component.translatable("commands.bcya.check.unknown_skill", value)).createWithContext(reader);
		}
		return value;
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(SkillTable.ABILITY_OF.keySet(), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
