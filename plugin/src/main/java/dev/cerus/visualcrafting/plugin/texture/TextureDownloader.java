package dev.cerus.visualcrafting.plugin.texture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.cerus.maps.api.MapColor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Downloads and converts textures from Mojang
 */
public class TextureDownloader {

    private static final String VERSION_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";

    private final ExecutorService executorService;

    public TextureDownloader() {
        this(Executors.newSingleThreadScheduledExecutor());
    }

    public TextureDownloader(final ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Downloads and converts the Minecraft item textures
     *
     * @param logger       The logger
     * @param folder       The temporary folder
     * @param textureCache The texture cache instance
     * @return A callback
     */
    public CompletableFuture<Void> downloadTextures(final Logger logger, final File folder, final TextureCache textureCache) {
        folder.mkdirs();
        final CompletableFuture<Void> future = new CompletableFuture<>();
        this.executorService.execute(() -> {
            try {
                // Get download url
                final String clientDownloadUrl;
                try {
                    clientDownloadUrl = this.getLatestClientUrl();
                } catch (final IOException e) {
                    future.completeExceptionally(e);
                    return;
                }

                // Download the client
                logger.info("Downloading Minecraft textures");
                folder.mkdirs();
                final File clientJar = new File(folder, "client.jar");
                final Path outputPath = folder.toPath();
                try {
                    this.download(clientDownloadUrl, clientJar);
                } catch (final Exception e) {
                    future.completeExceptionally(e);
                    return;
                }

                // Extract and convert the textures from the client
                logger.info("Extracting & converting item textures");
                try {
                    this.extractAndConvert(folder, textureCache, clientJar, "item", outputPath);
                } catch (final IOException e) {
                    future.completeExceptionally(e);
                    return;
                }
                logger.info("Extracting & converting block textures");
                try {
                    this.extractAndConvert(folder, textureCache, clientJar, "block", outputPath);
                } catch (final IOException e) {
                    future.completeExceptionally(e);
                    return;
                }

                // We don't need it anymore
                clientJar.delete();
                // some JVMs return null for empty dirs
                File[] files;
                if ((files = folder.listFiles()) == null || files.length == 0) folder.delete();

                // We're done!
                logger.info("Done");
                future.complete(null);
            } catch (final Throwable e) {
                future.completeExceptionally(e);
                // some JVMs return null for empty dirs
                File[] files;
                if ((files = folder.listFiles()) == null || files.length == 0) folder.delete();
            }
        });
        return future;
    }

    private void extractAndConvert(final File folder, final TextureCache textureCache, final File clientJar, final String group, final Path outputPath) throws IOException {
        this.extractTextures(clientJar, group, outputPath);
        final File[] files = folder.listFiles(f -> f.getName().endsWith(".png"));
        if (files != null) {
            for (final File textureFile : files) {
                this.convertFileToTexture(group, textureFile, textureCache);
            }
        }
    }

    /**
     * Gets the latest Minecraft client url from the Minecraft version manifest
     *
     * @return The latest client download url
     * @throws IOException In case of IO errors
     */
    private String getLatestClientUrl() throws IOException {
        // Get version manifest and pull out the latest release id
        final JsonObject versionsObj = new JsonParser().parse(this.get(VERSION_MANIFEST_URL)).getAsJsonObject();
        final JsonObject latestObj = versionsObj.get("latest").getAsJsonObject();
        final String latestRelease = latestObj.get("release").getAsString();

        // Loop through entries until we find the release
        String url = null;
        final JsonArray versionsArr = versionsObj.get("versions").getAsJsonArray();
        for (final JsonElement element : versionsArr) {
            final JsonObject obj = element.getAsJsonObject();
            if (obj.get("id").getAsString().equals(latestRelease)) {
                url = obj.get("url").getAsString();
            }
        }

        // Get manifest of the release and pull out the download url
        final JsonObject latestManifest = new JsonParser().parse(this.get(url)).getAsJsonObject();
        final JsonObject downloadsObj = latestManifest.get("downloads").getAsJsonObject();
        final JsonObject clientObj = downloadsObj.get("client").getAsJsonObject();
        return clientObj.get("url").getAsString();
    }

    /**
     * Extracts the textures from a Minecraft client jar
     *
     * @param fileToExtract The jar file to extract
     * @param outputPath    The path where we should drop the textures in
     * @param group         The texture group to extract
     * @throws IOException In case of IO errors
     */
    private void extractTextures(final File fileToExtract, final String group, final Path outputPath) throws IOException {
        // Most of this has been stolen from https://stackoverflow.com/a/51057815/10821925
        try (final ZipFile zipFile = new ZipFile(fileToExtract, ZipFile.OPEN_READ)) {
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                // We only want item and block textures, we don't care about the rest
                if (!entry.getName().startsWith("assets/minecraft/textures/" + group) || !entry.getName().endsWith(".png")) {
                    continue;
                }
                final Path entryPath = outputPath.resolve(entry.getName().substring(entry.getName().lastIndexOf('/') + 1));
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (final InputStream in = zipFile.getInputStream(entry)) {
                        Files.copy(in, entryPath);
                    }
                }
            }
        }
    }

    /**
     * Converts a PNG file to a texture
     *
     * @param textureFile  The file to convert
     * @param textureCache The texture cache
     * @throws IOException In case of IO errors
     */
    private void convertFileToTexture(final String group, final File textureFile, final TextureCache textureCache) throws IOException {
        // Parse name, read image and delete file
        final String name = textureFile.getName().substring(0, textureFile.getName().length() - 4);
        final BufferedImage textureImg = ImageIO.read(textureFile);
        textureFile.delete();

        // Read pixels from image, convert colors and set pixels in texture object
        final Texture texture = new Texture(group, name);
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                if ((textureImg.getRGB(x, y) >> 24) == 0x00) {
                    // Pixel is transparent
                    texture.set(x, y, (byte) 0);
                } else {
                    final Color color = new Color(textureImg.getRGB(x, y));
                    texture.set(x, y, (byte) MapColor.rgbToMapColor(color.getRed(), color.getGreen(), color.getBlue()).getId());
                }
            }
        }
        // Cache texture
        textureCache.addTexture(group, name, texture);
    }

    /**
     * Connects to an url and reads the response body
     *
     * @param url The url to connect to
     * @return The response body
     * @throws IOException In case of IO errors
     */
    private String get(final String url) throws IOException {
        final InputStream in = this.connect(url);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] arr = new byte[256];
        int read;
        while ((read = in.read(arr)) != -1) {
            out.write(arr, 0, read);
        }
        in.close();
        return out.toString(StandardCharsets.UTF_8);
    }

    /**
     * Downloads something from an url to a file
     *
     * @param url The url to download something from
     * @param to  The output file
     * @throws IOException In case of IO errors
     */
    private void download(final String url, final File to) throws IOException {
        to.createNewFile();
        final InputStream in = this.connect(url);
        try (final FileOutputStream out = new FileOutputStream(to)) {
            final byte[] arr = new byte[256];
            int read;
            while ((read = in.read(arr)) != -1) {
                out.write(arr, 0, read);
            }
        }
        in.close();
    }

    /**
     * Connect to an url and get the input stream
     *
     * @param url Url to connect to
     * @return The input stream of the connection
     * @throws IOException In case of IO errors
     */
    private InputStream connect(final String url) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setDoInput(true);

        InputStream in;
        try {
            in = connection.getInputStream();
        } catch (final Exception ex) {
            in = connection.getErrorStream();
        }
        return in;
    }

    public void shutdown() {
        this.executorService.shutdown();
    }

}
