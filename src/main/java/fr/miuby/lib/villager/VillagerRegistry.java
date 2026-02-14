package fr.miuby.lib.villager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VillagerRegistry {
    private static final Map<UUID, MLVillager> villagers = new HashMap<>();
    private static final Map<String, MLVillager> byName = new HashMap<>();

    private VillagerRegistry() {}

    public static void register(MLVillager villager) {
        if (villager.getVillager() == null)
            throw new IllegalStateException("Unable to register villager " + villager.getNameId() + ": Villager not spawned.");

        UUID uuid = villager.getVillager().getUniqueId();
        String name = villager.getNameId();

        if (villagers.containsKey(uuid))
            throw new IllegalArgumentException("Villager uuid already registered!");

        if (byName.containsKey(name))
            throw new IllegalArgumentException("Villager name already registered!");

        villagers.put(uuid, villager);
        byName.put(name, villager);
    }

    public static MLVillager get(UUID uuid) {
        return villagers.get(uuid);
    }

    public static MLVillager get(String name) {
        return byName.get(name);
    }

    public static Collection<MLVillager> getAll() {
        return new ArrayList<>(villagers.values());
    }
}

