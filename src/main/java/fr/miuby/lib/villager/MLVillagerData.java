package fr.miuby.lib.villager;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Données persistées d'un {@link MLVillager} (UUID, identifiant et position).
 */
@Getter
@AllArgsConstructor
public class MLVillagerData {
    /** Inconnu jusqu'au premier spawn — assigné automatiquement par {@link MLVillager}. */
    @Setter
    protected UUID uuid;

    protected final String nameId;

    @Setter
    @NotNull
    protected Location location;
}
