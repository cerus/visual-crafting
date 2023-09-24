package dev.cerus.visualcrafting.api.version;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

/**
 * A fake item display
 */
public class FakeItemDisplay {

    private final Location location;
    private final ItemStack itemStack;
    private final BlockFace rotationX;
    private final BlockFace rotationY;
    private final Transform transform;

    public FakeItemDisplay(final Location location,
                           final ItemStack itemStack,
                           final BlockFace rotationX,
                           final BlockFace rotationY,
                           final Transform transform) {
        this.location = location;
        this.itemStack = itemStack;
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.transform = transform;
    }

    public Location getLocation() {
        return this.location;
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public BlockFace getRotationX() {
        return this.rotationX;
    }

    public BlockFace getRotationY() {
        return this.rotationY;
    }

    public Transform getTransform() {
        return this.transform;
    }

    public enum Transform {
        FIRSTPERSON_LEFTHAND,
        FIRSTPERSON_RIGHTHAND,
        FIXED,
        GROUND,
        GUI,
        HEAD,
        NONE,
        THIRDPERSON_LEFTHAND,
        THIRDPERSON_RIGHTHAND
    }

}