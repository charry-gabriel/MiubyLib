package fr.miuby.lib.player;

import fr.miuby.lib.utils.MultiKeyRegistry;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

public class MLPlayerRegistry<T extends MLPlayer> {
    private final MultiKeyRegistry<T> players = new MultiKeyRegistry<>();

    /** Enregistre {@code player} sous son UUID et son pseudo. */
    public void register(T player) {
        players.register(player, player.getUuid(), player.getPseudo());
    }

    /** Retire {@code player} du registry (toutes ses clés). */
    public void unregister(T player) {
        players.unregister(player);
    }

    public T get(UUID uuid) {
        return players.get(uuid);
    }

    public T get(String pseudo) {
        return players.get(pseudo);
    }

    /** Raccourci : équivalent à {@link #get(UUID)} avec {@code player.getUniqueId()}. */
    public T get(Player player) {
        return players.get(player.getUniqueId());
    }

    public Collection<T> getAll() {
        return players.getAll();
    }

    public boolean contains(UUID uuid) {
        return players.contains(uuid);
    }

    public boolean contains(String pseudo) {
        return players.contains(pseudo);
    }

    public int size() {
        return players.size();
    }
}
