package dev.cerus.visualcrafting.plugin.texture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cache for textures
 */
public class TextureCache {

    private final Map<String, Map<String, Texture>> textureMap = new HashMap<>();

    /**
     * Write this cache to a file
     *
     * @param file The output file
     */
    public void write(final File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        try (final FileOutputStream out = new FileOutputStream(file)) {
            for (final String group : this.textureMap.keySet()) {
                // Group length
                out.write(ByteBuffer.allocate(4).putInt(group.length()).array());
                // Group
                out.write(group.getBytes(StandardCharsets.UTF_8));

                final Map<String, Texture> textures = this.textureMap.get(group);
                // Amount
                out.write(ByteBuffer.allocate(4).putInt(textures.size()).array());

                for (final Texture texture : textures.values()) {
                    // Name length
                    out.write(ByteBuffer.allocate(4).putInt(texture.getName().length()).array());
                    // Name
                    out.write(texture.getName().getBytes(StandardCharsets.UTF_8));
                    // Texture data
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            out.write(texture.get(x, y));
                        }
                    }
                }
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read cache from a file
     *
     * @param file The input file
     */
    public void read(final File file) {
        try (final FileInputStream in = new FileInputStream(file)) {
            while (in.available() > 0) {
                // Group length
                byte[] arr = new byte[4];
                in.read(arr);
                int len = ByteBuffer.wrap(arr).getInt();

                // Group
                arr = new byte[len];
                in.read(arr);
                final String group = new String(arr);

                arr = new byte[4];
                in.read(arr);
                final int amount = ByteBuffer.wrap(arr).getInt();

                for (int i = 0; i < amount; i++) {
                    // Name length
                    arr = new byte[4];
                    in.read(arr);
                    len = ByteBuffer.wrap(arr).getInt();

                    // Name
                    arr = new byte[len];
                    in.read(arr);
                    final String name = new String(arr);

                    // Texture data
                    final Texture texture = new Texture(group, name);
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            texture.set(x, y, (byte) in.read());
                        }
                    }
                    this.addTexture(group, name, texture);
                }
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public void addTexture(final String group, final String name, final Texture texture) {
        this.textureMap.computeIfAbsent(group, s -> new HashMap<>()).put(name, texture);
    }

    public Texture getTexture(final String group, final String name) {
        if (!this.textureMap.containsKey(group)) {
            return null;
        }
        return this.textureMap.get(group).get(name);
    }

    public Collection<String> getTextureGroups() {
        return this.textureMap.keySet();
    }

    public Collection<String> getTextureNames(final String group) {
        if (!this.textureMap.containsKey(group)) {
            return List.of();
        }
        return this.textureMap.get(group).keySet();
    }

}
