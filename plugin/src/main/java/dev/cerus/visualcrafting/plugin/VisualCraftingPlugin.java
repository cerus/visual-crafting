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
import dev.cerus.visualcrafting.v16r3.VersionAdapter16R3;
import dev.cerus.visualcrafting.v17r1.VersionAdapter17R1;
import dev.cerus.visualcrafting.v18r1.VersionAdapter18R1;
import dev.cerus.visualcrafting.v18r2.VersionAdapter18R2;
import dev.cerus.visualcrafting.v19r1.VersionAdapter19R1;
import dev.cerus.visualcrafting.v19r2.VersionAdapter19R2;
import dev.cerus.visualcrafting.v19r3.VersionAdapter19R3;
import dev.cerus.visualcrafting.v20r1.VersionAdapter20R1;
import dev.cerus.visualcrafting.v20r2.VersionAdapter20R2;
import dev.cerus.visualcrafting.v20r3.VersionAdapter20R3;
import dev.cerus.visualcrafting.v20r4.VersionAdapter20R4;
import java.io.File;
import java.net.URL;
import java.security.CodeSource;
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
            case "1.16.5" -> new VersionAdapter16R3();
            case "1.17", "1.17.1" -> new VersionAdapter17R1();
            case "1.18.1" -> new VersionAdapter18R1();
            case "1.18.2" -> new VersionAdapter18R2();
            case "1.19", "1.19.1", "1.19.2" -> new VersionAdapter19R1();
            case "1.19.3" -> new VersionAdapter19R2();
            case "1.19.4" -> new VersionAdapter19R3();
            case "1.20", "1.20.1" -> new VersionAdapter20R1();
            case "1.20.2" -> new VersionAdapter20R2();
            case "1.20.3", "1.20.4" -> new VersionAdapter20R3();
            case "1.20.5" -> new VersionAdapter20R4();
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

    private String getDownloadSource() {
        final CodeSource codeSource = VisualCraftingPlugin.class.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            return "unknown";
        }
        final URL location = codeSource.getLocation();
        String file = location.getFile();
        final String[] split = file.split("/");
        file = split[split.length - 1];
        final int lastDash = file.lastIndexOf('-');
        final int lastDot = file.lastIndexOf('.');
        if (lastDash == -1 || lastDot == -1 || lastDot < lastDash) {
            return "unknown";
        }
        return file.substring(lastDash + 1, lastDot).toLowerCase();
    }

}
