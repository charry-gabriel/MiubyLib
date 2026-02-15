package fr.miuby.lib.player;

import java.util.*;

public class MLPlayerRegistry<T extends MLPlayer> {
    private final Map<UUID, T> players = new HashMap<>();

    public void register(T player) {
        players.put(player.getUuid(), player);
    }

    public T get(UUID uuid) {
        return players.get(uuid);
    }

    public Collection<T> getAll() {
        return new ArrayList<>(players.values());
    }
}
