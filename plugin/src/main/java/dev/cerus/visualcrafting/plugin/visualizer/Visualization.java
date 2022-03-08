package dev.cerus.visualcrafting.plugin.visualizer;

import dev.cerus.visualcrafting.api.version.FakeMap;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Simple data class for a visualization. Fields are public because this class can only be used by the controller.
 */
class Visualization {

    public Block block;
    public Player player;
    public int entityId;
    public FakeMap map;

    Visualization(final Block block, final Player player, final int entityId, final FakeMap map) {
        this.block = block;
        this.player = player;
        this.entityId = entityId;
        this.map = map;
    }

}
