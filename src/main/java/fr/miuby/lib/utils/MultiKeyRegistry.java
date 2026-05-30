package fr.miuby.lib.utils;

import java.util.*;

public class MultiKeyRegistry<V> {
    private final Map<Object, V> storage = new HashMap<>();

    /** Enregistre {@code value} sous toutes les clés fournies. */
    public void register(V value, Object... keys) {
        for (Object key : keys) {
            storage.put(key, value);
        }
    }

    public V get(Object key) {
        return storage.get(key);
    }

    /** Retourne une vue dédupliquée de toutes les valeurs. */
    public Collection<V> getAll() {
        return new HashSet<>(storage.values());
    }

    public boolean contains(Object key) {
        return storage.containsKey(key);
    }

    /** Retire l'entrée associée à cette clé spécifique. */
    public V remove(Object key) {
        return storage.remove(key);
    }

    /** Retire toutes les clés pointant vers {@code value}. */
    public void unregister(V value) {
        storage.entrySet().removeIf(e -> e.getValue() == value);
    }

    /** Nombre de valeurs distinctes enregistrées. */
    public int size() {
        return new HashSet<>(storage.values()).size();
    }

    public void clear() {
        storage.clear();
    }
}
