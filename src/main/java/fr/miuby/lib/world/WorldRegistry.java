package fr.miuby.lib.world;

import fr.miuby.lib.utils.MultiKeyRegistry;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

public class WorldRegistry {
    private static final MultiKeyRegistry<MLWorld> INSTANCE = new MultiKeyRegistry<>();

    private WorldRegistry() {}

    /** Enregistre {@code world} sous son UUID Bukkit, son nom d'affichage et son {@link WorldType}. */
    public static void register(MLWorld world) {
        INSTANCE.register(world, world.getUUID(), world.getName(), world.getType());
    }

    /** Retire {@code world} du registry (toutes ses clés). */
    public static void unregister(MLWorld world) {
        INSTANCE.unregister(world);
    }

    public static MLWorld get(UUID uuid) {
        return INSTANCE.get(uuid);
    }

    public static MLWorld get(String name) {
        return INSTANCE.get(name);
    }

    public static MLWorld get(WorldType type) {
        return INSTANCE.get(type);
    }

    /** Retourne le {@link MLWorld} dans lequel se trouve {@code player}, ou {@code null}. */
    public static MLWorld get(Player player) {
        return INSTANCE.get(player.getWorld().getUID());
    }

    public static Collection<MLWorld> getAll() {
        return INSTANCE.getAll();
    }

    public static boolean isPlayerInRegisteredWorld(Player player) {
        return INSTANCE.contains(player.getWorld().getUID());
    }
}
