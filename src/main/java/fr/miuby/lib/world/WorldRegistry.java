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
    private static final Map<UUID, MiubyWorld> worlds = new HashMap<>();
    private static final Map<String, MiubyWorld> byName = new HashMap<>();
    private static final Map<WorldType, MiubyWorld> byType = new HashMap<>();

    private WorldRegistry() {}

    public static void register(MiubyWorld world) {
        worlds.put(world.getUUID(), world);
        byName.put(world.getName(), world);
        byType.put(world.getType(), world);
    }

    public static MiubyWorld get(UUID uuid) {
        return worlds.get(uuid);
    }

    public static MiubyWorld get(String name) {
        return byName.get(name);
    }

    public static MiubyWorld get(WorldType type) {
        return byType.get(type);
    }

    public static Collection<MiubyWorld> getAll() {
        return new ArrayList<>(worlds.values());
    }

    public static boolean isPlayerInRegisteredWorld(Player player) {
        return worlds.containsKey(player.getWorld().getUID());
    }

    public static MiubyWorld get(Player player) {
        return worlds.get(player.getWorld().getUID());
    }
}
