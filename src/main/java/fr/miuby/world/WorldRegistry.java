package fr.miuby.world;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Minimal registry to keep track of your worlds at runtime. It deliberately
 * keeps a tiny surface-area: register, retrieve by id or name, a few helpers.
 */
public class WorldRegistry {

    private final Map<UUID, MiubyWorld> worlds = new HashMap<>();
    private final Map<String, MiubyWorld> byName = new HashMap<>();
    private final Map<WorldType, MiubyWorld> byType = new HashMap<>();

    public void register(MiubyWorld world) {
        worlds.put(world.getUUID(), world);
        byName.put(world.getName(), world);
        byType.put(world.getType(), world);
    }

    public MiubyWorld get(UUID uuid) {
        return worlds.get(uuid);
    }

    public MiubyWorld get(String name) {
        return byName.get(name);
    }

    public MiubyWorld get(WorldType type) {
        return byType.get(type);
    }

    public Collection<MiubyWorld> getAll() {
        return worlds.values();
    }

    public boolean isPlayerInRegisteredWorld(Player player) {
        return worlds.containsKey(player.getWorld().getUID());
    }

    public MiubyWorld get(Player player) {
        return worlds.get(player.getWorld().getUID());
    }
}
