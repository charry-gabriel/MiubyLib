package fr.miuby.lib.log;

import org.jetbrains.annotations.Nullable;

/**
 * Interface de persistence des états de log (tags et levels).
 *
 * <p>À implémenter dans le plugin pour brancher la persistence sur une base de données
 * ou un fichier de config. Si aucune persistence n'est fournie à
 * {@link MLLogManager#initialize(MLLogPersistence)}, les états sont gardés uniquement en mémoire.</p>
 *
 * <pre>{@code
 * public class MyLogPersistence implements MLLogPersistence {
 *     private final SystemRepository repo;
 *
 *     public MyLogPersistence(SystemRepository repo) { this.repo = repo; }
 *
 *     @Override public Boolean getTagState(String name)   { return repo.getLogTagState(name); }
 *     @Override public void saveTagState(String name, boolean e) { repo.saveLogTagState(name, e); }
 *     @Override public Boolean getLevelState(String name)  { return repo.getLogLevelState(name); }
 *     @Override public void saveLevelState(String name, boolean e) { repo.saveLogLevelState(name, e); }
 * }
 * }</pre>
 */
public interface MLLogPersistence {

    /**
     * Charge l'état d'un tag depuis la persistence.
     *
     * @param tagName nom du tag (ex : {@code "PLAYER"})
     * @return {@code true}/{@code false} si une valeur existe, {@code null} sinon
     */
    @Nullable
    Boolean getTagState(String tagName);

    /**
     * Persiste l'état d'un tag.
     *
     * @param tagName nom du tag
     * @param enabled nouvel état
     */
    void saveTagState(String tagName, boolean enabled);

    /**
     * Charge l'état d'un level depuis la persistence.
     *
     * @param levelName nom du level JUL (ex : {@code "INFO"}, {@code "FINE"})
     * @return {@code true}/{@code false} si une valeur existe, {@code null} sinon
     */
    @Nullable
    Boolean getLevelState(String levelName);

    /**
     * Persiste l'état d'un level.
     *
     * @param levelName nom du level JUL
     * @param enabled   nouvel état
     */
    void saveLevelState(String levelName, boolean enabled);
}
