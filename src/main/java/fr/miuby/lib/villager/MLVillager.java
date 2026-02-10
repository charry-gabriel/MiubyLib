package fr.miuby.lib.villager;

import fr.miuby.lib.MiubyLib;
import lombok.Getter;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;

import javax.annotation.Nullable;
import java.util.function.Supplier;

@Getter
public abstract class MLVillager {
    protected final String nameId;
    private final Villager.Type type;
    private final Villager.Profession profession;
    protected Villager villager;
    protected TextComponent displayName;
    protected Inventory inventory;

    private MLVillagerData villagerData;

    public static <T extends MLVillager> T create(Supplier<T> constructor) {
        T villager = constructor.get();
        villager.init();
        return villager;
    }

    public MLVillager(String nameId, Villager.Type type, Villager.Profession profession) {
        this.nameId = nameId;
        this.type = type;
        this.profession = profession;
    }

    public final void init() {
        villagerData = this.loadData();
        if (villagerData == null) {
            villagerData = createDefaultData();
            createVillager();
            saveData();
            onInitialized();
        } else {
            findVillager(0);
        }
    }

    protected void destroy() {
        if (villager != null) {
            villager.remove();
        }
    }

    protected abstract @Nullable MLVillagerData loadData();
    protected abstract void saveData();
    protected abstract MLVillagerData createDefaultData();

    protected void onInitialized() {
        getVillager().customName(getDisplayName());
        createInventory();

        MiubyLib.callEvent(new VillagerLoadedEvent(this));
    }

    protected void createInventory() {
        this.inventory = this.getVillager().getInventory();
    }

    private void createVillager() {
        if (villagerData == null || villagerData.location.getWorld() == null) {
            throw new IllegalStateException("Unable to create villager " + nameId + ": Missing Location or World in villagerData.");
        }

        if (!villagerData.location.getChunk().isLoaded()) {
            villagerData.location.getChunk().load();
        }

        this.villager = (Villager) villagerData.location.getWorld().spawnEntity(villagerData.location, EntityType.VILLAGER);
        villager.setVillagerType(type);
        villager.setProfession(profession);
        villager.setAI(false);
        villager.setCollidable(false);
        villager.setSilent(true);
        villager.setPersistent(true);
        villager.setRemoveWhenFarAway(false);

        this.villagerData.uuid = this.villager.getUniqueId();
    }

    private void findVillager(int attempt) {
        if (attempt >= 10) {
            MiubyLib.getLogger().warning("Unable to find villager " + nameId + " after waiting for chunk load.");
            createVillager();
            saveData();
            onInitialized();
            return;
        }

        if (!villagerData.location.getChunk().isLoaded()) {
            villagerData.location.getChunk().load();
        }

        Entity entity = villagerData.location.getWorld().getEntity(villagerData.uuid);
        if (entity != null && entity.getType() == EntityType.VILLAGER) {
            this.villager = (Villager)entity;
            MiubyLib.getLogger().info("Villager " + nameId + " found after waiting " + attempt + " ticks.");
            onInitialized();
        } else {
            MiubyLib.runLater(() -> findVillager(attempt + 1), 10L);
        }
    }
}