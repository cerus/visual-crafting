package dev.cerus.visualcrafting.v16r3;

import dev.cerus.visualcrafting.api.config.Config;
import dev.cerus.visualcrafting.api.version.FakeMap;
import dev.cerus.visualcrafting.api.version.VersionAdapter;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.server.v1_16_R3.DataWatcher;
import net.minecraft.server.v1_16_R3.DataWatcherRegistry;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.Packet;
import net.minecraft.server.v1_16_R3.PacketPlayInUseEntity;
import net.minecraft.server.v1_16_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_16_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_16_R3.PacketPlayOutMap;
import net.minecraft.server.v1_16_R3.PacketPlayOutSpawnEntity;
import net.minecraft.server.v1_16_R3.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class VersionAdapter16R3 extends VersionAdapter {

    private Config config;
    private BiConsumer<Player, Integer> entityClickCallback;
    private int nextEntityId;
    private int nextMapId;

    @Override
    public void init(final Config config, final BiConsumer<Player, Integer> entityClickCallback) {
        this.config = config;
        this.entityClickCallback = entityClickCallback;
        this.nextEntityId = config.entityIdRangeMin();
        this.nextMapId = config.mapIdRangeMin();
    }

    @Override
    public void inject(final Player player) {
        if (this.config.enablePacketListening()) {
            ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel.pipeline()
                    .addBefore("packet_handler", "visual_crafting", new ChannelDuplexHandler() {
                        @Override
                        public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
                            if (msg instanceof PacketPlayInUseEntity useEntity) {
                                VersionAdapter16R3.this.handlePacketIn(player, useEntity);
                            }
                            super.channelRead(ctx, msg);
                        }
                    });
        }
    }

    private void handlePacketIn(final Player player, final PacketPlayInUseEntity packet) {
        try {
            final Field a = packet.getClass().getDeclaredField("a");
            a.setAccessible(true);
            this.entityClickCallback.accept(player, (Integer) a.get(packet));
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
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
                EntityTypes.ITEM_FRAME,
                this.config.adjustHitbox() ? 0 : switch (direction) {
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
        final PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata();
        this.setField(packet, "a", frameId);
        this.setField(packet, "b", Arrays.asList(
                new DataWatcher.Item<>(DataWatcherRegistry.g.a(7), CraftItemStack.asNMSCopy(itemStack)),
                new DataWatcher.Item<>(DataWatcherRegistry.b.a(8), rotation.ordinal()),
                new DataWatcher.Item<>(DataWatcherRegistry.a.a(0), (byte) (invisible ? 0x20 : 0))
        ));
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
                false,
                false,
                Set.of(),
                this.getMapData(map),
                0,
                0,
                128,
                128
        );
        Bukkit.getOnlinePlayers().forEach(player -> this.sendPacket(player, packet));
    }

    private void sendPacket(final Player player, final Packet<?> packet) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
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

    private void setField(final Object o, final String field, final Object value) {
        try {
            final Field declaredField = o.getClass().getDeclaredField(field);
            declaredField.setAccessible(true);
            declaredField.set(o, value);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
