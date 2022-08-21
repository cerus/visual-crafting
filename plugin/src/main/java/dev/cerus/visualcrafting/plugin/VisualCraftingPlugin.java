package dev.cerus.visualcrafting.plugin;

import dev.cerus.visualcrafting.api.config.Config;
import dev.cerus.visualcrafting.api.version.VersionAdapter;
import dev.cerus.visualcrafting.plugin.listener.CraftingListener;
import dev.cerus.visualcrafting.plugin.listener.listener.PlayerJoinListener;
import dev.cerus.visualcrafting.plugin.texture.TextureCache;
import dev.cerus.visualcrafting.plugin.texture.TextureDownloader;
import dev.cerus.visualcrafting.plugin.visualizer.VisualizationController;
import dev.cerus.visualcrafting.v16r3.VersionAdapter16R3;
import dev.cerus.visualcrafting.v17r1.VersionAdapter17R1;
import dev.cerus.visualcrafting.v18r1.VersionAdapter18R1;
import dev.cerus.visualcrafting.v18r2.VersionAdapter18R2;
import dev.cerus.visualcrafting.v19r1.VersionAdapter19R1;
import java.io.File;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.java.JavaPlugin;

public class VisualCraftingPlugin extends JavaPlugin implements Config {

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        // Initialize textures
        final File textureCacheFile = new File(this.getDataFolder(), "textures");
        final TextureCache textureCache = new TextureCache();
        if (textureCacheFile.exists()) {
            textureCache.read(textureCacheFile);
        } else {
            final TextureDownloader downloader = new TextureDownloader();
            downloader.downloadTextures(this.getLogger(), new File(this.getDataFolder(), "temp"), textureCache)
                    .whenComplete((unused, throwable) -> {
                        downloader.shutdown();
                        textureCache.write(textureCacheFile);
                    });
        }

        // Initialize version adapter
        String version = Bukkit.getVersion();
        version = version.substring(version.indexOf("MC: ") + 4, version.lastIndexOf(')'));
        final VersionAdapter versionAdapter = switch (version) {
            case "1.16.5" -> new VersionAdapter16R3();
            case "1.17", "1.17.1" -> new VersionAdapter17R1();
            case "1.18.1" -> new VersionAdapter18R1();
            case "1.18.2" -> new VersionAdapter18R2();
            case "1.19", "1.19.1", "1.19.2" -> new VersionAdapter19R1();
            default -> null;
        };
        if (versionAdapter == null) {
            this.getLogger().severe("Unsupported server version");
            this.getPluginLoader().disablePlugin(this);
            return;
        }

        final VisualizationController visualizationController = new VisualizationController(versionAdapter, textureCache);
        versionAdapter.init(this, (player, integer) ->
                this.getServer().getScheduler().runTask(this, () ->
                        visualizationController.entityClick(player, integer)));

        this.getServer().getPluginManager().registerEvents(new CraftingListener(this, visualizationController), this);
        this.getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, versionAdapter), this);

        this.getLogger().info("Visual Crafting was enabled!");
        this.getLogger().info("Using version adapter '" + versionAdapter.getClass().getSimpleName() + "'");

        // bStats
        final Metrics metrics = new Metrics(this, 14561);
        metrics.addCustomChart(new SimplePie("enable_permission", () ->
                this.getConfig().getBoolean("permission.enable", false) ? "True" : "False"));
        metrics.addCustomChart(new SimplePie("enable_hitbox", () ->
                this.adjustHitbox() ? "True" : "False"));
        metrics.addCustomChart(new SimplePie("enable_packet_listening", () ->
                this.enablePacketListening() ? "True" : "False"));
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

}
