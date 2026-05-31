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
     */
    public static SimpleCommandExceptionType simpleError(String errorMessage) {
        return new SimpleCommandExceptionType(message(Component.text(errorMessage)));
    }
}