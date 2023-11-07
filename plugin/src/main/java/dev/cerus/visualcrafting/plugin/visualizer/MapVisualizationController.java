package dev.cerus.visualcrafting.plugin.visualizer;

import dev.cerus.visualcrafting.api.version.FakeMap;
import dev.cerus.visualcrafting.api.version.Feature;
import dev.cerus.visualcrafting.api.version.VersionAdapter;
import dev.cerus.visualcrafting.plugin.texture.Texture;
import dev.cerus.visualcrafting.plugin.texture.TextureCache;
import static dev.cerus.visualcrafting.plugin.visualizer.DirectionProvider.getDirection;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Controls visualizations
 * TODO: Maybe break this class up a bit by moving the map drawing part somewhere else
 */
public class MapVisualizationController implements VisualizationController {

    private static final int MIN_X = 32;
    private static final int MIN_Y = 32;
    private static final int SPACE = 8;
    private static final int WIDTH = 16;
    private static final int HEIGHT = 16;

    private final Map<Long, Visualization<FramedMap>> visualizationMap = new HashMap<>();
    private final VersionAdapter versionAdapter;
    private final TextureCache textureCache;

    public MapVisualizationController(final VersionAdapter versionAdapter, final TextureCache textureCache) {
        this.versionAdapter = versionAdapter;
        this.textureCache = textureCache;
    }

    @Override
    public boolean accepts(final VersionAdapter versionAdapter) {
        return versionAdapter.getImplementedFeatures().contains(Feature.MAPS);
    }

    @Override
    public void entityClick(final Player player, final int eid) {
        this.visualizationMap.values().stream()
                .filter(visualization -> visualization.obj.frameEntityId == eid)
                .findAny()
                .ifPresent(visualization -> {
                    final PlayerInteractEvent interactEvent = new PlayerInteractEvent(player,
                            Action.RIGHT_CLICK_BLOCK,
                            player.getInventory().getItemInMainHand(),
                            visualization.block.getLocation().getBlock(), // Get a fresh block object, the old one could be outdated
                            BlockFace.SELF,
                            EquipmentSlot.HAND);
                    Bukkit.getPluginManager().callEvent(interactEvent);
                    if (interactEvent.useInteractedBlock() == Event.Result.ALLOW
                        || interactEvent.useInteractedBlock() == Event.Result.DEFAULT) {
                        player.openWorkbench(visualization.block.getLocation(), false);
                    }
                });
    }

    /**
     * A player has selected a recipe. This will display the recipe
     * if the constraints are met.
     *
     * @param matrix        The crafting matrix
     * @param result        The resulting item
     * @param actor         The acting player
     * @param craftingTable The used crafting table
     */
    @Override
    public void recipeSelected(final ItemStack[] matrix, final ItemStack result, final Player actor, final Block craftingTable) {
        if (craftingTable.getRelative(BlockFace.UP).getType() != Material.AIR) {
            return;
        }

        final long key = this.getBlockKey(craftingTable);
        if (!this.visualizationMap.containsKey(key)) {
            final int eid = this.versionAdapter.spawnItemFrame(craftingTable.getLocation().clone().add(0, 1, 0), BlockFace.UP);
            final FakeMap map = this.versionAdapter.createMap();
            final Visualization<FramedMap> visualization = new Visualization<>(
                    craftingTable,
                    actor,
                    new FramedMap(map, eid)
            );
            this.visualizationMap.put(key, visualization);
            this.updateMap(map, matrix, result);
            this.versionAdapter.sendMap(map);
            this.versionAdapter.updateItemFrame(eid, map.toItem(), this.calculateFrameRotation(actor), true);
        } else {
            final Visualization<FramedMap> visualization = this.visualizationMap.get(key);
            if (!visualization.player.getUniqueId().equals(actor.getUniqueId())) {
                // Someone else is using this crafting table
                return;
            }

            this.updateMap(visualization.obj.map, matrix, result);
            this.versionAdapter.sendMap(visualization.obj.map);
            this.versionAdapter.updateItemFrame(visualization.obj.frameEntityId, visualization.obj.map.toItem(), this.calculateFrameRotation(actor), true);
        }
    }

