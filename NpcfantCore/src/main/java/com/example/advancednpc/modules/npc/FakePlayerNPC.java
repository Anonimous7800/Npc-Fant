package com.example.advancednpc.modules.npc;

import com.example.advancednpc.modules.skin.SkinData;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class FakePlayerNPC {


    public static final String STEVE_VALUE =
        "ewogICJ0aW1lc3RhbXAiIDogMTcxMDAwMDAwMDAwMCwKICAicHJvZmlsZUlkIiA6" +
        "ICIwNjlhNzllNTRkODQ0MDY1YWE2ODU5MjZhNjQxMDdhYiIsCiAgInByb2ZpbGVOYW1lIiA6" +
        "ICJTdGV2ZSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6" +
        "IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVj" +
        "cmFmdC5uZXQvdGV4dHVyZS82MGEzNWFiMWM3MDI0MzQ0OWVmZTZiM2EwYzYwY2ExOWVhNTk4" +
        "MmI3NDZhMzYzNWEzYzRlM2VjMjZhNjZlIgogICAgfQogIH0KfQ==";
    public static final String STEVE_SIGNATURE = "";


    private final int          entityId;
    private final UUID         uuid;
    private       String       name;
    private       Location     location;
    private       SkinData     skinData;
    private final Plugin       plugin;


    private final Set<UUID> spawnedFor = new HashSet<>();

    public FakePlayerNPC(UUID uuid, String name, Location location, Plugin plugin) {
        this.uuid     = uuid;
        this.name     = name;
        this.location = location.clone();
        this.plugin   = plugin;
        this.entityId = (int)(Math.random() * Integer.MAX_VALUE / 2) + 1000;

        this.skinData = new SkinData(STEVE_VALUE, STEVE_SIGNATURE);
    }




    public void spawnForAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            spawnForPlayer(p);
        }
    }


    public void spawnForPlayer(Player player) {
        if (spawnedFor.contains(player.getUniqueId())) return;

        try {
            var channel = PacketEvents.getAPI().getPlayerManager().getChannel(player);
            if (channel == null) return;


            sendAddToTabList(player);


            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    sendSpawnPacket(player);
                    sendMetadataPacket(player);
                    spawnedFor.add(player.getUniqueId());


                    Bukkit.getScheduler().runTaskLater(plugin, () ->
                        sendRemoveFromTabList(player), 40L);
                } catch (Exception e) {
                    plugin.getLogger().warning("[NpcFant] Error al spawnear NPC para " +
                        player.getName() + ": " + e.getMessage());
                }
            }, 2L);

        } catch (Exception e) {
            plugin.getLogger().warning("[NpcFant] Error en spawnForPlayer: " + e.getMessage());
        }
    }




    public void despawnForAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            despawnForPlayer(p);
        }
        spawnedFor.clear();
    }


    public void despawnForPlayer(Player player) {
        if (!spawnedFor.contains(player.getUniqueId())) return;
        try {
            WrapperPlayServerDestroyEntities destroyPacket =
                new WrapperPlayServerDestroyEntities(entityId);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroyPacket);
            sendRemoveFromTabList(player);
            spawnedFor.remove(player.getUniqueId());
        } catch (Exception e) {
            plugin.getLogger().warning("[NpcFant] Error al despawnear: " + e.getMessage());
        }
    }




    public void updateSkin(SkinData newSkin) {
        this.skinData = newSkin;

        Set<UUID> hadSpawned = new HashSet<>(spawnedFor);
        despawnForAll();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID uid : hadSpawned) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null) spawnForPlayer(p);
            }
        }, 5L);
    }




    public void teleport(Location newLocation) {
        this.location = newLocation.clone();
        for (UUID uid : new HashSet<>(spawnedFor)) {
            Player p = Bukkit.getPlayer(uid);
            if (p == null) continue;
            try {
                WrapperPlayServerEntityTeleport teleportPacket = new WrapperPlayServerEntityTeleport(
                    entityId,
                    new Vector3d(newLocation.getX(), newLocation.getY(), newLocation.getZ()),
                    newLocation.getYaw(),
                    newLocation.getPitch(),
                    false
                );
                PacketEvents.getAPI().getPlayerManager().sendPacket(p, teleportPacket);
            } catch (Exception e) {
                plugin.getLogger().warning("[NpcFant] Error al teletransportar NPC: " + e.getMessage());
            }
        }
    }


    public void lookAt(Player target) {
        Location eye  = location.clone().add(0, 1.62, 0);
        Location tEye = target.getEyeLocation();
        double   dx   = tEye.getX() - eye.getX();
        double   dy   = tEye.getY() - eye.getY();
        double   dz   = tEye.getZ() - eye.getZ();
        double   dist = Math.sqrt(dx * dx + dz * dz);
        float    yaw  = (float)(Math.toDegrees(Math.atan2(-dx, dz)));
        float    pitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));

        for (UUID uid : new HashSet<>(spawnedFor)) {
            Player p = Bukkit.getPlayer(uid);
            if (p == null) continue;
            try {
                WrapperPlayServerEntityHeadLook headLookPacket =
                    new WrapperPlayServerEntityHeadLook(entityId, yaw);
                WrapperPlayServerEntityRotation rotPacket =
                    new WrapperPlayServerEntityRotation(entityId, yaw, pitch, true);
                PacketEvents.getAPI().getPlayerManager().sendPacket(p, headLookPacket);
                PacketEvents.getAPI().getPlayerManager().sendPacket(p, rotPacket);
            } catch (Exception ignored) {}
        }
    }



    private void sendAddToTabList(Player player) {
        List<TextureProperty> textures = skinData != null && skinData.getValue() != null
                && !skinData.getValue().isEmpty()
            ? Collections.singletonList(new TextureProperty("textures", skinData.getValue(),
                skinData.getSignature()))
            : Collections.emptyList();

        UserProfile profile = new UserProfile(uuid, name, textures);

        WrapperPlayServerPlayerInfoUpdate infoUpdate = new WrapperPlayServerPlayerInfoUpdate(
            EnumSet.of(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                       WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED),
            new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(profile, false,
                0, GameMode.SURVIVAL, null, null)
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, infoUpdate);
    }

    private void sendSpawnPacket(Player player) {
        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
            entityId, Optional.of(uuid), EntityTypes.PLAYER,
            new Vector3d(location.getX(), location.getY(), location.getZ()),
            location.getPitch(), location.getYaw(), location.getYaw(), 0,
            Optional.empty()
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnPacket);
    }

    private void sendMetadataPacket(Player player) {

        List<EntityData> metadata = new ArrayList<>();
        metadata.add(new EntityData(17, EntityDataTypes.BYTE, (byte) 0x7F));
        WrapperPlayServerEntityMetadata metaPacket =
            new WrapperPlayServerEntityMetadata(entityId, metadata);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, metaPacket);
    }

    private void sendRemoveFromTabList(Player player) {
        try {
            WrapperPlayServerPlayerInfoRemove removePacket =
                new WrapperPlayServerPlayerInfoRemove(Collections.singletonList(uuid));
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, removePacket);
        } catch (Exception ignored) {}
    }


    public int      getEntityId()  { return entityId; }
    public UUID     getUUID()      { return uuid; }
    public String   getName()      { return name; }
    public void     setName(String n) { this.name = n; }
    public Location getLocation()  { return location.clone(); }
    public SkinData getSkinData()  { return skinData; }
    public boolean  isSpawnedFor(UUID uid) { return spawnedFor.contains(uid); }
    public Set<UUID> getSpawnedFor() { return Collections.unmodifiableSet(spawnedFor); }
}
