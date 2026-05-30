package fr.miuby.lib.villager;

import fr.miuby.lib.utils.MultiKeyRegistry;

import java.util.Collection;
import java.util.UUID;

public class VillagerRegistry {
    private static final MultiKeyRegistry<MLVillager> INSTANCE = new MultiKeyRegistry<>();

    private VillagerRegistry() {}

    /**
     * Enregistre {@code villager} sous son UUID Bukkit et son {@code nameId}.
     *
     * <p>Idempotent : un appel sur un villager déjà enregistré est silencieusement ignoré.
     * Cela permet de cumuler l'auto-enregistrement de {@link MLVillager#onInitialized()} et
     * un éventuel appel manuel depuis un listener sans risque de doublon.</p>
     */
    public static void register(MLVillager villager) {
        if (villager.getVillager() == null)
            throw new IllegalStateException(
                    "Impossible d'enregistrer le villager " + villager.getNameId() + " : entité Bukkit non créée.");

        if (INSTANCE.contains(villager.getNameId())) return;

        INSTANCE.register(villager, villager.getVillager().getUniqueId(), villager.getNameId());
    }

    /** Retire {@code villager} du registry (toutes ses clés). */
    public static void unregister(MLVillager villager) {
        INSTANCE.unregister(villager);
    }

    public static MLVillager get(UUID uuid) {
        return INSTANCE.get(uuid);
    }

    public static MLVillager get(String nameId) {
        return INSTANCE.get(nameId);
    }

    public static Collection<MLVillager> getAll() {
        return INSTANCE.getAll();
    }

    public static boolean contains(UUID uuid) {
        return INSTANCE.contains(uuid);
    }

    public static boolean contains(String nameId) {
        return INSTANCE.contains(nameId);
    }
}
