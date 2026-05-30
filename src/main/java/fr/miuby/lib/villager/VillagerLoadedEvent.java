package fr.miuby.lib.villager;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

@Getter
public class VillagerLoadedEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final MLVillager villager;

    public VillagerLoadedEvent(MLVillager villager) {
        this.villager = villager;
    }

    @Override
    public @NonNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
