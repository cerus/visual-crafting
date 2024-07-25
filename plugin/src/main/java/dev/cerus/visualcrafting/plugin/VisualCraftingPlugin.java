package dev.cerus.visualcrafting.plugin;

import dev.cerus.visualcrafting.api.config.Config;
import dev.cerus.visualcrafting.api.version.VersionAdapter;
import dev.cerus.visualcrafting.plugin.listener.CraftingListener;
import dev.cerus.visualcrafting.plugin.listener.PlayerJoinListener;
import dev.cerus.visualcrafting.plugin.texture.TextureCache;
import dev.cerus.visualcrafting.plugin.texture.TextureDownloader;
import dev.cerus.visualcrafting.plugin.visualizer.DisplayVisualizationController;
import dev.cerus.visualcrafting.plugin.visualizer.MapVisualizationController;
import dev.cerus.visualcrafting.plugin.visualizer.VisualizationController;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.java.JavaPlugin;

public class VisualCraftingPlugin extends JavaPlugin implements Config {

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        String version = Bukkit.getVersion();
        version = version.substring(version.indexOf("MC: ") + 4, version.lastIndexOf(')'));
        if (!version.matches("\\d+\\.\\d+(\\.\\d+)?")) {
            this.getLogger().severe("Could not detect server version for " + version);
            this.getPluginLoader().disablePlugin(this);
            return;
        }
        final String minor = version.split("\\.")[1];

        // Initialize version adapter
        final VersionAdapter versionAdapter = switch (version) {
            case "1.16.5" -> createVersionAdapter("16R3");
            case "1.17", "1.17.1" -> createVersionAdapter("17R1");
            case "1.18.1" -> createVersionAdapter("18R1");
            case "1.18.2" -> createVersionAdapter("18R2");
            case "1.19", "1.19.1", "1.19.2" -> createVersionAdapter("19R1");
            case "1.19.3" -> createVersionAdapter("19R2");
            case "1.19.4" -> createVersionAdapter("19R3");
            case "1.20", "1.20.1" -> createVersionAdapter("20R1");
            case "1.20.2" -> createVersionAdapter("20R2");
            case "1.20.3", "1.20.4" -> createVersionAdapter("20R3");
            case "1.20.5", "1.20.6" -> createVersionAdapter("20R4");
            case "1.21" -> createVersionAdapter("21R1");
            default -> null;
        };
        if (versionAdapter == null) {
            this.getLogger().severe("Unsupported server version");
            this.getPluginLoader().disablePlugin(this);
            return;
        }

        String renderType = this.getConfig().getString("rendering", "DISPLAY");
        if (renderType.equals("DISPLAY")) {
            final String[] verSplit = version.split("\\.");
            final int minorVer = Integer.parseInt(verSplit[1]);
            final int patchVer = Integer.parseInt(verSplit.length == 3 ? verSplit[2] : "0");
            if (minorVer < 19 || (minorVer == 19 && patchVer != 4)) {
                renderType = "MAP";
                this.getLogger().warning("Render style 'DISPLAY' is not supported in version %s. Falling back to 'MAP'".formatted(version));
            }
        }
        final String finalRenderType = renderType; // Java does Java things

        final VisualizationController visualizationController = switch (renderType) {
            case "MAP" -> {
                // Initialize textures
                final File textureCacheFile = new File(this.getDataFolder(), "textures_" + minor);
                final TextureCache textureCache = new TextureCache();
                if (textureCacheFile.exists()) {
                    textureCache.read(textureCacheFile);
                } else {
                    final TextureDownloader downloader = new TextureDownloader();
                    downloader.downloadTextures(this.getLogger(), new File(this.getDataFolder(), "temp"), textureCache)
                            .whenComplete((unused, throwable) -> {
                                if (throwable != null) {
                                    this.getLogger().log(Level.SEVERE, "Failed to download textures", throwable);
                                }

                                downloader.shutdown();
                                textureCache.write(textureCacheFile);
                            });
                }

                yield new MapVisualizationController(versionAdapter, textureCache);
            }
            case "DISPLAY" -> new DisplayVisualizationController(versionAdapter);
            default -> null;
        };
        if (visualizationController == null) {
            this.getLogger().severe("Unsupported render style");
            this.getPluginLoader().disablePlugin(this);
            return;
        }
        if (!visualizationController.accepts(versionAdapter)) {
            this.getLogger().severe("Controller '%s' does not accept version adapter for version %s"
                    .formatted(visualizationController.getClass().getSimpleName(), version));
            this.getLogger().severe("This usually means that you're using an unsupported render style. " +
                                    "Changing the render style to 'MAP' should resolve this issue.");
            this.getPluginLoader().disablePlugin(this);
            return;
        }

