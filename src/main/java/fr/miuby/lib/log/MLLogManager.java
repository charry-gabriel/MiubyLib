package fr.miuby.lib.log;

import com.google.common.base.CaseFormat;
import fr.miuby.lib.MiubyLib;
import lombok.Getter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.*;

/**
 * Gestionnaire de logs par catégories ({@link ILogTag}) ET par niveaux ({@link Level}).
 *
 * <p>Un message est émis si son tag ET son level sont tous les deux activés.</p>
 *
 * <p><b>Sorties fichiers</b></p>
 * <ul>
 *   <li>{@code <plugin>-debug-%g.log} — tout (ALL), stacktraces incluses, 10 × 10 MB</li>
 *   <li>{@code <plugin>-info-%g.log}  — INFO+, 5 × 5 MB</li>
 *   <li>{@code <plugin>-warn-%g.log}  — WARNING+, 5 × 2 MB (archive légère)</li>
 *   <li>Console                       — WARNING+ seulement</li>
 * </ul>
 *
 * <p><b>Usage minimal</b></p>
 * <pre>{@code
 * // 1. Déclarer les tags (enum implements ILogTag)
 * public enum ETagLog implements ILogTag { PLAYER, WORLD, SYSTEM }
 *
 * // 2. Enregistrer et initialiser (après MiubyLib.init())
 * MLLogManager log = MLLogManager.getInstance();
 * log.registerTags(ETagLog.values());
 * log.initialize();                          // sans persistence
 * // ou :
 * log.initialize(new MyLogPersistence(db));  // avec persistence DB
 *
 * // 3. Logger
 * log.log(Level.INFO, ETagLog.SYSTEM, "Plugin démarré");
 * log.log(Level.SEVERE, ETagLog.PLAYER, "Erreur critique", exception);
 * }</pre>
 */
public class MLLogManager {
    private static MLLogManager instance = null;

    /**
     * Retourne l'instance unique du gestionnaire de logs.
     *
     * @return l'instance singleton de {@code MLLogManager}
     */
    public static MLLogManager getInstance() {
        if (instance == null) instance = new MLLogManager();
        return instance;
    }

    /** Logger JUL utilisé pour toutes les sorties. Initialisé à {@code "MiubyLib"} avant {@link #initialize()}. */
    @Getter
    private Logger logger = Logger.getLogger("MiubyLib");

    private final Map<String, Boolean>  enabledTags   = new HashMap<>();
    private final Map<Level, Boolean>   enabledLevels = new LinkedHashMap<>();
    private final List<String>          noiseFilters  = new ArrayList<>();

    /**
     * Cache des noms de tags formatés ({@code "ALPHA_PLAYER"} → {@code "AlphaPlayer"}).
     * Peuplé lors de {@link #registerTags} pour éviter la conversion Guava à chaque appel de log.
     */
    private final Map<String, String> formattedTagCache = new HashMap<>();

    private boolean isInitialized = false;
    private MLLogPersistence persistence = null;

    private MLLogManager() {
        enabledLevels.put(Level.SEVERE,  true);
        enabledLevels.put(Level.WARNING, true);
        enabledLevels.put(Level.INFO,    true);
        enabledLevels.put(Level.CONFIG,  true);
        enabledLevels.put(Level.FINE,    true);
        enabledLevels.put(Level.FINER,   true);
        enabledLevels.put(Level.FINEST,  true);

        // Bruit Paper intégré par défaut
        noiseFilters.add("Named entity");
        noiseFilters.add("Saving oversized chunk");
    }

    // =========================================================================
    // ENREGISTREMENT DES TAGS
    // =========================================================================

