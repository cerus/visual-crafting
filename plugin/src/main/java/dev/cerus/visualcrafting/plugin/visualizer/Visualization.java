package dev.cerus.visualcrafting.plugin.visualizer;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Simple data class for a visualization. Fields are public because this class can only be used by the controller.
 */
class Visualization<T> {

    public Block block;
    public Player player;
    public T obj;

    Visualization(final Block block, final Player player, final T obj) {
        this.block = block;
        this.player = player;
        this.obj = obj;
    }

}
