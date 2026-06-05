package fr.miuby.lib.world;

/**
 * Interface marqueur pour les types de monde.
 *
 * <p>Implémenter sur une enum dans le plugin pour catégoriser les mondes :</p>
 * <pre>{@code
 * public enum EWorldType implements WorldType {
 *     LOBBY, SURVIVAL, MINIGAME
 * }
 * }</pre>
 */
public interface WorldType { }