    /**
     * Enregistre les tags d'un enum implémentant {@link ILogTag}.
     * Peut être appelé avant ou après {@link #initialize()}.
     * Pré-calcule les noms formatés pour éviter la conversion Guava sur chaque appel de log.
     *
     * @param <T>  type de l'enum implémentant {@link ILogTag}
     * @param tags tableau {@code MyEnum.values()}
     */
    public <T extends Enum<T> & ILogTag> void registerTags(T[] tags) {
        for (T tag : tags) {
            enabledTags.putIfAbsent(tag.name(), true);
            formattedTagCache.putIfAbsent(tag.name(), CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, tag.name()));
        }
    }

    /**
     * Ajoute un filtre de bruit externe par sous-chaîne.
     * Les messages contenant cette sous-chaîne seront redirigés vers debug
     * et masqués de la console.
     *
     * @param substring sous-chaîne à filtrer
     */
    public void addNoiseFilter(String substring) {
        noiseFilters.add(substring);
    }

    // =========================================================================
    // INITIALISATION
    // =========================================================================

    /** Initialise sans persistence — les états tag/level sont en mémoire uniquement. */
    public void initialize() {
        initialize(null);
    }

    /**
     * Initialise avec persistence optionnelle.
     *
     * <p><b>Pré-requis :</b> {@link MiubyLib#init(org.bukkit.plugin.java.JavaPlugin)} doit avoir été
     * appelé avant cette méthode.</p>
     *
     * @param persistence implémentation de la persistence, ou {@code null}
     */
    public void initialize(@Nullable MLLogPersistence persistence) {
        if (isInitialized) {
            logger.warning("MLLogManager déjà initialisé !");
            return;
        }

        this.logger      = Logger.getLogger(MiubyLib.getPluginName());
        this.persistence = persistence;

        setupHandlers();
        suppressExternalNoise();
        if (persistence != null) loadFromPersistence();

        isInitialized = true;

        logInternal(Level.INFO, "SYSTEM", "MLLogManager initialisé");
        logInternal(Level.INFO, "SYSTEM", "  ├─ Tags activés : " + countEnabled(enabledTags) + "/" + enabledTags.size());
        logInternal(Level.INFO, "SYSTEM", "  └─ Levels activés : " + countEnabled(enabledLevels) + "/" + enabledLevels.size());
    }

    // =========================================================================
    // SETUP HANDLERS
    // =========================================================================

    /** Format : {@code [2026-05-27 18:42:01] [INFO] message\nstacktrace si présente} */
    private static final String LOG_FORMAT = "[%1$tF %1$tT] [%4$s] %5$s%6$s%n";

    private void setupHandlers() {
        try {
            System.setProperty("java.util.logging.SimpleFormatter.format", LOG_FORMAT);

            File logDir = new File(MiubyLib.getDataFolder(), "logs");
            logDir.mkdirs();

            String prefix = MiubyLib.getPluginName().toLowerCase().replace(" ", "-");

            // DEBUG : tout, stacktraces, longue rétention (10 fichiers × 10 MB)
            addFileHandler(logDir, prefix + "-debug-%g.log", 10 * 1024 * 1024, 10, logRecord -> true);

            // INFO : INFO+ (5 fichiers × 5 MB)
            addFileHandler(logDir, prefix + "-info-%g.log", 5 * 1024 * 1024, 5,
                    logRecord -> logRecord.getLevel().intValue() >= Level.INFO.intValue());

            // WARN : WARNING+ archive légère (5 fichiers × 2 MB)
            addFileHandler(logDir, prefix + "-warn-%g.log", 2 * 1024 * 1024, 5,
                    logRecord -> logRecord.getLevel().intValue() >= Level.WARNING.intValue());

            // Débranche la console Bukkit par défaut
            logger.setUseParentHandlers(false);

            // Console : WARNING+ seulement
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new SimpleFormatter());
            consoleHandler.setLevel(Level.WARNING);
            logger.addHandler(consoleHandler);

            logger.setLevel(Level.ALL);

        } catch (IOException e) {
            logger.warning("Impossible de créer les fichiers de log : " + e.getMessage());
        }
    }

    private void addFileHandler(File logDir, String pattern, int maxBytes, int maxFiles, Filter filter) throws IOException {
        FileHandler handler = new FileHandler(new File(logDir, pattern).getPath(), maxBytes, maxFiles, true);
        handler.setFormatter(new SimpleFormatter());
        handler.setLevel(Level.ALL);
        handler.setFilter(filter);
        logger.addHandler(handler);
    }

    // =========================================================================
    // FILTRE BRUIT EXTERNE
    // =========================================================================

    private void suppressExternalNoise() {
        org.apache.logging.log4j.core.Logger rootLogger =
                (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager.getRootLogger();

        rootLogger.addFilter(new AbstractFilter() {
            @Override
            public Result filter(LogEvent event) {
                String message = event.getMessage().getFormattedMessage();
                if (!isNoise(message)) return Result.NEUTRAL;
                logger.log(Level.FINE, "[External] {0}", message);
                return Result.DENY;
            }
        });
    }

    private boolean isNoise(String message) {
        if (message == null) return false;
        for (String filter : noiseFilters) {
            if (message.contains(filter)) return true;
        }
        return false;
    }

    // =========================================================================
    // LOG
    // =========================================================================

    /**
     * Émet un message si {@code tag} et {@code level} sont tous les deux activés.
     *
     * <p>Les tags non enregistrés via {@link #registerTags} sont traités comme activés
     * par défaut, ce qui permet de logger avant l'enregistrement complet des tags.</p>
     *
     * @param level   niveau JUL du message
     * @param tag     catégorie du message
     * @param message texte à logger
     */
    public void log(Level level, ILogTag tag, String message) {
        logInternal(level, tag.name(), message);
    }

    /**
     * Variante avec exception — la stacktrace complète apparaît dans {@code -debug.log}.
     *
     * @param level     niveau JUL du message
     * @param tag       catégorie du message
     * @param message   texte à logger
     * @param throwable exception dont la stacktrace sera enregistrée
     */
    public void log(Level level, ILogTag tag, String message, Throwable throwable) {
        if (!enabledTags.getOrDefault(tag.name(), true)) return;
        if (!enabledLevels.getOrDefault(level, true)) return;
        logger.log(level, "[" + formatTag(tag.name()) + "] " + message, throwable);
    }

    /**
     * Usage interne — contourne l'interface {@link ILogTag} pour les messages système.
     * Construit la chaîne de log une seule fois, partagée entre tous les handlers.
     */
    private void logInternal(Level level, String tagName, String message) {
        if (!enabledTags.getOrDefault(tagName, true)) return;
        if (!enabledLevels.getOrDefault(level, true)) return;
        logger.log(level, "[" + formatTag(tagName) + "] " + message);
    }

    /**
     * Retourne le nom formaté du tag depuis le cache.
     * Calcul Guava effectué au plus une fois par tag (à l'enregistrement ou au premier accès).
     * {@code "ALPHA_PLAYER"} → {@code "AlphaPlayer"}
     */
    private String formatTag(String tagName) {
        return formattedTagCache.computeIfAbsent(tagName, n -> CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, n));
    }

    // =========================================================================
    // GESTION DES TAGS — overloads ILogTag
    // =========================================================================

    /**
     * Inverse l'état d'activation du tag.
     *
     * @param tag le tag à basculer
     */
    public void toggleTag(ILogTag tag) {
        boolean newState = !enabledTags.getOrDefault(tag.name(), true);
        enabledTags.put(tag.name(), newState);
        if (persistence != null) persistence.saveTagState(tag.name(), newState);
    }

    /**
     * Définit l'état d'activation du tag.
     *
     * @param tag     le tag à modifier
     * @param enabled {@code true} pour activer, {@code false} pour désactiver
     */
    public void setTagEnabled(ILogTag tag, boolean enabled) {
        enabledTags.put(tag.name(), enabled);
        if (persistence != null) persistence.saveTagState(tag.name(), enabled);
    }

    /**
     * Indique si le tag est actuellement activé.
     *
     * @param tag le tag à vérifier
     * @return {@code true} si le tag est activé
     */
    public boolean isTagEnabled(ILogTag tag) {
        return enabledTags.getOrDefault(tag.name(), true);
    }

    // =========================================================================
    // GESTION DES TAGS — overloads String (utilisés par MLLogCommand)
    // =========================================================================

    /**
     * Overload String de {@link #toggleTag(ILogTag)}.
     * Utilisé par {@code MLLogCommand} pour éviter le couplage aux enums de tags des plugins.
     *
     * @param tagName nom du tag à basculer
     */
    public void toggleTag(String tagName) {
        boolean newState = !enabledTags.getOrDefault(tagName, true);
        enabledTags.put(tagName, newState);
        if (persistence != null) persistence.saveTagState(tagName, newState);
    }

    /**
     * Overload String de {@link #setTagEnabled(ILogTag, boolean)}.
     *
     * @param tagName nom du tag à modifier
     * @param enabled {@code true} pour activer, {@code false} pour désactiver
     */
    public void setTagEnabled(String tagName, boolean enabled) {
        enabledTags.put(tagName, enabled);
        if (persistence != null) persistence.saveTagState(tagName, enabled);
    }

    /**
     * Overload String de {@link #isTagEnabled(ILogTag)}.
     *
     * @param tagName nom du tag à vérifier
     * @return {@code true} si le tag est activé
     */
    public boolean isTagEnabled(String tagName) {
        return enabledTags.getOrDefault(tagName, true);
    }

    /**
     * Active tous les tags enregistrés et persiste les changements
     * si un {@link MLLogPersistence} est configuré.
     */
    public void enableAllTags() {
        enabledTags.replaceAll((tag, ignored) -> {
            if (persistence != null) persistence.saveTagState(tag, true);
            return true;
        });
    }

    /**
     * Désactive tous les tags enregistrés et persiste les changements
     * si un {@link MLLogPersistence} est configuré.
     */
    public void disableAllTags() {
        enabledTags.replaceAll((tag, ignored) -> {
            if (persistence != null) persistence.saveTagState(tag, false);
            return false;
        });
    }

    /**
     * Retourne une copie de la map {@code tagName → enabled}.
     *
     * @return copie de la map d'état des tags
     */
    public Map<String, Boolean> getAllTagStates() {
        return new HashMap<>(enabledTags);
    }

    // =========================================================================
    // GESTION DES LEVELS
    // =========================================================================

    /**
     * Inverse l'état d'activation du level.
     *
     * @param level le level JUL à basculer
     */
    public void toggleLevel(Level level) {
        boolean newState = !enabledLevels.getOrDefault(level, true);
        enabledLevels.put(level, newState);
        if (persistence != null) persistence.saveLevelState(level.getName(), newState);
    }

    /**
     * Définit l'état d'activation du level.
     *
     * @param level   le level JUL à modifier
     * @param enabled {@code true} pour activer, {@code false} pour désactiver
     */
    public void setLevelEnabled(Level level, boolean enabled) {
        enabledLevels.put(level, enabled);
        if (persistence != null) persistence.saveLevelState(level.getName(), enabled);
    }

    /**
     * Indique si le level est actuellement activé.
     *
     * @param level le level JUL à vérifier
     * @return {@code true} si le level est activé
     */
    public boolean isLevelEnabled(Level level) {
        return enabledLevels.getOrDefault(level, true);
    }

    /** Active tous les levels enregistrés. */
    public void enableAllLevels() {
        for (Level level : enabledLevels.keySet()) setLevelEnabled(level, true);
    }

    /** Désactive tous les levels enregistrés. */
    public void disableAllLevels() {
        for (Level level : enabledLevels.keySet()) setLevelEnabled(level, false);
    }

    /**
     * Retourne une copie de la map {@code Level → enabled}.
     *
     * @return copie de la map d'état des levels
     */
    public Map<Level, Boolean> getAllLevelStates() {
        return new HashMap<>(enabledLevels);
    }

    // =========================================================================
    // PRESETS
    // =========================================================================

    /** Mode PRODUCTION : seulement WARNING et SEVERE, tous les tags. */
    public void setProductionMode() {
        enableAllTags();
        setLevelEnabled(Level.INFO,    false);
        setLevelEnabled(Level.CONFIG,  false);
        setLevelEnabled(Level.FINE,    false);
        setLevelEnabled(Level.FINER,   false);
        setLevelEnabled(Level.FINEST,  false);
        setLevelEnabled(Level.WARNING, true);
        setLevelEnabled(Level.SEVERE,  true);
        logger.info("MLLogManager en mode PRODUCTION");
    }

    /** Mode DEBUG : tout activé. */
    public void setDebugMode() {
        enableAllTags();
        enableAllLevels();
        logger.info("MLLogManager en mode DEBUG");
    }

    /** Mode QUIET : seulement SEVERE. */
    public void setQuietMode() {
        enableAllTags();
        for (Level level : enabledLevels.keySet()) {
            setLevelEnabled(level, level == Level.SEVERE);
        }
        logger.severe("MLLogManager en mode QUIET");
    }

    // =========================================================================
    // PERSISTENCE
    // =========================================================================

    private void loadFromPersistence() {
        for (String tag : enabledTags.keySet()) {
            Boolean state = persistence.getTagState(tag);
            if (state != null) enabledTags.put(tag, state);
        }
        for (Level level : enabledLevels.keySet()) {
            Boolean state = persistence.getLevelState(level.getName());
            if (state != null) enabledLevels.put(level, state);
        }
    }

    // =========================================================================
    // UTILITAIRES
    // =========================================================================

    private <T> int countEnabled(Map<T, Boolean> map) {
        int count = 0;
        for (boolean b : map.values()) { if (b) count++; }
        return count;
    }
}
