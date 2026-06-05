package fr.miuby.lib.sqlite;

import fr.miuby.lib.MiubyLib;
import fr.miuby.lib.log.ILogTag;
import fr.miuby.lib.log.MLLogManager;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.logging.Level;

/**
 * Base abstraite pour les connexions SQLite dans les plugins MiubyLib.
 *
 * <p>Gère l'ouverture du fichier {@code .db}, le versionnage du schéma via
 * {@code PRAGMA user_version}, et l'orchestration des migrations.</p>
 *
 * <p><b>Pattern d'implémentation minimal</b></p>
 * <pre>{@code
 * public class MyDatabase extends MLSQLite {
 *     private static final int TARGET_VERSION = 2;
 *     private MyRepository myRepo;
 *
 *     public MyDatabase() {
 *         super("myplugin"); // ouvre plugins/MyPlugin/myplugin.db
 *     }
 *
 *     @Override protected int getTargetVersion() { return TARGET_VERSION; }
 *
 *     @Override
 *     protected void createTables() throws SQLException {
 *         try (Statement s = getConnection().createStatement()) {
 *             s.executeUpdate("CREATE TABLE IF NOT EXISTS player (uuid TEXT PRIMARY KEY, name TEXT NOT NULL)");
 *         }
 *     }
 *
 *     @Override
 *     protected void runMigrations(int currentVersion) throws SQLException {
 *         try (Statement s = getConnection().createStatement()) {
 *             if (currentVersion < 2) {
 *                 s.executeUpdate("ALTER TABLE player ADD COLUMN score INT NOT NULL DEFAULT 0");
 *             }
 *         }
 *     }
 *
 *     @Override
 *     protected void onLoaded() {
 *         myRepo = new MyRepository(getConnection(), this);
 *     }
 *
 *     public MyRepository myRepo() { return myRepo; }
 * }
 * }</pre>
 *
 * <p><b>Pattern avancé (chaîne d'héritage)</b></p>
 * <p>Il est possible de glisser une classe abstraite intermédiaire entre {@code MLSQLite} et
 * l'implémentation concrète. C'est le pattern utilisé dans Survi :</p>
 * <pre>
 *   MLSQLite          (MiubyLib) — plomberie connexion + migration
 *      ↑
 *   Database          (Survi, abstract) — repositories + délégués
 *      ↑
 *   SQLite            (Survi, concrete) — SQL des tables + runMigrations()
 * </pre>
 * <p>Dans ce cas, la classe intermédiaire déclare ses repositories dans {@link #onLoaded()},
 * et l'implémentation concrète surcharge {@link #onLoaded()} en appelant {@code super.onLoaded()}.</p>
 */
public abstract class MLSQLite {
    private static final ILogTag TAG = () -> "SQLITE";

    private Connection connection;
    private final String dbName;

    protected MLSQLite(String dbName) {
        this.dbName = dbName;
    }

    // =========================================================================
    // Cycle de vie
    // =========================================================================

    /**
     * Ouvre la connexion SQLite, crée les tables ({@link #createTables()}), applique les
     * migrations si {@code PRAGMA user_version < }{@link #getTargetVersion()}, puis appelle
     * {@link #onLoaded()}.
     *
     * <p><b>Finale</b> — le séquençage n'est pas surchargeable.
     * Surcharger {@link #createTables()}, {@link #runMigrations(int)} et {@link #onLoaded()}.</p>
     */
    public final void load() {
        File dbFile = new File(MiubyLib.getDataFolder(), dbName + ".db");
        if (!dbFile.exists()) {
            try {
                dbFile.getParentFile().mkdirs();
                dbFile.createNewFile();
            } catch (IOException e) {
                MLLogManager.getInstance().log(Level.SEVERE, TAG, "Impossible de créer le fichier : " + dbName + ".db", e);
                return;
            }
        }

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        } catch (ClassNotFoundException e) {
            MLLogManager.getInstance().log(Level.SEVERE, TAG, "Driver SQLite JDBC introuvable. Placez-le dans /lib.", e);
            return;
        } catch (SQLException e) {
            MLLogManager.getInstance().log(Level.SEVERE, TAG, "Impossible d'ouvrir la connexion SQLite : " + dbName, e);
            return;
        }

