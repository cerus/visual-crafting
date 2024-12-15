package dev.cerus.visualcrafting.plugin.listener;

import dev.cerus.visualcrafting.plugin.visualizer.VisualizationController;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.CraftingInventory;

public class CancelCraftingListener implements Listener {

    private final VisualizationController visualizationController;

    public CancelCraftingListener(final VisualizationController visualizationController) {
        this.visualizationController = visualizationController;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (!(event.getView().getTopInventory() instanceof final CraftingInventory inv)) {
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
