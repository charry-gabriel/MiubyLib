package fr.miuby.lib.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * Base générique pour les arguments Brigadier qui convertissent un mot (String) en valeur typée.
 *
 * <p>Élimine le boilerplate répétitif : {@code getNativeType()} est fixé à
 * {@link StringArgumentType#word()}, et {@code listSuggestions} filtre automatiquement
 * par préfixe la collection renvoyée par {@link #suggestions()}.</p>
 *
 * <h3>Usage minimal</h3>
 * <pre>{@code
 * public class RoleArgument extends MLStringArgument<Role> {
 *
 *     public static RoleArgument role() { return new RoleArgument(); }
 *
 *     @Override
 *     public Role convert(String value) throws CommandSyntaxException {
 *         Role role = GameManager.getInstance().getRoleLoader().getRole(value);
 *         if (role == null) throw CommandErrors.ROLE_NOT_FOUND.create(value);
 *         return role;
 *     }
 *
 *     @Override
 *     protected Collection<String> suggestions() {
 *         return GameManager.getInstance().getRoleLoader().getRoles()
 *             .stream().map(r -> r.type().toString()).toList();
 *     }
 *
 *     public static Role getRole(CommandContext<?> ctx, String name) {
 *         return ctx.getArgument(name, Role.class);
 *     }
 * }
 * }</pre>
 *
 * <h3>Auto-complétion contextuelle</h3>
 * Si les suggestions dépendent du contexte de la commande (joueur, autres arguments…),
 * surchargez directement {@link #listSuggestions} plutôt que {@link #suggestions} :
 * <pre>{@code
 * @Override
 * public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(
 *         @NonNull CommandContext<S> ctx, SuggestionsBuilder builder) {
 *     String remaining = builder.getRemaining().toLowerCase();
 *     computeSuggestionsFromContext(ctx).stream()
 *         .filter(s -> s.toLowerCase().startsWith(remaining))
 *         .forEach(builder::suggest);
 *     return builder.buildFuture();
 * }
 * }</pre>
 *
 * @param <T> le type retourné par {@link #convert(String)}
 */
public abstract class MLStringArgument<T> implements CustomArgumentType.Converted<T, String> {

    /**
     * Retourne la collection de noms à proposer à l'auto-complétion.
     *
     * <p>Appelé à chaque frappe ; peut accéder à l'état courant du jeu.
     * L'implémentation par défaut retourne une collection vide — à surcharger,
     * ou surcharger directement {@link #listSuggestions} pour une logique contextuelle.</p>
     */
    protected Collection<String> suggestions() {
        return Collections.emptyList();
    }

    /** Toujours {@link StringArgumentType#word()} pour ce type d'argument. */
    @Override
    public final @NonNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    /**
     * Filtre {@link #suggestions()} par le préfixe déjà tapé (insensible à la casse).
     * Surchargez cette méthode si la liste de suggestions dépend du contexte.
     */
    @Override
    public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(
            @NonNull CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        suggestions().stream()
                .filter(name -> name.toLowerCase().startsWith(remaining))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }
}
