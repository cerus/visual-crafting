package dev.cerus.visualcrafting.api.config;

/**
 * Simple configuration interface
 */
public interface Config {

    /**
     * Minimum entity id to use
     *
     * @return Min entity id
     */
    int entityIdRangeMin();

    /**
     * Maximum entity id to use
     *
     * @return Max entity id
     */
    int entityIdRangeMax();

    /**
     * Minimum map id to use
     *
     * @return Min map id
     */
    int mapIdRangeMin();

    /**
     * Maximum map id to use
     *
     * @return Max map id
     */
    int mapIdRangeMax();

    /**
     * Whether to always force the hitbox to the top or not
     *
     * @return True or false
     */
    boolean adjustHitbox();

}
