package cn.erindax.bcya.command;

import cn.erindax.bcya.BcyaMod;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;

import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.network.chat.Component;

public final class DifficultyArgumentType implements ArgumentType<Integer> {

	private static final DifficultyArgumentType INSTANCE = new DifficultyArgumentType();
	private static final List<String> EXAMPLES = List.of("1", "2", "简单", "中等", "困难", "极难");
	private static final Map<String, Integer> NAMES = Map.of(
		"1", 1, "简单", 1,
		"2", 2, "中等", 2,
		"3", 3, "困难", 3,
		"4", 4, "极难", 4);
	private static final DynamicCommandExceptionType ERROR = new DynamicCommandExceptionType(
		value -> Component.translatable("commands.bcya.check.invalid_difficulty", value));

	private DifficultyArgumentType() {
	}

	public static void register() {
		ArgumentTypeRegistry.registerArgumentType(
			BcyaMod.id("difficulty"), DifficultyArgumentType.class,
			SingletonArgumentInfo.contextFree(DifficultyArgumentType::difficulty));
	}

	public static DifficultyArgumentType difficulty() {
		return INSTANCE;
	}

	public static int get(CommandContext<?> context, String name) {
		return context.getArgument(name, Integer.class);
	}

	public static int getOrDefault(CommandContext<?> context, String name, int fallback) {
		try {
			return get(context, name);
		} catch (IllegalArgumentException exception) {
			return fallback;
		}
	}

	@Override
	public Integer parse(StringReader reader) throws CommandSyntaxException {
		int start = reader.getCursor();
		while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
			reader.skip();
		}
		if (reader.getCursor() == start) {
			throw ERROR.createWithContext(reader, "");
		}
		String raw = reader.getString().substring(start, reader.getCursor());
		Integer value = NAMES.get(raw.toLowerCase(Locale.ROOT));
		if (value == null) {
			value = NAMES.get(raw);
		}
		if (value == null) {
			throw ERROR.createWithContext(reader, raw);
		}
		return value;
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(EXAMPLES, builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
