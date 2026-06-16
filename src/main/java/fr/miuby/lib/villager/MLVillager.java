package fr.miuby.lib.villager;

import fr.miuby.lib.MiubyLib;
import fr.miuby.lib.log.ILogTag;
import fr.miuby.lib.log.MLLogManager;
import lombok.AccessLevel;
import lombok.Getter;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;

@Getter
public abstract class MLVillager {
    private static final ILogTag TAG = () -> "VILLAGER";

    protected final String nameId;
    @Getter(AccessLevel.NONE) private LivingEntity entity;

    protected TextComponent displayName;
    protected Inventory inventory;
    private MLVillagerData villagerData;

    public MLVillager(String nameId) {
        this.nameId = nameId;
    }

    // -------------------------------------------------------------------------
    // API publique
    // -------------------------------------------------------------------------

    public static <T extends MLVillager> T spawn(Supplier<T> constructor) {
        T villager = constructor.get();
        villager.init();
        return villager;
    }

    /**
     * Type d'entité Bukkit à spawner.
     * Toutes les implémentations retournent désormais {@link EntityType#MANNEQUIN}.
     */
    protected abstract EntityType getEntityType();

    /**
     * Retourne l'entité Bukkit de manière null-safe, avec relookup par UUID si nécessaire.
     */
    public LivingEntity getVillager() {
        if (this.entity != null && this.entity.isValid() && !this.entity.isDead())
            return this.entity;

        UUID uuid = this.entity == null ? null : this.entity.getUniqueId();
        if (uuid == null)
            throw new IllegalStateException("Villager " + this.nameId + " est null.");

        Entity lookup = Bukkit.getEntity(uuid);
        if (lookup instanceof LivingEntity live) {
            this.entity = live;
            return live;
        }

        return entity;
    }

    public void destroy() {
        VillagerRegistry.unregister(this);
        onDestroy();
        if (entity != null) entity.remove();
    }

    // -------------------------------------------------------------------------
    // Méthodes abstraites
    // -------------------------------------------------------------------------

    protected abstract @Nullable MLVillagerData loadData();
    protected abstract void saveData();
    protected abstract MLVillagerData createDefaultData();

    // -------------------------------------------------------------------------
    // Hooks
    // -------------------------------------------------------------------------

    protected void onInitialized() {
        getVillager().customName(getDisplayName());
        createInventory();
        VillagerRegistry.register(this);
        MiubyLib.callEvent(new VillagerLoadedEvent(this));
    }

    protected void onDestroy() {}

    protected void createInventory() {}

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    public final void init() {
        villagerData = this.loadData();
        if (villagerData == null) {
            villagerData = createDefaultData();
            spawnEntity();
            saveData();
            onInitialized();
        } else {
            findEntity(0);
        }
    }

    private void spawnEntity() {
        if (villagerData == null || villagerData.getLocation().getWorld() == null)
            throw new IllegalStateException("Impossible de spawner " + nameId + " : world null.");

        if (!villagerData.getLocation().getChunk().isLoaded())
            villagerData.getLocation().getChunk().load();

        this.entity = (LivingEntity) villagerData.getLocation().getWorld()
                .spawnEntity(villagerData.getLocation(), getEntityType());

        this.entity.setAI(false);
        this.entity.setCollidable(false);
        this.entity.setSilent(true);
        this.entity.setPersistent(true);
        this.entity.setRemoveWhenFarAway(false);
        this.entity.setInvulnerable(true);

        villagerData.setUuid(this.entity.getUniqueId());
    }

    private void findEntity(int attempt) {
        if (attempt >= 10) {
            MLLogManager.getInstance().log(Level.WARNING, TAG,
                    nameId + " introuvable après 10 tentatives — respawn forcé.");
            spawnEntity();
            saveData();
            onInitialized();
            return;
        }

        World world = villagerData.getLocation().getWorld();
        if (world == null) {
            MiubyLib.runLater(() -> findEntity(attempt + 1), 10L);
            return;
        }

        if (!villagerData.getLocation().getChunk().isLoaded())
            villagerData.getLocation().getChunk().load();

        Entity found = world.getEntity(villagerData.getUuid());
        if (found instanceof LivingEntity le) {
            this.entity = le;
            MLLogManager.getInstance().log(Level.INFO, TAG,
                    nameId + (attempt == 0 ? " chargé." : " trouvé après " + (attempt * 10) + " ticks."));
            onInitialized();
        } else {
            MiubyLib.runLater(() -> findEntity(attempt + 1), 10L);
        }
    }
}
