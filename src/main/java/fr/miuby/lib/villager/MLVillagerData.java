package fr.miuby.lib.villager;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class MLVillagerData {
    protected UUID uuid;
    protected final String nameId;
    @NotNull
    protected Location location;
}
