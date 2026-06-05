package fr.miuby.lib.utils;

import java.util.*;

/**
 * Registry générique permettant d'associer plusieurs clés à une même valeur.
 *
 * <p>Utile pour indexer un objet selon plusieurs identifiants (UUID, nom, type, etc.).</p>
 *
 * @param <V> type des valeurs stockées
 */
public class MultiKeyRegistry<V> {
    private final Map<Object, V> storage = new HashMap<>();

    /**
     * Enregistre {@code value} sous toutes les clés fournies.
     *
     * @param value la valeur à enregistrer
     * @param keys  une ou plusieurs clés d'accès
     */
    public void register(V value, Object... keys) {
        for (Object key : keys) {
            storage.put(key, value);
        }
    }

    /**
     * Retourne la valeur associée à cette clé.
     *
     * @param key la clé de recherche
     * @return la valeur associée, ou {@code null}
     */
    public V get(Object key) {
        return storage.get(key);
    }

    /**
     * Retourne une vue dédupliquée de toutes les valeurs.
     *
     * @return ensemble dédupliqué de toutes les valeurs
     */
    public Collection<V> getAll() {
        return new HashSet<>(storage.values());
    }

    /**
     * Indique si cette clé est enregistrée.
     *
     * @param key la clé à vérifier
     * @return {@code true} si la clé est présente
     */
    public boolean contains(Object key) {
        return storage.containsKey(key);
    }

    /**
     * Retire l'entrée associée à cette clé spécifique.
     *
     * @param key la clé à retirer
     * @return la valeur précédemment associée, ou {@code null}
     */
    public V remove(Object key) {
        return storage.remove(key);
    }

    /**
     * Retire toutes les clés pointant vers {@code value}.
     *
     * @param value la valeur à désindexer
     */
    public void unregister(V value) {
        storage.entrySet().removeIf(e -> e.getValue() == value);
    }

    /**
     * Retourne le nombre de valeurs distinctes enregistrées.
     *
     * @return nombre de valeurs distinctes
     */
    public int size() {
        return new HashSet<>(storage.values()).size();
    }

    /** Vide toutes les entrées du registry. */
    public void clear() {
        storage.clear();
    }
}
