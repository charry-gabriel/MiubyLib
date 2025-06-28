package fr.miuby.lib.world;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Minimal registry to keep track of your worlds at runtime. It deliberately
 * keeps a tiny surface-area: register, retrieve by id or name, a few helpers.
 */
public class WorldRegistry {
    private static final Map<UUID, MLWorld> worlds = new HashMap<>();
    private static final Map<String, MLWorld> byName = new HashMap<>();
    private static final Map<WorldType, MLWorld> byType = new HashMap<>();

    private WorldRegistry() {}

    public static void register(MLWorld world) {
        worlds.put(world.getUUID(), world);
        byName.put(world.getName(), world);
        byType.put(world.getType(), world);
    }

    public static MLWorld get(UUID uuid) {
        return worlds.get(uuid);
    }

    public static MLWorld get(String name) {
        return byName.get(name);
    }

    public static MLWorld get(WorldType type) {
        return byType.get(type);
    }

    public static Collection<MLWorld> getAll() {
        return new ArrayList<>(worlds.values());
    }

    public static boolean isPlayerInRegisteredWorld(Player player) {
        return worlds.containsKey(player.getWorld().getUID());
    }

    public static MLWorld get(Player player) {
        return worlds.get(player.getWorld().getUID());
    }
}