        versionAdapter.init(this, (player, integer) ->
                this.getServer().getScheduler().runTask(this, () ->
                        visualizationController.entityClick(player, integer)));

        this.getServer().getPluginManager().registerEvents(new CraftingListener(this, visualizationController), this);
        this.getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, versionAdapter), this);

        this.getLogger().info("Visual Crafting was enabled!");
        this.getLogger().info("Using version adapter '%s' and controller '%s'"
                .formatted(versionAdapter.getClass().getSimpleName(), visualizationController.getClass().getSimpleName()));

        // bStats
        final Metrics metrics = new Metrics(this, 14561);
        metrics.addCustomChart(new SimplePie("enable_permission", () ->
                this.getConfig().getBoolean("permission.enable", false) ? "True" : "False"));
        metrics.addCustomChart(new SimplePie("enable_hitbox", () ->
                this.adjustHitbox() ? "True" : "False"));
        metrics.addCustomChart(new SimplePie("enable_packet_listening", () ->
                this.enablePacketListening() ? "True" : "False"));
        metrics.addCustomChart(new SimplePie("render_style", () -> this.normalize(finalRenderType)));
        metrics.addCustomChart(new SimplePie("download_source", this::getDownloadSource));
    }

    public boolean canUse(final Permissible permissible) {
        return !this.getConfig().getBoolean("permission.enable", false)
               || permissible.hasPermission(this.getConfig().getString("permission.perm", ""));
    }

    @Override
    public int entityIdRangeMin() {
        return this.getConfig().getInt("entity-id.min");
    }

    @Override
    public int entityIdRangeMax() {
        return this.getConfig().getInt("entity-id.max");
    }

    @Override
    public int mapIdRangeMin() {
        return this.getConfig().getInt("map-id.min");
    }

    @Override
    public int mapIdRangeMax() {
        return this.getConfig().getInt("map-id.max");
    }

    @Override
    public boolean adjustHitbox() {
        return this.getConfig().getBoolean("adjust-hitbox");
    }

    @Override
    public boolean enablePacketListening() {
        return this.getConfig().getBoolean("enable-packet-listening", true);
    }

    private String normalize(final String s) {
        if (s.length() <= 1) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    // Required to prevent the JVM from complaining about java versions
    private VersionAdapter createVersionAdapter(String versionSlug) {
        try {
            String classPath = "dev.cerus.visualcrafting.v%s.VersionAdapter%s".formatted(versionSlug.toLowerCase(), versionSlug.toUpperCase());
            Class<?> cls = Class.forName(classPath);
            return (VersionAdapter) cls.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            getLogger().log(Level.SEVERE, "Could not create version adapter", e);
            return null;
        }
    }

    private String getDownloadSource() {
        StringBuilder sourceBuilder = new StringBuilder();
        try (InputStream in = VisualCraftingPlugin.class.getClassLoader().getResourceAsStream("metadata")) {
            if (in == null) {
                return "unknown";
            }
            byte[] buffer = new byte[64];
            int read;
            while ((read = in.read(buffer)) != -1) {
                sourceBuilder.append(new String(buffer, 0, read));
            }
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Failed to read metadata file", e);
            return "unknown";
        }
        return sourceBuilder.toString();
    }

}
