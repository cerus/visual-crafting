package dev.cerus.visualcrafting.api.version;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

/**
 * A fake map
 */
public class FakeMap {

    private final int id;
    private final byte[] data;
    private final Object handle;

    FakeMap(final int id, Object handle) {
        this.id = id;
        this.data = new byte[128 * 128];
        this.handle = handle;
    }

    public void setPixel(final int x, final int y, final byte color) {
        this.data[x + y * 128] = color;
    }

    public byte getPixel(final int x, final int y) {
        return this.data[x + y * 128];
    }

    public ItemStack toItem() {
        final ItemStack itemStack = new ItemStack(Material.FILLED_MAP);
        final MapMeta meta = (MapMeta) itemStack.getItemMeta();
        meta.setMapId(this.id);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public int getId() {
        return this.id;
    }

    byte[] getData() {
        return this.data;
    }

    Object getHandle() {
        return handle;
    }
}
