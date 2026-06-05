package fr.miuby.lib.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilitaire de cooldown générique basé sur le temps système.
 *
 * <pre>{@code
 * private final Cooldown<UUID> warnCooldown = new Cooldown<>(6_000L);
 *
 * if (!warnCooldown.isOnCooldown(player.getUniqueId())) {
 *     warnCooldown.set(player.getUniqueId());
 *     player.sendMessage("...");
 * }
 * }</pre>
 *
 * @param <K> type de la clé (ex : {@code UUID}, {@code String}, …)
 */
public class Cooldown<K> {
    private final long durationMs;
    private final Map<K, Long> timestamps = new HashMap<>();

    /**
     * Crée un cooldown avec la durée spécifiée.
     *
     * @param durationMs durée du cooldown en millisecondes, doit être &gt; 0
     * @throws IllegalArgumentException si {@code durationMs} est &lt;= 0
     */
    public Cooldown(long durationMs) {
        if (durationMs <= 0) throw new IllegalArgumentException("durationMs doit être > 0");
        this.durationMs = durationMs;
    }

    /**
     * Déclenche le cooldown pour {@code key}.
     *
     * @param key la clé pour laquelle déclencher le cooldown
     */
    public void set(K key) {
        timestamps.put(key, System.currentTimeMillis());
    }

    /**
     * Indique si {@code key} est encore en cooldown.
     *
     * @param key la clé à vérifier
     * @return {@code true} si {@code key} est en cooldown
     */
    public boolean isOnCooldown(K key) {
        Long last = timestamps.get(key);
        if (last == null) return false;
        return System.currentTimeMillis() - last < durationMs;
    }

    /**
     * Retourne le temps restant en millisecondes (0 si pas en cooldown ou inconnu).
     *
     * @param key la clé à vérifier
     * @return temps restant en millisecondes, {@code 0} si le cooldown est expiré ou absent
     */
    public long remaining(K key) {
        Long last = timestamps.get(key);
        if (last == null) return 0L;
        return Math.max(0L, durationMs - (System.currentTimeMillis() - last));
    }

    /**
     * Force la fin du cooldown pour {@code key}.
     *
     * @param key la clé dont le cooldown doit être réinitialisé
     */
    public void reset(K key) {
        timestamps.remove(key);
    }

    /** Vide tous les cooldowns enregistrés. */
    public void clear() {
        timestamps.clear();
    }
}
