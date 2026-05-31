package fr.miuby.lib.sqlite;

import fr.miuby.lib.MiubyLib;
import fr.miuby.lib.log.ILogTag;
import fr.miuby.lib.log.MLLogManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Classe de base pour les repositories SQLite dans les plugins MiubyLib.
 *
 * <p>Fournit :</p>
 * <ul>
 *   <li>La connexion sync (thread principal), accessible via {@link #connection}.</li>
 *   <li>{@link #runAsync(SQLTask)} — exécute une opération async avec gestion de connexion et d'erreur intégrées.</li>
 *   <li>{@link #runAsync(SQLTask, ILogTag, String)} — variante avec tag et message d'erreur personnalisés.</li>
 * </ul>
 *
 * <h3>Pattern d'implémentation</h3>
 * <pre>{@code
 * public class MyRepository extends MLRepository {
 *
 *     public MyRepository(Connection connection, MLSQLite db) {
 *         super(connection, db);
 *     }
 *
 *     // Lecture sync — utilise this.connection directement
 *     public String load(String id) {
 *         try (PreparedStatement ps = connection.prepareStatement("SELECT name FROM my_table WHERE id = ?")) {
 *             ps.setString(1, id);
 *             try (ResultSet rs = ps.executeQuery()) {
 *                 return rs.next() ? rs.getString("name") : null;
 *             }
 *         } catch (SQLException ex) {
 *             MLLogManager.getInstance().log(Level.SEVERE, ELogTag.SYSTEM, "Failed to load", ex);
 *             return null;
 *         }
 *     }
 *
 *     // Écriture async — runAsync ouvre une connexion fraîche et la ferme automatiquement
 *     public void save(String id, String name) {
 *         runAsync(conn -> {
 *             try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO my_table (id, name) VALUES (?, ?)")) {
 *                 ps.setString(1, id);
 *                 ps.setString(2, name);
 *                 ps.executeUpdate();
 *             }
 *         }, ELogTag.SYSTEM, "Failed to save");
 *     }
 * }
 * }</pre>
 */
public abstract class MLRepository {
    private static final ILogTag TAG = () -> "REPOSITORY";

    protected final Connection connection;
    private final MLSQLite db;

    protected MLRepository(Connection connection, MLSQLite db) {
        this.connection = connection;
        this.db = db;
    }

    /**
     * Exécute {@code task} dans un thread async avec une connexion fraîche auto-fermée.
     * Les erreurs SQL sont loggées avec le tag générique REPOSITORY.
     */
    protected void runAsync(SQLTask task) {
        MiubyLib.runAsync(() -> {
            try (Connection conn = db.getConnection()) {
                task.execute(conn);
            } catch (SQLException ex) {
                MLLogManager.getInstance().log(Level.SEVERE, TAG, "Async DB error", ex);
            }
        });
    }

    /**
     * Variante de {@link #runAsync(SQLTask)} avec tag et message d'erreur propres au plugin.
     *
     * @param tag      tag de log du plugin appelant (e.g. {@code ELogTag.PLAYER})
     * @param errorMsg message d'erreur affiché si la tâche échoue
     */
    protected void runAsync(SQLTask task, ILogTag tag, String errorMsg) {
        MiubyLib.runAsync(() -> {
            try (Connection conn = db.getConnection()) {
                task.execute(conn);
            } catch (SQLException ex) {
                MLLogManager.getInstance().log(Level.SEVERE, tag, errorMsg, ex);
            }
        });
    }

    /**
     * Tâche SQL async passée à {@link #runAsync}.
     * La connexion fournie est fraîche ; utiliser try-with-resources pour les PreparedStatement et ResultSet.
     */
    @FunctionalInterface
    public interface SQLTask {
        void execute(Connection conn) throws SQLException;
    }
}