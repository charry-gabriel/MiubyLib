package fr.miuby.lib.resource;

import fr.miuby.lib.log.ILogTag;
import fr.miuby.lib.log.MLLogManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

/**
 * Utilitaire MiubyLib pour la gestion des ressources YAML d'un plugin.
 *
 * <p><b>Déploiement (JAR → disque)</b></p>
 * <ul>
 *   <li>{@link #deploy(JavaPlugin, String)} — copie/met à jour un fichier unique.</li>
 *   <li>{@link #deployFolder(JavaPlugin, String)} — copie/met à jour tous les {@code .yml}
 *       d'un dossier embarqué dans le JAR.</li>
 * </ul>
 * Peut être appelé <em>avant</em> {@code MiubyLib.init()} — seul un {@link JavaPlugin} est requis.
 *
 * <p><b>Chargement POJO (SnakeYAML, avec cache)</b></p>
 * <ul>
 *   <li>{@link #loadPojo(JavaPlugin, String, String, Class)} — charge par ID, résultat mis en cache.</li>
 *   <li>{@link #loadPojoAll(JavaPlugin, String, Class)} — charge tout un dossier, résultats mis en cache.</li>
 * </ul>
 *
 * <p><b>Usage typique dans un plugin</b></p>
 * <pre>{@code
 * // Dans onEnable(), avant MiubyLib.init() :
 * MLResourceManager.deploy(this, "config.yml");
 * MLResourceManager.deployFolder(this, "villagers");
 * MLResourceManager.deployFolder(this, "traders");
 *
 * // Dans une factory (après MiubyLib.init()) :
 * MyConfig cfg = MLResourceManager.loadPojo(plugin, "villagers", "francois", MyConfig.class);
 * List<TraderConfig> all = MLResourceManager.loadPojoAll(plugin, "traders", TraderConfig.class);
 * }</pre>
 */
public final class MLResourceManager {

    private MLResourceManager() {}

    /** Tag de log utilisé par toutes les méthodes de cette classe. */
    private static final ILogTag TAG = () -> "RESOURCE";

    /** Clé : {@code "folder/id@fr.pkg.Type"} → POJO déjà chargé. */
    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>();

    // =========================================================================
    // DEPLOY — ressource JAR → disque
    // =========================================================================

