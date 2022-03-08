package dev.cerus.visualcrafting.v18r1;

import dev.cerus.visualcrafting.api.config.Config;
import dev.cerus.visualcrafting.api.version.FakeMap;
import dev.cerus.visualcrafting.api.version.VersionAdapter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutMap;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.saveddata.maps.WorldMap;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class VersionAdapter18R1 extends VersionAdapter {

    private Config config;
    private int nextEntityId;
    private int nextMapId;

    @Override
    public void init(final Config config) {
        this.config = config;
        this.nextEntityId = config.entityIdRangeMin();
        this.nextMapId = config.mapIdRangeMin();
    }

    @Override
    public int spawnItemFrame(final Location location, final BlockFace direction) {
        final int eid = this.getNewEntityId();
        final PacketPlayOutSpawnEntity packet = new PacketPlayOutSpawnEntity(
                eid,
                UUID.randomUUID(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                direction == BlockFace.DOWN ? 90 : direction == BlockFace.UP ? -90 : 0,
                switch (direction) {
                    case NORTH -> -180;
                    case EAST -> -90;
                    case WEST -> 90;
                    default -> 0;
                },
                EntityTypes.R,
                switch (direction) {
                    case UP -> 1;
                    case NORTH -> 2;
                    case SOUTH -> 3;
                    case WEST -> 4;
                    case EAST -> 5;
                    default -> 0;
                },
                new Vec3D(0, 0, 0)
        );
        Bukkit.getOnlinePlayers().forEach(player -> this.sendPacket(player, packet));
        return eid;
    }

    @Override
    public void updateItemFrame(final int frameId, final ItemStack itemStack, final Rotation rotation, final boolean invisible) {
        final PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(frameId, new DataWatcher(null) {
            @Override
            public @Nullable
            List<Item<?>> b() {
                return Arrays.asList(
                        new DataWatcher.Item<>(DataWatcherRegistry.g.a(8), CraftItemStack.asNMSCopy(itemStack)),
                        new DataWatcher.Item<>(DataWatcherRegistry.b.a(9), rotation.ordinal()),
                        new DataWatcher.Item<>(DataWatcherRegistry.a.a(0), (byte) (invisible ? 0x20 : 0))
                );
            }
        }, false);
        Bukkit.getOnlinePlayers().forEach(player -> this.sendPacket(player, packet));
    }

    @Override
    public void destroyEntity(final int entityId) {
        final PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(entityId);
        Bukkit.getOnlinePlayers().forEach(player -> this.sendPacket(player, packet));
    }

    @Override
    public FakeMap createMap() {
        return this.createMap(this.getNewMapId());
    }

    @Override
    public void sendMap(final FakeMap map) {
        final PacketPlayOutMap packet = new PacketPlayOutMap(
                map.getId(),
                (byte) 0,
                true,
                Collections.emptyList(),
                new WorldMap.b(0,
                        0,
                        128,
                        128,
                        this.getMapData(map))
        );
        Bukkit.getOnlinePlayers().forEach(player -> this.sendPacket(player, packet));
    }

    private void sendPacket(final Player player, final Packet<?> packet) {
        ((CraftPlayer) player).getHandle().b.a(packet);
    }

    private int getNewEntityId() {
        if (this.nextEntityId >= this.config.entityIdRangeMax()) {
            this.nextEntityId = this.config.entityIdRangeMin();
            return this.nextEntityId;
        } else {
            return this.nextEntityId++;
        }
    }

    private int getNewMapId() {
        if (this.nextMapId >= this.config.mapIdRangeMax()) {
            this.nextMapId = this.config.mapIdRangeMin();
            return this.nextMapId;
        } else {
            return this.nextMapId++;
        }
    }

}
