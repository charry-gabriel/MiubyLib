package fr.miuby.lib.message;

import fr.miuby.lib.log.ILogTag;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.resource.MLResourceManager;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Service de traduction générique : YAML + MiniMessage, mono- ou multi-langue.
 *
 * <p>Les templates vivent dans {@code <resourceFolder>/<locale>.yml} (dossier de données
 * du plugin, déployés via {@link MLResourceManager#deployFolder}). Une clé est une simple
 * chaîne du type {@code "categorie.sous_categorie.nom"} ; toute clé utilisée dans le code
 * doit exister dans le fichier de la locale par défaut (et idéalement dans les autres).</p>
 *
 * <h3>Clé manquante — pas de crash</h3>
 * <p>Si une clé est absente de la locale demandée ET de la locale par défaut, l'appelant
 * reçoit un message d'avertissement bien visible (template overridable, contient le nom
 * exact de la clé) et un warning est loggé une seule fois par clé (tag {@code MESSAGE}).</p>
 *
 * <h3>Format des templates (MiniMessage)</h3>
 * <ul>
 *   <li>{@code {0}}, {@code {1}}… — remplacements de chaînes simples (échappés pour MiniMessage)</li>
 *   <li>{@code <name>} — {@link TagResolver} Adventure (ex. composant coloré du nom d'un métier)</li>
 * </ul>
 *
 * <h3>Mode mono-langue</h3>
 * <p>Si {@code locales} ne contient qu'un seul code, {@link #resolveLanguage(Player)} retourne
 * toujours ce code (sans lire {@code player.locale()}, {@code forceDefault} est ignoré) et
 * {@link #broadcast} devient un simple envoi à tous les joueurs en ligne dans cette langue.</p>
 *
 * <h3>Fallback</h3>
 * <p>locale demandée → locale par défaut ({@code locales.get(0)}) → message "clé manquante".</p>
 *
 * <h3>API</h3>
 * <pre>{@code
 * MLMessageService msg = new MLMessageService(plugin, "lang", List.of("fr", "en"), false);
 *
 * // Message simple
 * player.sendMessage(msg.text(player, "world.locked"));
 *
 * // Avec args positionnels
 * player.sendMessage(msg.text(player, "grave.created", x, y, z, worldName));
 *
 * // Avec TagResolvers (composants Adventure)
 * player.sendMessage(msg.text(player, "job.level_up.broadcast",
 *     Placeholder.unparsed("player", pseudo),
 *     Placeholder.component("job", job.toComponent())
 * ));
 *
 * // Broadcast à tous les joueurs dans leur langue
 * msg.broadcast("world.level_up.broadcast", oldLevel, newLevel);
 *
 * // Pour un CommandSender (console incluse)
 * String locale = msg.resolveOrDefault(sender);
 * sender.sendMessage(msg.text(locale, "cmd.role.assigned", roleName));
 * }</pre>
 */
public class MLMessageService {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** Tag de log utilisé par toutes les méthodes de cette classe. */
    private static final ILogTag TAG = () -> "MESSAGE";

    /**
     * Message renvoyé quand une clé est absente de la locale demandée ET de la locale
     * par défaut. Volontairement très visible (rouge, gras) et contient la clé exacte
     * à ajouter. Overridable via le constructeur à 5 arguments.
     */
    private static final String DEFAULT_MISSING_KEY_TEMPLATE =
            "<red><bold>⚠ Missing translation (<white>{0}<red>)";

    private final String resourceFolder;
    private final List<String> locales;
    /**
     * -- GETTER --
     * Locale par défaut, c'est-à-dire
     *  passé au constructeur.
     */
    @Getter
    private final String defaultLocale;
    private final boolean forceDefault;
    private final boolean mono;
    private final String missingKeyTemplate;

    private final Map<String, Map<String, String>> translations = new HashMap<>();

    /** Clés déjà signalées dans les logs, pour n'avertir qu'une seule fois par clé. */
    private final Set<String> loggedMissingKeys = ConcurrentHashMap.newKeySet();

    // =========================================================================
    // Initialisation
    // =========================================================================

    /**
     * @param plugin        instance du plugin hôte
     * @param resourceFolder dossier (dans les ressources du plugin et le dossier de données)
     *                        contenant les fichiers {@code <locale>.yml}, ex. {@code "lang"}
     * @param locales        codes de locale supportés, ex. {@code List.of("fr", "en")}.
     *                        Le premier élément est la locale par défaut. Non vide.
     * @param forceDefault   si {@code true}, {@link #resolveLanguage(Player)} retourne toujours
     *                        la locale par défaut, indépendamment du client du joueur.
     *                        Ignoré si {@code locales.size() == 1} (mode mono-langue).
     */
    public MLMessageService(JavaPlugin plugin, String resourceFolder, List<String> locales, boolean forceDefault) {
        this(plugin, resourceFolder, locales, forceDefault, DEFAULT_MISSING_KEY_TEMPLATE);
    }

    /**
     * Variante permettant de personnaliser le message "clé manquante" (doit contenir
     * {@code {0}}, remplacé par la clé concernée).
     *
     * @see #MLMessageService(JavaPlugin, String, List, boolean)
     */
    public MLMessageService(JavaPlugin plugin, String resourceFolder, List<String> locales,
                            boolean forceDefault, String missingKeyTemplate) {
        if (locales == null || locales.isEmpty())
            throw new IllegalArgumentException("[MLMessageService] La liste de locales ne peut pas être vide.");

        this.resourceFolder = resourceFolder;
        this.locales = List.copyOf(locales);
        this.defaultLocale = this.locales.getFirst();
        this.forceDefault = forceDefault;
        this.mono = this.locales.size() == 1;
        this.missingKeyTemplate = missingKeyTemplate;

        MLResourceManager.deployFolder(plugin, resourceFolder);

        for (String locale : this.locales) {
            translations.put(locale, loadFile(plugin, locale));
        }

        StringBuilder summary = new StringBuilder();
        for (String locale : this.locales) {
            if (!summary.isEmpty()) summary.append(", ");
            summary.append(locale.toUpperCase()).append('=').append(translations.get(locale).size()).append(" clés");
        }
        MLLogManager.getInstance().log(Level.INFO, TAG,
                "Initialisé (" + resourceFolder + "/) — locale par défaut : " + defaultLocale
                        + (mono ? " (mono-langue)" : forceDefault ? " (forcée pour tous)" : " (détection par client)")
                        + " — " + summary);
    }

    private Map<String, String> loadFile(JavaPlugin plugin, String localeCode) {
        File file = new File(plugin.getDataFolder(), resourceFolder + "/" + localeCode + ".yml");
        if (!file.exists()) {
            MLLogManager.getInstance().log(Level.WARNING, TAG,
                    "Fichier manquant : " + resourceFolder + "/" + localeCode + ".yml");
            return Collections.emptyMap();
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        Map<String, String> map = new HashMap<>();
        for (String key : cfg.getKeys(true)) {
            if (cfg.isString(key)) map.put(key, cfg.getString(key));
        }
        return Collections.unmodifiableMap(map);
    }

    // =========================================================================
    // Résolution de la langue
    // =========================================================================

    /**
     * Détermine la locale à utiliser pour ce joueur.
     * <ul>
     *   <li>Mode mono-langue ({@code locales.size() == 1}) → retourne toujours cette locale.</li>
     *   <li>{@code forceDefault: true} → retourne toujours {@link #getDefaultLocale()}.</li>
     *   <li>Sinon → {@code player.locale().getLanguage()}, repli sur la locale par défaut
     *       si la langue du client n'est pas supportée.</li>
     * </ul>
     */
    public String resolveLanguage(Player player) {
        if (mono || player == null || forceDefault) return defaultLocale;
        String code = player.locale().getLanguage();
        for (String locale : locales) {
            if (locale.equalsIgnoreCase(code)) return locale;
        }
        return defaultLocale;
    }

    /** Résout la locale d'un {@link CommandSender}, en tombant sur la locale par défaut si c'est la console. */
    public String resolveOrDefault(CommandSender sender) {
        return sender instanceof Player p ? resolveLanguage(p) : defaultLocale;
    }

    // =========================================================================
    // text() — composant pour un joueur
    // =========================================================================

    /** Résout un message pour ce joueur, sans arguments. */
    public Component text(Player player, String key) {
        return text(resolveLanguage(player), key);
    }

    /** Résout un message avec placeholders positionnels {@code {0}}, {@code {1}}... */
    public Component text(Player player, String key, Object... args) {
        return text(resolveLanguage(player), key, args);
    }

    /** Résout un message avec TagResolvers Adventure (placeholders {@code <name>}). */
    public Component text(Player player, String key, TagResolver... resolvers) {
        return text(resolveLanguage(player), key, resolvers);
    }

    // =========================================================================
    // text() — composant pour une locale donnée
    // =========================================================================

    /** Résout un message dans une locale précise, sans arguments. */
    public Component text(String localeCode, String key) {
        return MM.deserialize(resolve(localeCode, key));
    }

    /** Résout un message avec placeholders positionnels {@code {0}}, {@code {1}}... */
    public Component text(String localeCode, String key, Object... args) {
        String template = resolve(localeCode, key);
        for (int i = 0; i < args.length; i++) {
            template = template.replace("{" + i + "}", MM.escapeTags(String.valueOf(args[i])));
        }
        return MM.deserialize(template);
    }

    /** Résout un message avec TagResolvers Adventure (placeholders {@code <name>}). */
    public Component text(String localeCode, String key, TagResolver... resolvers) {
        String template = resolve(localeCode, key);
        return resolvers.length == 0 ? MM.deserialize(template) : MM.deserialize(template, resolvers);
    }

    // =========================================================================
    // broadcast() — envoi à tous les joueurs en ligne dans leur langue
    // =========================================================================

    /** Envoie un message à tous les joueurs en ligne, chacun dans sa langue. */
    public void broadcast(String key) {
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(text(p, key));
    }

    /** Envoie avec placeholders positionnels. */
    public void broadcast(String key, Object... args) {
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(text(p, key, args));
    }

    /**
     * Envoie avec TagResolvers partagés (même valeur pour tous les joueurs).
     * Ne pas utiliser si la valeur d'un resolver dépend du joueur.
     */
    public void broadcast(String key, TagResolver... resolvers) {
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(text(p, key, resolvers));
    }

    // =========================================================================
    // getString() / getDefaultLocale()
    // =========================================================================

    /** Retourne la chaîne brute (non parsée) d'une clé, utile pour la passer en arg positionnel. */
    public String getString(String localeCode, String key) {
        return resolve(localeCode, key);
    }

    // =========================================================================
    // Résolution interne du template
    // =========================================================================

    /**
     * Résout le template brut pour {@code key} dans {@code localeCode}, avec repli sur la
     * locale par défaut, puis sur le message "clé manquante" si la clé n'existe nulle part.
     */
    private String resolve(String localeCode, String key) {
        Map<String, String> map = translations.get(localeCode);
        if (map != null) {
            String val = map.get(key);
            if (val != null) return val;
        }
        // Repli sur la locale par défaut si la clé est absente de la locale demandée
        if (!defaultLocale.equals(localeCode)) {
            Map<String, String> def = translations.get(defaultLocale);
            if (def != null) {
                String val = def.get(key);
                if (val != null) return val;
            }
        }
        // Clé absente partout : on avertit (log une fois) et on renvoie un message
        // visible contenant la clé, pour que l'appelant puisse signaler le bug.
        if (loggedMissingKeys.add(key)) {
            MLLogManager.getInstance().log(Level.WARNING, TAG,
                    "Clé de traduction manquante : \"" + key + "\" — ajoutez-la dans "
                            + resourceFolder + "/" + defaultLocale + ".yml.");
        }
        return missingKeyTemplate.replace("{0}", MM.escapeTags(key));
    }
}