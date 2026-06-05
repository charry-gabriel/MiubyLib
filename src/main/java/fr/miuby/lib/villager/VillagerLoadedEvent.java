package fr.miuby.lib.villager;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * Événement Bukkit déclenché lorsqu'un {@link MLVillager} a été chargé et initialisé.
 *
 * <p>Permet aux autres systèmes de réagir à l'apparition d'un villager sans couplage direct.</p>
 */
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

    /**
     * Retourne la liste statique des handlers, requise par l'API Bukkit.
     *
     * @return la liste statique des handlers
     */
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
