package net.nightshade.divinity_engine.commands.arg;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.IForgeRegistry;
import net.nightshade.divinity_engine.divinity.gods.BaseGod;
import net.nightshade.divinity_engine.registry.divinity.gods.GodsRegistry;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GodsArgument implements ArgumentType<BaseGod> {
    private static final DynamicCommandExceptionType ERROR_INVALID_VALUE = new DynamicCommandExceptionType((o) -> Component.translatable("tbate.argument.clan.invalid"));

    public GodsArgument() {
    }

    public BaseGod parse(StringReader reader) throws CommandSyntaxException {
        String remaining = reader.getRemaining();
        String registryName = remaining.contains(" ") ? remaining.split(" ")[0] : remaining;
        reader.setCursor(reader.getString().indexOf(registryName) + registryName.length());
        return (BaseGod)((IForgeRegistry) GodsRegistry.GODS_REGISTRY.get()).getValues().stream().filter((clan) -> ((IForgeRegistry) GodsRegistry.GODS_REGISTRY.get()).getKey(clan).toString().equalsIgnoreCase(registryName)).findFirst().orElseGet(() -> ERROR_INVALID_VALUE.create(registryName));
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return context.getSource() instanceof SharedSuggestionProvider ? SharedSuggestionProvider.suggestResource(((IForgeRegistry) GodsRegistry.GODS_REGISTRY.get()).getValues().stream().map((Clan) -> ((IForgeRegistry) GodsRegistry.GODS_REGISTRY.get()).getKey(Clan)), builder) : Suggestions.empty();
    }

    public static GodsArgument god() {
        return new GodsArgument();
    }

    public static BaseGod getGod(CommandContext<CommandSourceStack> context, String name) {
        return (BaseGod)context.getArgument(name, BaseGod.class);
    }

    public Collection<String> getExamples() {
        return (Collection) Stream.of(GodsRegistry.SOLARIUS.getId(), GodsRegistry.SOLARIUS.getId()).map(ResourceLocation::toString).collect(Collectors.toList());
    }

}
