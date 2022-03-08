package dev.cerus.visualcrafting.plugin.texture;

/**
 * Simple wrapper for a texture name and texture data
 */
public class Texture {

    private final byte[][] colors = new byte[16][16];
    private final String group;
    private final String name;

    public Texture(final String group, final String name) {
        this.group = group;
        this.name = name;
    }

    public void set(final int x, final int y, final byte color) {
        this.colors[x][y] = color;
    }

    public byte get(final int x, final int y) {
        return this.colors[x][y];
    }

    public String getGroup() {
        return this.group;
    }

    public String getName() {
        return this.name;
    }

}