        try {
            createTables();

            int current = getCurrentVersion();
            if (current < getTargetVersion()) {
                MLLogManager.getInstance().log(Level.INFO, TAG, "Schéma " + current + " → " + getTargetVersion() + ", migration en cours.");
                runMigrations(current);
                setVersion(getTargetVersion());
                MLLogManager.getInstance().log(Level.INFO, TAG, "Migration terminée (schéma v" + getTargetVersion() + ").");
            }

            onLoaded();
        } catch (SQLException e) {
            MLLogManager.getInstance().log(Level.SEVERE, TAG, "Erreur SQL lors du chargement de la DB : " + dbName, e);
        }
    }

    // =========================================================================
    // Méthodes abstraites à implémenter
    // =========================================================================

    /**
     * Version cible du schéma — incrémenter à chaque migration ajoutée.
     * Utilisée pour comparer avec {@code PRAGMA user_version} lu en base.
     *
     * @return version cible du schéma
     */
    protected abstract int getTargetVersion();

    /**
     * Crée les tables manquantes via {@code CREATE TABLE IF NOT EXISTS}.
     * Appelé avant la vérification de version — idempotent par nature.
     *
     * @throws SQLException si la création d'une table échoue
     */
    protected abstract void createTables() throws SQLException;

    /**
     * Applique les migrations nécessaires pour passer de {@code currentVersion}
     * à {@link #getTargetVersion()}.
     *
     * <p>Pattern attendu :</p>
     * <pre>{@code
     * try (Statement s = getConnection().createStatement()) {
     *     if (currentVersion < 2) {
     *         s.executeUpdate("ALTER TABLE player ADD COLUMN score INT NOT NULL DEFAULT 0");
     *     }
     *     if (currentVersion < 3) {
     *         s.executeUpdate("CREATE TABLE IF NOT EXISTS log (...)");
     *     }
     * }
     * }</pre>
     *
     * <p>{@link #setVersion(int)} est appelé automatiquement par {@link #load()} après
     * cette méthode — ne pas l'appeler depuis l'implémentation.</p>
     *
     * @param currentVersion version actuelle lue depuis {@code PRAGMA user_version}
     * @throws SQLException si une migration échoue
     */
    protected abstract void runMigrations(int currentVersion) throws SQLException;

    // =========================================================================
    // Hook
    // =========================================================================

    /**
     * Appelé une fois la connexion ouverte, les tables créées et les migrations appliquées.
     * Surcharger pour initialiser les repositories ou toute logique post-chargement.
     *
     * <p>Dans une chaîne d'héritage, toujours appeler {@code super.onLoaded()} en premier
     * pour que les repositories de la classe parente soient initialisés avant les vôtres.</p>
     */
    protected void onLoaded() {}

    // =========================================================================
    // Connexion
    // =========================================================================

    /**
     * Retourne la connexion SQLite active.
     *
     * <p>Sur le thread principal ({@code "Server thread"}), réutilise la connexion persistante
     * si elle est encore ouverte. Sur un thread async, ouvre une nouvelle connexion indépendante
     * (à fermer dans un bloc {@code try-with-resources} ou {@code finally}).</p>
     *
     * @return la connexion, ou {@code null} en cas d'erreur
     */
    @Nullable
    public Connection getConnection() {
        try {
            if (connection != null && !connection.isClosed() && Thread.currentThread().getName().equals("Server thread")) {
                return connection;
            }
            File dbFile = new File(MiubyLib.getDataFolder(), dbName + ".db");
            return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        } catch (SQLException e) {
            MLLogManager.getInstance().log(Level.SEVERE, TAG, "Impossible d'obtenir une connexion SQLite.", e);
            return null;
        }
    }

    // =========================================================================
    // Debug SQL brut
    // =========================================================================

    /**
     * Exécute une requête SQL brute et retourne le résultat formaté en String.
     * Réservé au debug et aux commandes admin — ne pas utiliser dans du code métier.
     *
     * <p>Si la requête commence par {@code SELECT}, retourne les lignes séparées par {@code \n}.
     * Sinon (INSERT/UPDATE/DELETE/PRAGMA…), retourne {@code "Query executed !"}.</p>
     *
     * @param sql requête SQL à exécuter
     * @return résultat formaté de la requête, ou un message d'erreur en cas d'échec
     */
    public String executeRaw(String sql) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);

            if (sql.trim().split("\\s+")[0].equalsIgnoreCase("select")) {
                ResultSet rs = ps.executeQuery();
                int column = rs.getMetaData().getColumnCount();
                StringBuilder result = new StringBuilder();
                while (rs.next()) {
                    for (int i = 1; i <= column; i++) {
                        result.append(rs.getString(i));
                        if (i != column) result.append(", ");
                    }
                    result.append("\n");
                }
                return result.toString();
            } else {
                ps.executeUpdate();
                return "Query executed !";
            }
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, TAG, "Failed to execute raw SQL: " + sql, ex);
            return "Error: " + ex.getMessage();
        } finally {
            closeResources(conn, ps);
        }
    }

    private static void closeResources(Connection conn, PreparedStatement ps) {
        try {
            if (ps != null) ps.close();
            if (conn != null) conn.close();
        } catch (SQLException ex) {
            MLLogManager.getInstance().log(Level.SEVERE, TAG, "Failed to close database resources", ex);
        }
    }

    // =========================================================================
    // Utilitaires protégés
    // =========================================================================

    /**
     * Lit la version actuelle du schéma via {@code PRAGMA user_version}.
     * Retourne {@code 0} si la lecture échoue (base vierge ou corrompue).
     *
     * @return version actuelle du schéma, ou {@code 0} en cas d'erreur
     */
    protected int getCurrentVersion() {
        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            MLLogManager.getInstance().log(Level.WARNING, TAG, "Impossible de lire la version du schéma, hypothèse 0.", e);
            return 0;
        }
    }

    /**
     * Écrit la version du schéma via {@code PRAGMA user_version = N}.
     * Appelé automatiquement par {@link #load()} après {@link #runMigrations(int)}.
     * Ne pas appeler manuellement.
     *
     * @param version nouvelle version du schéma à persister
     * @throws SQLException si l'écriture échoue
     */
    protected void setVersion(int version) throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.execute("PRAGMA user_version = " + version);
        }
    }

    /**
     * Vérifie si une colonne existe dans une table.
     * Utile dans {@link #runMigrations(int)} pour rendre les {@code ALTER TABLE} idempotents :
     *
     * <pre>{@code
     * if (currentVersion < 3 && !hasColumn("player", "score")) {
     *     s.executeUpdate("ALTER TABLE player ADD COLUMN score INT NOT NULL DEFAULT 0");
     * }
     * }</pre>
     *
     * @param table  nom de la table à inspecter
     * @param column nom de la colonne à chercher
     * @return {@code true} si la colonne existe
     * @throws SQLException si la lecture du schéma échoue
     */
    protected boolean hasColumn(String table, String column) throws SQLException {
        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) return true;
            }
        }
        return false;
    }
}
