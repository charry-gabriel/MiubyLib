package fr.miuby.lib.log;

/**
 * Interface marqueur pour les enums de catégories de log.
 *
 * <p>Implémenter sur l'enum qui définit les tags du plugin :</p>
 * <pre>{@code
 * public enum ETagLog implements ILogTag {
 *     PLAYER, VILLAGER, QUEST, WORLD, SYSTEM
 * }
 * }</pre>
 *
 * <p>Les {@code Enum} fournissent {@link #name()} automatiquement — aucune méthode à implémenter.</p>
 */
public interface ILogTag {
    /**
     * Identifiant de la catégorie, tel qu'il apparaît dans la map interne du {@link MLLogManager}.
     * Pour un {@code Enum}, retourne le nom de la constante en majuscules (ex : {@code "PLAYER"}).
     *
     * @return le nom du tag en majuscules
     */
    String name();
}
