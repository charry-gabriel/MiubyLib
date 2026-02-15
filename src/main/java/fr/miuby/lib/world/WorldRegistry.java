package fr.miuby.lib.world;

import fr.miuby.lib.utils.MultiKeyRegistry;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

public class WorldRegistry {
    private static final MultiKeyRegistry<MLWorld> INSTANCE = new MultiKeyRegistry<>();

    private WorldRegistry() {}

    public static void register(MLWorld world) {
        INSTANCE.register(world, world.getUUID(), world.getName(), world.getType());
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

    public static Collection<MLWorld> getAll() {
        return INSTANCE.getAll();
    }

    public static boolean isPlayerInRegisteredWorld(Player player) {
        return INSTANCE.contains(player.getWorld().getUID());
    }

    public static MLWorld get(Player player) {
        return INSTANCE.get(player.getWorld().getUID());
    }
}
