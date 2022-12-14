package dev.cerus.visualcrafting.plugin.visualizer;

import java.util.Comparator;
import java.util.Map;

import org.bukkit.block.BlockFace;

class DirectionProvider {

    private final static Map<Integer, BlockFace> DIRECTION_MAP = Map.of(
            0, BlockFace.SOUTH,
            360, BlockFace.SOUTH,
            90, BlockFace.WEST,
            180, BlockFace.NORTH,
            270, BlockFace.EAST
    );

    /**
     * Calculate a direction from a yaw value
     *
     * @param yaw The yaw
     *
     * @return The direction
     */
    public static BlockFace getDirection(final float yaw) {
        final int yawInt = (int) (yaw < 0 ? 180 + (180 - -yaw) : yaw);
        return DIRECTION_MAP.entrySet().stream()
                .map(e -> {
                    final double diff = (double) Math.max(yawInt, e.getKey()) - Math.min(yawInt, e.getKey());
                    return Map.entry(diff, e);
                })
                .sorted(Comparator.comparingDouble(Map.Entry::getKey))
                .map(e -> e.getValue().getValue())
                .findFirst()
                .orElseThrow();
    }
}
