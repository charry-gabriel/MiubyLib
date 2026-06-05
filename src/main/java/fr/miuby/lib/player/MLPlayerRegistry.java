package fr.miuby.lib.player;

import fr.miuby.lib.utils.MultiKeyRegistry;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

/**
 * Registry générique des joueurs {@link MLPlayer}.
 *
 * <p>Indexe les joueurs par {@link UUID} et par pseudo pour un accès rapide dans les deux sens.</p>
 *
 * @param <T> type concret du joueur, doit étendre {@link MLPlayer}
 */
public class MLPlayerRegistry<T extends MLPlayer> {
    private final MultiKeyRegistry<T> players = new MultiKeyRegistry<>();

    /**
     * Enregistre {@code player} sous son UUID et son pseudo.
     *
     * @param player le joueur à enregistrer
     */
    public void register(T player) {
        players.register(player, player.getUuid(), player.getPseudo());
    }

    /**
     * Retire {@code player} du registry (toutes ses clés).
     *
     * @param player le joueur à retirer
     */
    public void unregister(T player) {
        players.unregister(player);
    }

    /**
     * Retourne le joueur associé à cet UUID.
     *
     * @param uuid l'UUID du joueur
     * @return le joueur correspondant, ou {@code null}
     */
    public T get(UUID uuid) {
        return players.get(uuid);
    }

    /**
     * Retourne le joueur associé à ce pseudo.
     *
     * @param pseudo le pseudo du joueur
     * @return le joueur correspondant, ou {@code null}
     */
    public T get(String pseudo) {
        return players.get(pseudo);
    }

    /**
     * Raccourci : équivalent à {@link #get(UUID)} avec {@code player.getUniqueId()}.
     *
     * @param player l'entité Bukkit du joueur
     * @return le joueur correspondant, ou {@code null}
     */
    public T get(Player player) {
        return players.get(player.getUniqueId());
    }

    /**
     * Retourne tous les joueurs enregistrés.
     *
     * @return collection dédupliquée de tous les joueurs
     */
    public Collection<T> getAll() {
        return players.getAll();
    }

    /**
     * Indique si un joueur avec cet UUID est enregistré.
     *
     * @param uuid l'UUID à vérifier
     * @return {@code true} si un joueur est enregistré avec cet UUID
     */
    public boolean contains(UUID uuid) {
        return players.contains(uuid);
    }

    /**
     * Indique si un joueur avec ce pseudo est enregistré.
     *
     * @param pseudo le pseudo à vérifier
     * @return {@code true} si un joueur est enregistré avec ce pseudo
     */
    public boolean contains(String pseudo) {
        return players.contains(pseudo);
    }

    /**
     * Retourne le nombre de joueurs distincts enregistrés.
     *
     * @return nombre de joueurs dans le registry
     */
    public int size() {
        return players.size();
    }
}
