package fr.miuby.lib.utils;

import java.util.*;

public class MultiKeyRegistry<V> {
    private final Map<Object, V> storage = new HashMap<>();

    public void register(V value, Object... keys) {
        for (Object key : keys) {
            storage.put(key, value);
        }
    }

    public V get(Object key) {
        return storage.get(key);
    }

    public Collection<V> getAll() {
        return new HashSet<>(storage.values());
    }

    public boolean contains(Object key) {
        return storage.containsKey(key);
    }

    public void clear() {
        storage.clear();
    }
}