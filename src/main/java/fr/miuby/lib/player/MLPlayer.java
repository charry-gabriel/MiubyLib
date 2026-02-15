package fr.miuby.lib.player;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MLPlayer {
    @Getter
    protected final UUID uuid;
    @Setter
    @Getter
    protected String pseudo;
    @Setter
    @Getter
    protected Player player;

    public MLPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    public void onJoinServer() {
    }
}
