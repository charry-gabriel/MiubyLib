package fr.miuby.lib.world;

import fr.miuby.lib.utils.MultiKeyRegistry;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

/**
 * Registry statique des {@link MLWorld} enregistrés.
 *
 * <p>Chaque monde est indexé par son UUID Bukkit, son nom d'affichage et son {@link WorldType}.</p>
 */
public class WorldRegistry {
    private static final MultiKeyRegistry<MLWorld> INSTANCE = new MultiKeyRegistry<>();

    private WorldRegistry() {}

    /**
     * Enregistre {@code world} sous son UUID Bukkit, son nom d'affichage et son {@link WorldType}.
     *
     * @param world le monde à enregistrer
     */
    public static void register(MLWorld world) {
        INSTANCE.register(world, world.getUUID(), world.getName(), world.getType());
    }

    /**
     * Retire {@code world} du registry (toutes ses clés).
     *
     * @param world le monde à retirer
     */
    public static void unregister(MLWorld world) {
        INSTANCE.unregister(world);
    }

    /**
     * Retourne le monde associé à cet UUID.
     *
     * @param uuid l'UUID du monde Bukkit
     * @return le {@link MLWorld} correspondant, ou {@code null}
     */
    public static MLWorld get(UUID uuid) {
        return INSTANCE.get(uuid);
    }

    /**
     * Retourne le monde associé à ce nom d'affichage.
     *
     * @param name le nom d'affichage du monde
     * @return le {@link MLWorld} correspondant, ou {@code null}
     */
    public static MLWorld get(String name) {
        return INSTANCE.get(name);
    }

    /**
     * Retourne le monde associé à ce type.
     *
     * @param type le type du monde
     * @return le {@link MLWorld} correspondant, ou {@code null}
     */
    public static MLWorld get(WorldType type) {
        return INSTANCE.get(type);
    }

    /**
     * Retourne le {@link MLWorld} dans lequel se trouve {@code player}, ou {@code null}.
     *
     * @param player le joueur dont on cherche le monde
     * @return le {@link MLWorld} correspondant, ou {@code null} si non enregistré
     */
    public static MLWorld get(Player player) {
        return INSTANCE.get(player.getWorld().getUID());
    }

    /**
     * Retourne tous les mondes enregistrés.
     *
     * @return collection dédupliquée de tous les {@link MLWorld}
     */
    public static Collection<MLWorld> getAll() {
        return INSTANCE.getAll();
    }

    /**
     * Indique si le joueur se trouve dans un monde enregistré dans ce registry.
     *
     * @param player le joueur à vérifier
     * @return {@code true} si le monde du joueur est enregistré
     */
    public static boolean isPlayerInRegisteredWorld(Player player) {
        return INSTANCE.contains(player.getWorld().getUID());
    }
}
