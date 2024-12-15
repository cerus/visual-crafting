package dev.cerus.visualcrafting.plugin.listener;

import dev.cerus.visualcrafting.plugin.VisualCraftingPlugin;
import dev.cerus.visualcrafting.plugin.visualizer.VisualizationController;
import java.util.Arrays;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.inventory.CraftingInventory;

public class CraftingInventoryInteractListener implements Listener {
    private final VisualCraftingPlugin plugin;
    private final VisualizationController visualizationController;

    public CraftingInventoryInteractListener(VisualCraftingPlugin plugin, VisualizationController visualizationController) {
        this.plugin = plugin;
        this.visualizationController = visualizationController;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        onInteract(event);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        onInteract(event);
    }

    public void onInteract(InventoryInteractEvent event) {
        if (!(event.getView().getTopInventory() instanceof CraftingInventory inv)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            if (Arrays.stream(inv.getMatrix()).allMatch(Objects::isNull) && inv.getResult() == null) {
                this.visualizationController.craftingCancelled(player, inv.getLocation().getBlock());
            } else {
                this.visualizationController.recipeSelected(inv.getMatrix(),
                        inv.getResult(),
                        player,
                        inv.getLocation().getBlock());
            }
        }, 1);
    }
}
