package dev.cerus.visualcrafting.plugin.listener;

import dev.cerus.visualcrafting.api.version.VersionAdapter;
import dev.cerus.visualcrafting.plugin.VisualCraftingPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final VisualCraftingPlugin plugin;
    private final VersionAdapter versionAdapter;

    public PlayerJoinListener(final VisualCraftingPlugin plugin, final VersionAdapter versionAdapter) {
        this.plugin = plugin;
        this.versionAdapter = versionAdapter;
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        if (this.plugin.enablePacketListening()) {
            this.versionAdapter.inject(event.getPlayer());
        }
    }

}
