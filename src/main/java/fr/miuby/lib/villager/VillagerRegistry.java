package fr.miuby.lib.villager;

import fr.miuby.lib.utils.MultiKeyRegistry;

import java.util.Collection;
import java.util.UUID;

/**
 * Registry statique des {@link MLVillager} enregistrés.
 *
 * <p>Chaque villager est indexé par son UUID Bukkit et son {@code nameId}.</p>
 */
public class VillagerRegistry {
    private static final MultiKeyRegistry<MLVillager> INSTANCE = new MultiKeyRegistry<>();

    private VillagerRegistry() {}

    /**
     * Enregistre {@code villager} sous son UUID Bukkit et son {@code nameId}.
     *
     * <p>Idempotent : un appel sur un villager déjà enregistré est silencieusement ignoré.
     * Cela permet de cumuler l'auto-enregistrement de {@link MLVillager#onInitialized()} et
     * un éventuel appel manuel depuis un listener sans risque de doublon.</p>
     *
     * @param villager le villager à enregistrer
     * @throws IllegalStateException si l'entité Bukkit du villager n'a pas encore été créée
     */
    public static void register(MLVillager villager) {
        if (villager.getVillager() == null)
            throw new IllegalStateException(
                    "Impossible d'enregistrer le villager " + villager.getNameId() + " : entité Bukkit non créée.");

        if (INSTANCE.contains(villager.getNameId())) return;

        INSTANCE.register(villager, villager.getVillager().getUniqueId(), villager.getNameId());
    }

    /**
     * Retire {@code villager} du registry (toutes ses clés).
     *
     * @param villager le villager à retirer
     */
    public static void unregister(MLVillager villager) {
        INSTANCE.unregister(villager);
    }

    /**
     * Retourne le villager associé à cet UUID Bukkit.
     *
     * @param uuid l'UUID de l'entité Bukkit
     * @return le {@link MLVillager} correspondant, ou {@code null}
     */
    public static MLVillager get(UUID uuid) {
        return INSTANCE.get(uuid);
    }

    /**
     * Retourne le villager associé à ce {@code nameId}.
     *
     * @param nameId identifiant du villager
     * @return le {@link MLVillager} correspondant, ou {@code null}
     */
    public static MLVillager get(String nameId) {
        return INSTANCE.get(nameId);
    }

    /**
     * Retourne tous les villagers enregistrés.
     *
     * @return collection dédupliquée de tous les {@link MLVillager}
     */
    public static Collection<MLVillager> getAll() {
        return INSTANCE.getAll();
    }

    /**
     * Indique si un villager avec cet UUID est enregistré.
     *
     * @param uuid l'UUID à vérifier
     * @return {@code true} si un villager est enregistré avec cet UUID
     */
    public static boolean contains(UUID uuid) {
        return INSTANCE.contains(uuid);
    }

    /**
     * Indique si un villager avec ce {@code nameId} est enregistré.
     *
     * @param nameId l'identifiant à vérifier
     * @return {@code true} si un villager est enregistré avec ce nameId
     */
    public static boolean contains(String nameId) {
        return INSTANCE.contains(nameId);
    }
}
