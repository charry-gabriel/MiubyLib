package fr.miuby.lib;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.logging.Logger;

public class MiubyLib {
    private static JavaPlugin plugin;

    public static void init(JavaPlugin pluginInstance) {
        if (plugin != null)
            throw new IllegalStateException("MiubyLib est déjà initialisé.");
        plugin = pluginInstance;
    }

    // -------------------------------------------------------------------------
    // Scheduler
    // -------------------------------------------------------------------------

    /** Exécute {@code task} au prochain tick (sync). */
    public static BukkitTask runTask(Runnable task) {
        return Bukkit.getScheduler().runTask(plugin, task);
    }

    /** Exécute {@code task} après {@code delay} ticks (sync). */
    public static BukkitTask runLater(Runnable task, long delay) {
        return Bukkit.getScheduler().runTaskLater(plugin, task, delay);
    }

    /**
     * Exécute {@code task} toutes les {@code period} ticks après {@code delay} ticks (sync).
     *
     * @return le {@link BukkitTask} — à annuler avec {@code task.cancel()}
     */
    public static BukkitTask runTaskTimer(Runnable task, long delay, long period) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
    }

    /** Exécute {@code task} hors du thread principal (async — pour DB/IO). */
    public static BukkitTask runAsync(Runnable task) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    // -------------------------------------------------------------------------
    // Plugin
    // -------------------------------------------------------------------------

    public static Logger getLogger() {
        return plugin.getLogger();
    }

    /** Nom du plugin tel que déclaré dans {@code plugin.yml}. */
    public static String getPluginName() {
        return plugin != null ? plugin.getName() : "MiubyLib";
    }

    /** Dossier de données du plugin ({@code plugins/<NomDuPlugin>/}). */
    public static File getDataFolder() {
        return plugin.getDataFolder();
    }

    public static void callEvent(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }
}
