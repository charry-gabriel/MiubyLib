package fr.miuby.lib.command;

import com.mojang.brigadier.Message;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.text.Component;

/**
 * Utilitaires génériques pour l'API Brigadier.
 *
 * <p>Utilisation typique dans la déclaration d'erreurs :</p>
 * <pre>{@code
 * public static final DynamicCommandExceptionType ENTITY_NOT_FOUND =
 *     new DynamicCommandExceptionType(name ->
 *         MLBrigadierHelper.message(Component.text("Entité introuvable : " + name))
 *     );
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
}
