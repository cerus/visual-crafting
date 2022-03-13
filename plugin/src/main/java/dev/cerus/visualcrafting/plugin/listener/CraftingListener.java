package dev.cerus.visualcrafting.plugin.listener;

import dev.cerus.visualcrafting.plugin.VisualCraftingPlugin;
import dev.cerus.visualcrafting.plugin.visualizer.VisualizationController;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;

public class CraftingListener implements Listener {

    private final VisualCraftingPlugin plugin;
    private final VisualizationController visualizationController;

    public CraftingListener(final VisualCraftingPlugin plugin, final VisualizationController visualizationController) {
        this.plugin = plugin;
        this.visualizationController = visualizationController;
    }

    @EventHandler
    public void onPrepareCraft(final PrepareItemCraftEvent event) {
        final CraftingInventory inv = event.getInventory();
        if (inv.getSize() != 10) {
            // Not a crafting table
            return;
        }
        if (inv.getLocation() == null) {
            return;
        }

        final Player player = (Player) event.getView().getPlayer();
        if (event.getRecipe() == null) {
            // No recipe selected? Cancel crafting
            this.visualizationController.craftingCancelled(player, inv.getLocation().getBlock());
        } else if (this.plugin.canUse(player)) {
            // Recipe selected? Show item matrix
            this.visualizationController.recipeSelected(inv.getMatrix(),
                    inv.getResult(),
                    player,
                    inv.getLocation().getBlock());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (!(event.getView().getTopInventory() instanceof CraftingInventory inv)) {
            return;
        }
        if (inv.getSize() != 10) {
            // Not a crafting table
            return;
        }
        if (inv.getLocation() == null) {
            return;
        }

        // Crafting table closed? Cancel crafting
        this.visualizationController.craftingCancelled((Player) event.getView().getPlayer(), inv.getLocation().getBlock());
    }

}
