package fr.miuby.lib.villager;

import fr.miuby.lib.utils.MultiKeyRegistry;

import java.util.Collection;
import java.util.UUID;

public class VillagerRegistry {
    private static final MultiKeyRegistry<MLVillager> INSTANCE = new MultiKeyRegistry<>();

    private VillagerRegistry() {}

    public static void register(MLVillager villager) {
        if (villager.getVillager() == null)
            throw new IllegalStateException("Unable to register villager " + villager.getNameId() + ": Villager not spawned.");

        INSTANCE.register(villager, villager.getVillager().getUniqueId(), villager.getNameId());
    }

    public static MLVillager get(UUID uuid) {
        return INSTANCE.get(uuid);
    }

    public static MLVillager get(String name) {
        return INSTANCE.get(name);
    }

    public static Collection<MLVillager> getAll() {
        return INSTANCE.getAll();
    }

    public static boolean contains(UUID uuid) {
        return INSTANCE.contains(uuid);
    }

    public static boolean contains(String name) {
        return INSTANCE.contains(name);
    }
}