    /**
     * A player has cancelled the crafting process. This will remove any
     * visualizations if the constraints are met.
     *
     * @param actor         The acting player
     * @param craftingTable The used crafting table
     */
    @Override
    public void craftingCancelled(final Player actor, final Block craftingTable) {
        final long key = this.getBlockKey(craftingTable);
        if (!this.visualizationMap.containsKey(key)) {
            return;
        }

        final Visualization<FramedMap> visualization = this.visualizationMap.get(key);
        if (!visualization.player.getUniqueId().equals(actor.getUniqueId())) {
            return;
        }

        this.visualizationMap.remove(key);
        this.versionAdapter.destroyEntity(visualization.obj.frameEntityId);
    }

    /**
     * Draws the crafting matrix onto a map
     *
     * @param map    The canvas
     * @param matrix The crafting matrix
     * @param result The crafting result
     */
    private void updateMap(final FakeMap map, final ItemStack[] matrix, final ItemStack result) {
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                map.setPixel(x, y, (byte) 0);
            }
        }

        if (result != null) {
            this.drawItem(map, result.getType(), MIN_X + WIDTH + SPACE, SPACE);
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                final ItemStack item = matrix[(row * 3) + col];
                final int x = MIN_X + (col * (WIDTH + SPACE));
                final int y = MIN_Y + (row * (HEIGHT + SPACE));
                if (item != null) {
                    this.drawItem(map, item.getType(), x, y);
                }
            }
        }
    }

    /**
     * Attempts to draw a material
     *
     * @param map      The canvas
     * @param material The material to draw
     * @param baseX    The x pos
     * @param baseY    The y pos
     */
    private void drawItem(final FakeMap map, final Material material, final int baseX, final int baseY) {
        Texture texture;
        if (material.isBlock()) {
            texture = this.textureCache.getTexture("block", material.name().toLowerCase());
            if (texture == null) {
                texture = this.textureCache.getTexture("item", material.name().toLowerCase());
            }
        } else if (material.isItem()) {
            texture = this.textureCache.getTexture("item", material.name().toLowerCase());
            if (texture == null) {
                texture = this.textureCache.getTexture("block", material.name().toLowerCase());
            }
        } else {
            texture = null;
        }

        if (texture != null) {
            for (int xx = 0; xx < 16; xx++) {
                for (int yy = 0; yy < 16; yy++) {
                    map.setPixel(baseX + xx, baseY + yy, texture.get(xx, yy));
                }
            }
        }
    }

    /**
     * Calculate the frame rotation that's needed to face the player
     *
     * @param actor The player
     *
     * @return The player's direction
     */
    private Rotation calculateFrameRotation(final Player actor) {
        final var yaw = actor.getLocation().getYaw();
        return switch (getDirection(yaw).getOppositeFace()) {
            case WEST -> Rotation.CLOCKWISE_45;
            case NORTH -> Rotation.CLOCKWISE;
            case EAST -> Rotation.CLOCKWISE_135;
            default -> Rotation.NONE;
        };
    }

    private long getBlockKey(final Block block) {
        return this.getBlockKey(block.getX(), block.getY(), block.getZ());
    }

    // Stolen from Paper
    private long getBlockKey(final int x, final int y, final int z) {
        return ((long) x & 0x7FFFFFF) | (((long) z & 0x7FFFFFF) << 27) | ((long) y << 54);
    }

    private static class FramedMap {

        public FakeMap map;
        public int frameEntityId;

        public FramedMap(final FakeMap map, final int frameEntityId) {
            this.map = map;
            this.frameEntityId = frameEntityId;
        }

    }

}
