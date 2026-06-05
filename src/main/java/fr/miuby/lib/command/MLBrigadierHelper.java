package fr.miuby.lib.command;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.text.Component;

/**
 * Utilitaires génériques pour l'API Brigadier.
 *
 * <p>Utilisation typique dans la déclaration d'erreurs :</p>
 * <pre>{@code
 * public static final DynamicCommandExceptionType PLAYER_NOT_FOUND = MLBrigadierHelper.notFound("Joueur");
 * public static final SimpleCommandExceptionType  INVALID_STATE    = MLBrigadierHelper.simpleError("État invalide.");
 * }</pre>
 */
public final class MLBrigadierHelper {
    private MLBrigadierHelper() {}

    /**
     * Convertit un {@link Component} Adventure en {@link Message} Brigadier,
     * nécessaire pour les {@code CommandExceptionType}.
     *
     * @param component le composant Adventure à convertir
     * @return le message Brigadier correspondant
     */
    public static Message message(Component component) {
        return MessageComponentSerializer.message().serialize(component);
    }

    /**
     * Crée un {@link DynamicCommandExceptionType} standard "{entityLabel} introuvable : {name}".
     *
     * <pre>{@code
     * public static final DynamicCommandExceptionType PLAYER_NOT_FOUND = MLBrigadierHelper.notFound("Joueur");
     * // → "Joueur introuvable : Steve" quand throw PLAYER_NOT_FOUND.create("Steve")
     * }</pre>
     *
     * @param entityLabel libellé de l'entité (ex : {@code "Joueur"}, {@code "Monde"})
     * @return le type d'exception dynamique créé
     */
    public static DynamicCommandExceptionType notFound(String entityLabel) {
        return new DynamicCommandExceptionType(name -> message(Component.text(entityLabel + " introuvable : " + name)));
    }

    /**
     * Crée un {@link SimpleCommandExceptionType} avec le message fourni.
     *
     * <pre>{@code
     * public static final SimpleCommandExceptionType NOT_A_LEVEL = MLBrigadierHelper.simpleError("Ce villageois n'est pas un level.");
     * }</pre>
     *
     * @param errorMessage message d'erreur à afficher
     * @return le type d'exception simple créé
     */
    public static SimpleCommandExceptionType simpleError(String errorMessage) {
        return new SimpleCommandExceptionType(message(Component.text(errorMessage)));
    }
}
