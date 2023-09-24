package dev.cerus.visualcrafting.api.version;

import dev.cerus.visualcrafting.api.config.Config;
import java.util.function.BiConsumer;
import org.bukkit.Location;
import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Base class for version implementations
 */
public abstract class VersionAdapter {

    /**
     * Initialize the adapter
     *
     * @param config              The plugin config
     * @param entityClickCallback Callback when a player clicks an entity
     */
    public abstract void init(Config config, BiConsumer<Player, Integer> entityClickCallback);

    /**
     * Used to inject a packet listener
     *
     * @param player The player to inject
     */
    public abstract void inject(Player player);

    /**
     * Spawn a fake item frame and return the frame's id
     *
     * @param location  Location of the frame
     * @param direction Direction the frame is facing
     *
     * @return Frame entity id
     */
    public abstract int spawnItemFrame(Location location, BlockFace direction);

    /**
     * Update a fake item frame's item, rotation and visibility
     *
     * @param frameId   The entity id of the frame
     * @param itemStack The displayed item
     * @param rotation  The frame's rotation
     * @param invisible The frame's visibility
     */
    public abstract void updateItemFrame(int frameId, ItemStack itemStack, Rotation rotation, boolean invisible);

    /**
     * Remove an entity
     *
     * @param entityId The entity's id
     */
    public abstract void destroyEntity(int entityId);

    /**
     * Helper method for creating a new fake map
     *
     * @param id The map's id
     *
     * @return A new fake map
     */
    protected FakeMap createMap(final int id) {
        return new FakeMap(id);
    }

    /**
     * Get a fake map's data array
     *
     * @param fakeMap The fake map
     *
     * @return The data array
     */
    protected byte[] getMapData(final FakeMap fakeMap) {
        return fakeMap.getData();
    }

    /**
     * Create a new fake map
     *
     * @return A new fake map
     */
    public abstract FakeMap createMap();

    /**
     * Broadcast a map
     *
     * @param map The map to broadcast
     */
    public abstract void sendMap(FakeMap map);

    /**
     * Get an array of implemented VisualCrafting features
     *
     * @return All the features this implementation implements
     */
    public Feature[] getImplementedFeatures() {
        return new Feature[] {Feature.MAPS};
    }

}
