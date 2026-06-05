package fr.miuby.lib.world;

import fr.miuby.lib.utils.Rect;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Représentation d'un monde géré par MiubyLib.
 *
 * <p>Encapsule un {@link World} Bukkit avec ses métadonnées (nom, couleur, type)
 * et une limite de zone optionnelle ({@link Rect}).</p>
 */
@Getter
@RequiredArgsConstructor
public class MLWorld {
    private final World world;
    private final String name;
    private final NamedTextColor color;
    private final WorldType type;

    @Setter
    private Rect limit;
    @Setter
    private boolean isLocked;

    /**
     * Retourne l'UUID unique du monde Bukkit.
     *
     * @return l'UUID du monde
     */
    public UUID getUUID() {
        return world.getUID();
    }

    /**
     * Indique si le joueur se trouve dans ce monde.
     *
     * @param player le joueur à vérifier
     * @return {@code true} si le joueur est dans ce monde
     */
    public boolean isPlayerInWorld(Player player) {
        return player.getWorld().getUID().equals(getUUID());
    }

    /**
     * Indique si le joueur est dans ce monde et hors de la {@link Rect limite}.
     * Retourne {@code false} si aucune limite n'est définie ou si le joueur n'est pas dans ce monde.
     *
     * @param player le joueur à vérifier
     * @return {@code true} si le joueur est hors de la limite définie
     */
    public boolean isPlayerOutOfLimit(Player player) {
        if (limit == null || !isPlayerInWorld(player)) return false;
        return limit.isOut(player.getLocation());
    }
}
