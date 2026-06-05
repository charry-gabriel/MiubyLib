package fr.miuby.lib;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.logging.Logger;

/**
 * Point d'entrée statique de MiubyLib.
 *
 * <p>Initialiser une seule fois via {@link #init(JavaPlugin)} lors du {@code onEnable()} du plugin,
 * avant toute utilisation des autres classes de la librairie.</p>
 */
public class MiubyLib {
    private static JavaPlugin plugin;

    /**
     * Initialise MiubyLib avec l'instance du plugin.
     * Doit être appelé une seule fois dans {@code onEnable()}.
     *
     * @param pluginInstance instance du plugin à utiliser
     * @throws IllegalStateException si MiubyLib est déjà initialisé
     */
    public static void init(JavaPlugin pluginInstance) {
        if (plugin != null)
            throw new IllegalStateException("MiubyLib est déjà initialisé.");
        plugin = pluginInstance;
    }

    // -------------------------------------------------------------------------
    // Scheduler
    // -------------------------------------------------------------------------

    /**
     * Exécute {@code task} au prochain tick (sync).
     *
     * @param task tâche à exécuter
     * @return le {@link BukkitTask} associé
     */
    public static BukkitTask runTask(Runnable task) {
        return Bukkit.getScheduler().runTask(plugin, task);
    }

    /**
     * Exécute {@code task} après {@code delay} ticks (sync).
     *
     * @param task  tâche à exécuter
     * @param delay délai en ticks avant l'exécution
     * @return le {@link BukkitTask} associé
     */
    public static BukkitTask runLater(Runnable task, long delay) {
        return Bukkit.getScheduler().runTaskLater(plugin, task, delay);
    }

    /**
     * Exécute {@code task} toutes les {@code period} ticks après {@code delay} ticks (sync).
     *
     * @param task   tâche à exécuter périodiquement
     * @param delay  délai initial en ticks
     * @param period intervalle en ticks entre chaque exécution
     * @return le {@link BukkitTask} — à annuler avec {@code task.cancel()}
     */
    public static BukkitTask runTaskTimer(Runnable task, long delay, long period) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
    }

    /**
     * Exécute {@code task} hors du thread principal (async — pour DB/IO).
     *
     * @param task tâche à exécuter de manière asynchrone
     * @return le {@link BukkitTask} associé
     */
    public static BukkitTask runAsync(Runnable task) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    // -------------------------------------------------------------------------
    // Plugin
    // -------------------------------------------------------------------------

    /**
     * Retourne le logger Bukkit du plugin.
     *
     * @return le {@link Logger} du plugin
     */
    public static Logger getLogger() {
        return plugin.getLogger();
    }

    /**
     * Nom du plugin tel que déclaré dans {@code plugin.yml}.
     *
     * @return le nom du plugin, ou {@code "MiubyLib"} si non initialisé
     */
    public static String getPluginName() {
        return plugin != null ? plugin.getName() : "MiubyLib";
    }

    /**
     * Dossier de données du plugin ({@code plugins/<NomDuPlugin>/}).
     *
     * @return le dossier de données du plugin
     */
    public static File getDataFolder() {
        return plugin.getDataFolder();
    }

    /**
     * Déclenche l'événement Bukkit donné via le {@link org.bukkit.plugin.PluginManager}.
     *
     * @param event l'événement à déclencher
     */
    public static void callEvent(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }
}
