package fr.miuby.lib.utils;

import java.util.*;

/**
 * Registry générique permettant d'associer plusieurs clés à une même valeur.
 *
 * <p>Utile pour indexer un objet selon plusieurs identifiants (UUID, nom, type, etc.).</p>
 *
 * <p><b>Perf :</b> {@link #getAll()} et {@link #size()} sont O(1) sans allocation grâce à
 * un {@code HashSet} secondaire mis à jour à chaque écriture. La vue retournée par {@link #getAll()}
 * est non-modifiable ; les appelants ne doivent pas tenter de la modifier.</p>
 *
 * <p><b>Contrainte :</b> ré-enregistrer une clé déjà connue vers une <em>valeur différente</em>
 * sans passer d'abord par {@link #unregister(Object)} peut laisser l'ancienne valeur orpheline
 * dans {@code distinctValues}. Ce cas ne se produit pas dans les usages du projet.</p>
 *
 * @param <V> type des valeurs stockées
 */
public class MultiKeyRegistry<V> {
    private final Map<Object, V> storage = new HashMap<>();

    /**
     * Ensemble dédupliqué des valeurs distinctes enregistrées.
     * Mis à jour en O(1) à chaque écriture pour éviter l'allocation dans {@link #getAll()}.
     */
    private final Set<V> distinctValues = new HashSet<>();

    /**
     * Enregistre {@code value} sous toutes les clés fournies.
     *
     * @param value la valeur à enregistrer
     * @param keys  une ou plusieurs clés d'accès
     */
    public void register(V value, Object... keys) {
        distinctValues.add(value);
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
     * Retourne une vue non-modifiable et dédupliquée de toutes les valeurs.
     * Appel O(1) — aucune allocation.
     *
     * @return vue non-modifiable de toutes les valeurs distinctes
     */
    public Collection<V> getAll() {
        return Collections.unmodifiableCollection(distinctValues);
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
     * Si la valeur n'a plus aucune clé après ce retrait, elle est aussi retirée des valeurs distinctes.
     *
     * @param key la clé à retirer
     * @return la valeur précédemment associée, ou {@code null}
     */
    public V remove(Object key) {
        V value = storage.remove(key);
        if (value != null && !storage.containsValue(value)) {
            distinctValues.remove(value);
        }
        return value;
    }

    /**
     * Retire toutes les clés pointant vers {@code value}.
     *
     * @param value la valeur à désindexer
     */
    public void unregister(V value) {
        storage.entrySet().removeIf(e -> e.getValue() == value);
        distinctValues.remove(value);
    }

    /**
     * Retourne le nombre de valeurs distinctes enregistrées.
     * Appel O(1) — aucune allocation.
     *
     * @return nombre de valeurs distinctes
     */
    public int size() {
        return distinctValues.size();
    }

    /** Vide toutes les entrées du registry. */
    public void clear() {
        storage.clear();
        distinctValues.clear();
    }
}
