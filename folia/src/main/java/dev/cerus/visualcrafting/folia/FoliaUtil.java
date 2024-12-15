package dev.cerus.visualcrafting.folia;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

public class FoliaUtil {
    // Source: https://github.com/pop4959/Chunky/blob/0f5d81d5ea4c4062f514fd6d7d59c3f7006cc91d/folia/src/main/java/org/popcraft/chunky/platform/Folia.java
    private static final boolean IS_FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer") || classExists("io.papermc.paper.threadedregions.RegionizedServerInitEvent");

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public static void runIfFolia(Runnable runFolia, Runnable runElse) {
        if (isFolia()) {
            runFolia.run();
        } else {
            runElse.run();
        }
    }

    public static void scheduleOnEntity(JavaPlugin plugin, Entity entity, Runnable runnable, int delay) {
        entity.getScheduler().execute(plugin, runnable, null, delay);
    }

    public static void scheduleOnServer(JavaPlugin plugin, Runnable runnable) {
        Bukkit.getServer().getGlobalRegionScheduler().execute(plugin, runnable);
    }

    private static boolean classExists(final String clazz) {
        try {
            Class.forName(clazz);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
