package com.example.advancednpc.modules.hologram;

import com.example.advancednpc.api.IHologram;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Hologram implements IHologram {

    private static final AtomicInteger NEXT_HOLO_ENTITY_ID = new AtomicInteger(500000);
    private static final double LINE_HEIGHT = 0.28;

    private final UUID   id;
    private final Plugin plugin;
    private Location     baseLocation;
    private final List<HologramLine> lines       = new CopyOnWriteArrayList<>();
    private final List<Integer>      lineIds     = new CopyOnWriteArrayList<>();
    private final List<UUID>         lineUUIDs   = new CopyOnWriteArrayList<>();
    private final Set<UUID>          hiddenFrom  = ConcurrentHashMap.newKeySet();
    private final Set<UUID>          spawnedFor  = ConcurrentHashMap.newKeySet();
    private volatile boolean         alive       = true;

    public Hologram(UUID id, Plugin plugin, Location location) {
        this.id           = id;
        this.plugin       = plugin;
        this.baseLocation = location.clone();
    }

    @Override public UUID getId() { return id; }

    @Override
    public void addLine(String text) {
        lines.add(new HologramLine(text));
        lineIds.add(NEXT_HOLO_ENTITY_ID.getAndIncrement());
        lineUUIDs.add(UUID.randomUUID());
        scheduleRefresh();
    }

    public void insertLine(int index, String text) {
        int safeIdx = Math.max(0, Math.min(index, lines.size()));
        lines.add(safeIdx, new HologramLine(text));
        lineIds.add(safeIdx, NEXT_HOLO_ENTITY_ID.getAndIncrement());
        lineUUIDs.add(safeIdx, UUID.randomUUID());
        scheduleRefresh();
    }

    @Override
    public void setLine(int index, String text) {
        if (index < 0 || index >= lines.size()) return;
        lines.get(index).setText(text);
        updateTextsForAll();
    }

    @Override
    public void removeLine(int index) {
        if (index < 0 || index >= lines.size()) return;
        despawnForAll();
        lines.remove(index);
        if (index < lineIds.size())   lineIds.remove(index);
        if (index < lineUUIDs.size()) lineUUIDs.remove(index);
        scheduleRefresh();
    }

    public void setAllLines(List<String> newLines) {
        despawnForAll();
        lines.clear();
        lineIds.clear();
        lineUUIDs.clear();
        if (newLines != null) {
            for (String s : newLines) {
                lines.add(new HologramLine(s));
                lineIds.add(NEXT_HOLO_ENTITY_ID.getAndIncrement());
                lineUUIDs.add(UUID.randomUUID());
            }
        }
        scheduleRefresh();
    }

    @Override
    public List<String> getLines() {
        List<String> texts = new ArrayList<>();
        for (HologramLine l : lines) texts.add(l.getRawText());
        return texts;
    }

    @Override public Location getLocation() { return baseLocation.clone(); }

    @Override
    public void move(Location location) {
        this.baseLocation = location.clone();
        scheduleRefresh();
    }

    @Override
    public void showTo(Player player) {
        if (player == null) return;
        hiddenFrom.remove(player.getUniqueId());
        spawnForPlayer(player);
    }

    @Override
    public void hideFrom(Player player) {
        if (player == null) return;
        hiddenFrom.add(player.getUniqueId());
        despawnForPlayer(player);
    }

    @Override
    public void delete() {
        alive = false;
        despawnForAll();
    }

    @Override public boolean isAlive() { return alive; }

    public void scheduleRefresh() {
        if (Bukkit.isPrimaryThread()) {
            refresh();
        } else {
            Bukkit.getScheduler().runTask(plugin, this::refresh);
        }
    }

    public void refresh() {
        despawnForAll();
        if (!alive || baseLocation.getWorld() == null) return;
        for (Player p : baseLocation.getWorld().getPlayers()) {
            if (!hiddenFrom.contains(p.getUniqueId())
                    && p.getLocation().distanceSquared(baseLocation) <= 60.0 * 60.0) {
                spawnForPlayer(p);
            }
        }
    }

    public void spawnForPlayer(Player player) {
        if (!alive || player == null || !player.isOnline()) return;
        if (hiddenFrom.contains(player.getUniqueId())) return;
        if (baseLocation.getWorld() == null || !player.getWorld().equals(baseLocation.getWorld())) return;
        if (spawnedFor.contains(player.getUniqueId())) return;

        int totalLines = lines.size();
        if (totalLines == 0) return;

        try {
            var channel = PacketEvents.getAPI().getPlayerManager().getChannel(player);
            if (channel == null) return;

            spawnedFor.add(player.getUniqueId());

            for (int i = 0; i < totalLines && i < lineIds.size(); i++) {
                HologramLine line = lines.get(i);
                if (line.isEmpty()) continue;

                int entityId = lineIds.get(i);
                UUID lineUuid = lineUUIDs.get(i);
                double yOffset = (totalLines - 1 - i) * LINE_HEIGHT;
                Location lineLoc = baseLocation.clone().add(0, yOffset, 0);

                WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                    entityId,
                    Optional.of(lineUuid),
                    EntityTypes.ARMOR_STAND,
                    new Vector3d(lineLoc.getX(), lineLoc.getY(), lineLoc.getZ()),
                    0f, 0f, 0f, 0,
                    Optional.empty()
                );
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnPacket);
                sendLineMetadata(player, entityId, line);
            }
        } catch (Exception ignored) {
            spawnedFor.remove(player.getUniqueId());
        }
    }

    public void despawnForPlayer(Player player) {
        if (player == null) return;
        if (!spawnedFor.remove(player.getUniqueId())) return;
        if (lineIds.isEmpty()) return;

        try {
            int[] ids = lineIds.stream().mapToInt(Integer::intValue).toArray();
            WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(ids);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroyPacket);
        } catch (Exception ignored) {}
    }

    public void despawnForAll() {
        for (UUID uid : new HashSet<>(spawnedFor)) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) {
                despawnForPlayer(p);
            }
        }
        spawnedFor.clear();
    }

    public void removePlayerFromTracking(UUID playerId) {
        spawnedFor.remove(playerId);
    }

    public boolean isSpawnedFor(UUID playerId) {
        return spawnedFor.contains(playerId);
    }

    public void updateTextsForAll() {
        for (UUID uid : new HashSet<>(spawnedFor)) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) {
                updateTextsForPlayer(p);
            }
        }
    }

    public void updateTextsForPlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        int totalLines = lines.size();
        for (int i = 0; i < totalLines && i < lineIds.size(); i++) {
            sendLineMetadata(player, lineIds.get(i), lines.get(i));
        }
    }

    private void sendLineMetadata(Player player, int entityId, HologramLine line) {
        try {
            String displayText = line.getDisplayText(player);
            Component comp = LegacyComponentSerializer.legacySection().deserialize(displayText);

            List<EntityData<?>> metadata = new ArrayList<>();
            metadata.add(new EntityData<>(0, EntityDataTypes.BYTE, (byte) 0x20));
            metadata.add(new EntityData<>(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.of(comp)));
            metadata.add(new EntityData<>(3, EntityDataTypes.BOOLEAN, !line.isEmpty()));
            metadata.add(new EntityData<>(5, EntityDataTypes.BOOLEAN, true));
            metadata.add(new EntityData<>(15, EntityDataTypes.BYTE, (byte) (0x01 | 0x08 | 0x10)));

            WrapperPlayServerEntityMetadata metaPacket =
                new WrapperPlayServerEntityMetadata(entityId, metadata);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, metaPacket);
        } catch (Exception ignored) {}
    }

    public Set<UUID> getHiddenFrom() { return Collections.unmodifiableSet(hiddenFrom); }
}
