package fr.miuby.world;

import fr.miuby.utils.Rect;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class MiubyWorld {
    private final World world;
    private final String name;
    private final NamedTextColor color;
    private final WorldType type;

    @Setter
    private Location spawnPoint;
    @Setter
    private Rect limit;
    @Setter
    private boolean isLocked;

    public UUID getUUID() {
        return world.getUID();
    }

    public boolean isPlayerInWorld(Player player) {
        return player.getWorld().getUID().equals(getUUID());
    }

    public boolean isPlayerOutOfLimit(Player player) {
        if (limit == null || !isPlayerInWorld(player)) {
            return false;
        }
        Block block = player.getLocation().getBlock();
        return limit.isOut(block.getX(), block.getY(), block.getZ());
    }
}