    /**
     * Copie la ressource {@code resourcePath} du JAR vers le dossier de données du plugin.
     * <ul>
     *   <li>Fichier absent → créé.</li>
     *   <li>Fichier présent mais hash MD5 différent → écrasé.</li>
     *   <li>Fichier identique → rien.</li>
     * </ul>
     *
     * @param plugin       instance du plugin
     * @param resourcePath chemin relatif dans le JAR (ex : {@code "quests.yml"},
     *                     {@code "villagers/bob.yml"})
     */
    public static void deploy(JavaPlugin plugin, String resourcePath) {
        File diskFile = new File(plugin.getDataFolder(), resourcePath);

        InputStream jarStream = plugin.getResource(resourcePath);
        if (jarStream == null) {
            MLLogManager.getInstance().log(Level.SEVERE, TAG,
                    "[MLResourceManager] Ressource introuvable dans le jar : " + resourcePath);
            return;
        }

        if (!diskFile.exists()) {
            diskFile.getParentFile().mkdirs();
            plugin.saveResource(resourcePath, false);
            MLLogManager.getInstance().log(Level.INFO, TAG,
                    "[MLResourceManager] Créé : " + resourcePath);
            return;
        }

        try {
            byte[] jarHash  = md5(jarStream);
            byte[] diskHash = md5(Files.newInputStream(diskFile.toPath()));

            if (!MessageDigest.isEqual(jarHash, diskHash)) {
                plugin.saveResource(resourcePath, true);
                MLLogManager.getInstance().log(Level.INFO, TAG,
                        "[MLResourceManager] Mis à jour : " + resourcePath);
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            MLLogManager.getInstance().log(Level.SEVERE, TAG,
                    "[MLResourceManager] Erreur lors de la comparaison de " + resourcePath, e);
        }
    }

    /**
     * Déploie tous les fichiers {@code .yml} du dossier {@code folder} embarqués dans le JAR
     * vers le sous-dossier correspondant du dossier de données du plugin.
     *
     * <p>Chaque fichier est traité via {@link #deploy}, donc seuls les fichiers absents
     * ou modifiés (MD5) sont écrits sur le disque.</p>
     *
     * @param plugin instance du plugin
     * @param folder nom du dossier dans le JAR (ex : {@code "villagers"}, {@code "traders"})
     */
    public static void deployFolder(JavaPlugin plugin, String folder) {
        File jarFile = resolveJar(plugin);

        if (jarFile == null) {
            MLLogManager.getInstance().log(Level.SEVERE, TAG,
                    "[MLResourceManager] Impossible de localiser le JAR pour déployer le dossier : " + folder);
            return;
        }

        String prefix = folder.endsWith("/") ? folder : folder + "/";

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(prefix) && name.endsWith(".yml") && !entry.isDirectory()) {
                    deploy(plugin, name);
                }
            }
        } catch (IOException e) {
            MLLogManager.getInstance().log(Level.SEVERE, TAG,
                    "[MLResourceManager] Erreur lors du scan du dossier JAR : " + folder, e);
        }
    }

    // =========================================================================
    // LOAD — POJO via SnakeYAML (cached)
    // =========================================================================

    /**
     * Charge {@code <folder>/<id>.yml} depuis le dossier de données du plugin
     * et le désérialise en POJO de type {@code type}.
     *
     * <p>Les appels successifs avec les mêmes arguments retournent l'instance mise en cache
     * sans relire le disque.</p>
     *
     * @param plugin  instance du plugin
     * @param folder  sous-dossier relatif dans le dossier de données (ex : {@code "villagers"})
     * @param id      nom du fichier sans extension (ex : {@code "francois"})
     * @param type    classe POJO cible
     * @param <T>     type du POJO
     * @return le POJO désérialisé
     * @throws RuntimeException si le fichier est absent ou illisible
     */
    @SuppressWarnings("unchecked")
    public static <T> T loadPojo(JavaPlugin plugin, String folder, String id, Class<T> type) {
        String key = cacheKey(folder, id, type);
        T cached = (T) CACHE.get(key);
        if (cached != null) return cached;

        File file = new File(plugin.getDataFolder(), folder + "/" + id + ".yml");
        try (InputStream stream = new FileInputStream(file)) {
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            Yaml yaml = new Yaml(new Constructor(type, new LoaderOptions()));
            T result = yaml.load(content);
            CACHE.put(key, result);
            return result;
        } catch (IOException e) {
            throw new RuntimeException(
                    "[MLResourceManager] Impossible de charger " + folder + "/" + id + ".yml", e);
        }
    }

    /**
     * Charge tous les fichiers {@code .yml} présents dans {@code folder} comme POJOs
     * de type {@code type}.
     *
     * <p>Les fichiers déjà en cache (via {@link #loadPojo}) sont réutilisés directement.</p>
     *
     * @param plugin  instance du plugin
     * @param folder  sous-dossier relatif dans le dossier de données
     * @param type    classe POJO cible
     * @param <T>     type du POJO
     * @return liste des configs chargées (peut être vide, jamais null)
     */
    public static <T> List<T> loadPojoAll(JavaPlugin plugin, String folder, Class<T> type) {
        File dir = new File(plugin.getDataFolder(), folder);
        if (!dir.isDirectory()) return List.of();

        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return List.of();

        List<T> results = new ArrayList<>(files.length);
        for (File f : files) {
            String id = f.getName().replace(".yml", "");
            results.add(loadPojo(plugin, folder, id, type));
        }
        return results;
    }

    /**
     * Vide le cache en mémoire.
     * Utile lors d'un rechargement à chaud — à appeler dans {@code onDisable()} si nécessaire.
     */
    public static void clearCache() {
        CACHE.clear();
    }

    // =========================================================================
    // Utilitaires internes
    // =========================================================================

    private static byte[] md5(InputStream stream)
            throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (DigestInputStream dis = new DigestInputStream(stream, md)) {
            dis.readAllBytes();
        }
        return md.digest();
    }

    private static <T> String cacheKey(String folder, String id, Class<T> type) {
        return folder + "/" + id + "@" + type.getName();
    }

    /**
     * Localise le fichier JAR du plugin via son ClassLoader (CodeSource).
     * Retourne {@code null} si la résolution échoue (contexte de test sans JAR, etc.).
     *
     * @param plugin instance du plugin dont on cherche le JAR
     * @return le fichier JAR, ou {@code null} si introuvable
     */
    private static File resolveJar(JavaPlugin plugin) {
        try {
            URL location = plugin.getClass()
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation();
            if (location != null) return new File(location.toURI());
        } catch (Exception ignored) {}
        return null;
    }
}
