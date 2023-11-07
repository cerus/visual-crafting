package dev.cerus.visualcrafting.plugin.visualizer;

import dev.cerus.visualcrafting.api.version.VersionAdapter;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Controls visualizations
 */
public interface VisualizationController {

    /**
     * Returns whether this controller accepts the specified version adapter
     *
     * @param versionAdapter The version adapter to check
     *
     * @return True if the version adapter is accepted
     */
    boolean accepts(VersionAdapter versionAdapter);

    /**
     * A player has interacted with an entity
     *
     * @param player The actor
     * @param eid    The clicked entity
     */
    void entityClick(final Player player, final int eid);

    /**
     * A player has selected a recipe. This will display the recipe
     * if the constraints are met.
     *
     * @param matrix        The crafting matrix
     * @param result        The resulting item
     * @param actor         The acting player
     * @param craftingTable The used crafting table
     */
    void recipeSelected(final ItemStack[] matrix, final ItemStack result, final Player actor, final Block craftingTable);

    /**
     * A player has cancelled the crafting process. This will remove any
     * visualizations if the constraints are met.
     *
     * @param actor         The acting player
     * @param craftingTable The used crafting table
     */
    void craftingCancelled(final Player actor, final Block craftingTable);

}
