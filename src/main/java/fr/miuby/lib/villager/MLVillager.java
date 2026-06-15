package fr.miuby.lib.villager;

import fr.miuby.lib.MiubyLib;
import fr.miuby.lib.log.ILogTag;
import fr.miuby.lib.log.MLLogManager;
import lombok.Getter;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * <p><b>Pattern d'utilisation :</b></p>
 * <ol>
 *   <li>Étendre {@code MLVillager}</li>
 *   <li>Implémenter {@link #loadData()} — charge depuis fichier/DB ; retourner {@code null} si premier spawn</li>
 *   <li>Implémenter {@link #saveData()} — persiste les données</li>
 *   <li>Implémenter {@link #createDefaultData()} — données par défaut au premier spawn</li>
 *   <li>Surcharger {@link #onInitialized()} si besoin de logique custom après chargement</li>
 *   <li>Surcharger {@link #onDestroy()} pour du cleanup avant suppression</li>
 * </ol>
 *
 * <p>Le système gère automatiquement :</p>
 * <ul>
 *   <li>Création de l'entité Bukkit</li>
 *   <li>Retry si le chunk n'est pas chargé (10 tentatives × 10 ticks)</li>
 *   <li>Respawn automatique si introuvable après les retries</li>
 *   <li>Persistence (UUID + Location)</li>
 *   <li>Configuration de base (AI off, collidable off, persistent on)</li>
 *   <li><b>Enregistrement automatique</b> dans {@link VillagerRegistry}</li>
 * </ul>
 *
 * <p><b>IMPORTANT :</b> Toujours créer via {@code MLVillager.spawn(Constructor::new)}, jamais {@code new}.</p>
 */
@Getter
public abstract class MLVillager {
    private static final ILogTag TAG = () -> "VILLAGER";

    protected final String nameId;
    private final Villager.Type type;
    private final Villager.Profession profession;
    private Villager villager;

    protected TextComponent displayName;
    protected Inventory inventory;

    private MLVillagerData villagerData;

    public MLVillager(String nameId, Villager.Type type, Villager.Profession profession) {
        this.nameId = nameId;
        this.type = type;
        this.profession = profession;
    }

    // -------------------------------------------------------------------------
    // API publique
    // -------------------------------------------------------------------------

    /**
     * Crée et initialise une instance de villager.
     * Doit toujours être utilisé à la place de {@code new}.
     *
     * @param <T>         type concret du villager
     * @param constructor fournisseur qui instancie le villager
     * @return l'instance initialisée et prête à l'emploi
     */
    public static <T extends MLVillager> T spawn(Supplier<T> constructor) {
        T villager = constructor.get();
        villager.init();
        return villager;
    }

    /**
     * Retourne l'entité Bukkit de manière null-safe.
     * Effectue un relookup par UUID si la référence locale est invalide (monde rechargé, etc.).
     *
     * @return l'entité Bukkit {@link Villager}, jamais {@code null}
     * @throws IllegalStateException si l'UUID du villager est introuvable
     */
    public Villager getVillager() {
        if (this.villager != null && this.villager.isValid() && !this.villager.isDead())
            return this.villager;

        UUID uuid = this.villager == null ? null : this.villager.getUniqueId();
        if (uuid == null)
            throw new IllegalStateException("Villager " + this.nameId + " est null.");

        Entity lookup = Bukkit.getEntity(uuid);
        if (lookup instanceof Villager live) {
            this.villager = live;
            return live;
        }

        return villager;
    }

    /**
     * Détruit le villager : le désenregistre de {@link VillagerRegistry},
     * appelle {@link #onDestroy()}, puis supprime l'entité Bukkit.
     */
    public void destroy() {
        VillagerRegistry.unregister(this);
        onDestroy();
        if (villager != null) {
            villager.remove();
        }
    }

    // -------------------------------------------------------------------------
    // Méthodes abstraites à implémenter
    // -------------------------------------------------------------------------

    /**
     * Charge les données persistées. Retourner {@code null} s'il s'agit du premier spawn.
     *
     * @return les données chargées, ou {@code null} au premier spawn
     */
    protected abstract @Nullable MLVillagerData loadData();

    /**
     * Persiste les données du villager (appelé après le premier spawn).
     */
    protected abstract void saveData();

    /**
     * Retourne les données par défaut pour le premier spawn.
     *
     * @return les données initiales du villager
     */
    protected abstract MLVillagerData createDefaultData();

    // -------------------------------------------------------------------------
    // Hooks overridables
    // -------------------------------------------------------------------------

    /**
     * Appelé une fois le villager prêt (entité Bukkit chargée).
     * La version de base applique le displayName, crée l'inventaire, enregistre dans
     * {@link VillagerRegistry} et fire {@link VillagerLoadedEvent}.
     * Toujours appeler {@code super.onInitialized()} en premier.
     */
    protected void onInitialized() {
        getVillager().customName(getDisplayName());
        createInventory();
        VillagerRegistry.register(this);
        MiubyLib.callEvent(new VillagerLoadedEvent(this));
    }

    /**
     * Appelé lors de {@link #destroy()}, avant la suppression de l'entité Bukkit.
     * Override pour libérer des ressources custom (inventaires, tasks, etc.).
     */
    protected void onDestroy() {}

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    protected void createInventory() {
        this.inventory = this.getVillager().getInventory();
    }

    public final void init() {
        villagerData = this.loadData();
        if (villagerData == null) {
            villagerData = createDefaultData();
            spawnVillager();
            saveData();
            onInitialized();
        } else {
            findVillager(0);
        }
    }

    private void spawnVillager() {
        if (villagerData == null || villagerData.getLocation().getWorld() == null) {
            throw new IllegalStateException(
                    "Impossible de spawner le villager " + nameId + " : Location ou World manquant dans villagerData.");
        }

        if (!villagerData.getLocation().getChunk().isLoaded()) {
            villagerData.getLocation().getChunk().load();
        }

        this.villager = (Villager) villagerData.getLocation().getWorld()
                .spawnEntity(villagerData.getLocation(), EntityType.VILLAGER);
        villager.setVillagerType(type);
        villager.setProfession(profession);
        villager.setAI(false);
        villager.setCollidable(false);
        villager.setSilent(true);
        villager.setPersistent(true);
        villager.setRemoveWhenFarAway(false);

        villagerData.setUuid(this.villager.getUniqueId());
    }

    private void findVillager(int attempt) {
        if (attempt >= 10) {
            MLLogManager.getInstance().log(Level.WARNING, TAG,
                    "Villager " + nameId + " introuvable après 10 tentatives — respawn forcé.");
            spawnVillager();
            saveData();
            onInitialized();
            return;
        }

        World world = villagerData.getLocation().getWorld();
        if (world == null) {
            MiubyLib.runLater(() -> findVillager(attempt + 1), 10L);
            return;
        }

        if (!villagerData.getLocation().getChunk().isLoaded()) {
            villagerData.getLocation().getChunk().load();
        }

        Entity entity = world.getEntity(villagerData.getUuid());
        if (entity != null && entity.getType() == EntityType.VILLAGER) {
            this.villager = (Villager) entity;
            if (attempt == 0) {
                MLLogManager.getInstance().log(Level.INFO, TAG, "Villager " + nameId + " chargé.");
            } else {
                MLLogManager.getInstance().log(Level.INFO, TAG,
                        "Villager " + nameId + " trouvé après " + (attempt * 10) + " ticks d'attente.");
            }
            onInitialized();
        } else {
            MiubyLib.runLater(() -> findVillager(attempt + 1), 10L);
        }
    }
}
