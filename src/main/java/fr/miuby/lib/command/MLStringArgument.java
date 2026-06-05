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
 * <p><b>Usage minimal</b></p>
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
 * <p><b>Auto-complétion contextuelle</b></p>
 * <p>Si les suggestions dépendent du contexte de la commande (joueur, autres arguments…),
 * surchargez directement {@link #listSuggestions} plutôt que {@link #suggestions} :</p>
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
 * @param <T> le type retourné par convert
 */
public abstract class MLStringArgument<T> implements CustomArgumentType.Converted<T, String> {

    /**
     * Retourne la collection de noms à proposer à l'auto-complétion.
     *
     * <p>Appelé à chaque frappe ; peut accéder à l'état courant du jeu.
     * L'implémentation par défaut retourne une collection vide — à surcharger,
     * ou surcharger directement {@link #listSuggestions} pour une logique contextuelle.</p>
     *
     * @return collection de suggestions, vide par défaut
     */
    protected Collection<String> suggestions() {
        return Collections.emptyList();
    }

    /**
     * Toujours {@link StringArgumentType#word()} pour ce type d'argument.
     *
     * @return le type natif Brigadier ({@link StringArgumentType#word()})
     */
    @Override
    public final @NonNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    /**
     * Filtre {@link #suggestions()} par le préfixe déjà tapé (insensible à la casse).
     * Surchargez cette méthode si la liste de suggestions dépend du contexte.
     *
     * @param <S>     type de la source de commande
     * @param context contexte Brigadier courant
     * @param builder constructeur de suggestions Brigadier
     * @return {@link CompletableFuture} des suggestions filtrées
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
