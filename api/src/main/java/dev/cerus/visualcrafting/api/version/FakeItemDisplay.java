package dev.cerus.visualcrafting.api.version;

import dev.cerus.visualcrafting.api.math.MatrixMath;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * A fake item display
 */
public class FakeItemDisplay {

    private static final float SCALE = 0.18f;

    private ItemStack itemStack;
    private Matrix3f rotationX;
    private Matrix3f rotationZ;
    private Matrix4f translation;
    private Transform transform;
    private Location location;

    public FakeItemDisplay(final Location location,
                           final ItemStack itemStack,
                           final Matrix3f rotationX,
                           final Matrix3f rotationZ,
                           final Matrix4f translation,
                           final Transform transform) {
        this.location = location;
        this.itemStack = itemStack;
        this.rotationX = rotationX;
        this.rotationZ = rotationZ;
        this.translation = translation;
        this.transform = transform;
    }

    /**
     * Create the transformation matrix for displaying an item
     *
     * @return the matrix
     */
    public Matrix4f getTransformationMatrix() {
        return MatrixMath.combine(
                this.translation,
                MatrixMath.combineAndExpand(
                        this.rotationX,
                        this.rotationZ
                ),
                MatrixMath.scale(SCALE, SCALE, 0.0001f)
        );
    }

    public Location getLocation() {
        return this.location;
    }

    public void setLocation(final Location location) {
        this.location = location;
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public void setItemStack(final ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public Matrix3f getRotationX() {
        return this.rotationX;
    }

    public void setRotationX(final Matrix3f rotationX) {
        this.rotationX = rotationX;
    }

    public Matrix3f getRotationZ() {
        return this.rotationZ;
    }

    public void setRotationZ(final Matrix3f rotationZ) {
        this.rotationZ = rotationZ;
    }

    public Matrix4f getTranslation() {
        return this.translation;
    }

    public void setTranslation(final Matrix4f translation) {
        this.translation = translation;
    }

    public Transform getTransform() {
        return this.transform;
    }

    public void setTransform(final Transform transform) {
        this.transform = transform;
    }

    public enum Transform {
        NONE,
        THIRDPERSON_LEFTHAND,
        THIRDPERSON_RIGHTHAND,
        FIRSTPERSON_LEFTHAND,
        FIRSTPERSON_RIGHTHAND,
        HEAD,
        GUI,
        GROUND,
        FIXED
    }

}