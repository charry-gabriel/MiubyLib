package fr.miuby.lib.player;

import fr.miuby.lib.utils.MultiKeyRegistry;

import java.util.*;

public class MLPlayerRegistry<T extends MLPlayer> {
    private final MultiKeyRegistry<T> players = new MultiKeyRegistry<>();

    public void register(T player) {
        players.register(player, player.getUuid(), player.getPseudo());
    }

    public T get(UUID uuid) {
        return players.get(uuid);
    }

    public T get(String pseudo) {
        return players.get(pseudo);
    }

    public Collection<T> getAll() {
        return players.getAll();
    }
}
