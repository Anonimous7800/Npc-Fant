package com.example.advancednpc.modules.npc;

import com.example.advancednpc.modules.animation.NPCPose;
import com.example.advancednpc.modules.equipment.NPCEquipment;
import com.example.advancednpc.modules.hologram.HologramLine;
import com.example.advancednpc.modules.skin.SkinData;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FakePlayerNPC {

    private static final AtomicInteger NEXT_ENTITY_ID = new AtomicInteger(100000);

    public static final String STEVE_VALUE     = "";
    public static final String STEVE_SIGNATURE = "";

    private final int          entityId;
    private final int          seatEntityId;
    private final UUID         uuid;
    private volatile UUID      profileUuid;
    private       String       name;
    private       Location     location;
    private       SkinData     skinData;
    private       NPCType      npcType      = NPCType.PLAYER;
    private       NPCPose      pose         = NPCPose.STANDING;
    private       NPCEquipment equipment    = new NPCEquipment();
    private       boolean      invisible    = false;
    private       boolean      glowing      = false;
    private       String       glowColor;
    private       boolean      showNametag  = true;
    private volatile boolean   respawning   = false;
    private final Plugin       plugin;

    private final Set<UUID> spawnedFor     = ConcurrentHashMap.newKeySet();
    private final Set<UUID> teamCreatedFor = ConcurrentHashMap.newKeySet();
    private final Set<UUID> seatSpawnedFor = ConcurrentHashMap.newKeySet();
    private final Map<UUID, float[]> lastLookAngles = new ConcurrentHashMap<>();

    public FakePlayerNPC(UUID uuid, String name, Location location, Plugin plugin) {
        this.uuid         = uuid;
        this.profileUuid  = uuid;
        this.name         = HologramLine.colorize(name);
        this.location     = location.clone();
        this.plugin       = plugin;
        this.entityId     = NEXT_ENTITY_ID.getAndIncrement();
        this.seatEntityId = NEXT_ENTITY_ID.getAndIncrement();
        this.skinData     = new SkinData(STEVE_VALUE, STEVE_SIGNATURE);
    }

    public void spawnForAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (location.getWorld() != null && p.getWorld().equals(location.getWorld())) {
                spawnForPlayer(p);
            }
        }
    }

    public void spawnForPlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        if (location.getWorld() == null || !player.getWorld().equals(location.getWorld())) return;
        if (respawning || spawnedFor.contains(player.getUniqueId())) return;

        try {
            var channel = PacketEvents.getAPI().getPlayerManager().getChannel(player);
            if (channel == null) return;

            spawnedFor.add(player.getUniqueId());
            final UUID activeProfileUuid = this.profileUuid;

            if (npcType == NPCType.PLAYER) {
                sendAddToTabList(player, activeProfileUuid);
                sendHideNametagTeamPacket(player);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline() || respawning || !spawnedFor.contains(player.getUniqueId())) return;
                    try {
                        sendSpawnPacket(player, activeProfileUuid);
                        sendRotationPackets(player, location.getYaw(), location.getPitch());
                        sendMetadataPacket(player);
                        sendEquipmentPackets(player);
                        if (pose == NPCPose.SITTING) {
                            spawnSeatForPlayer(player);
                        }

                        ServerVersion ver = PacketEvents.getAPI().getServerManager().getVersion();
                        if (ver.isOlderThan(ServerVersion.V_1_19_3)) {
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                if (player.isOnline()) {
                                    sendRemoveFromTabList(player, activeProfileUuid);
                                }
                            }, 40L);
                        }
                    } catch (Exception e) {
                        spawnedFor.remove(player.getUniqueId());
                        plugin.getLogger().warning("[NpcFant] Error al spawnear NPC para " +
                            player.getName() + ": " + e.getMessage());
                    }
                }, 2L);
            } else {
                sendSpawnPacket(player, activeProfileUuid);
                sendRotationPackets(player, location.getYaw(), location.getPitch());
                sendMetadataPacket(player);
                sendEquipmentPackets(player);
                if (pose == NPCPose.SITTING) {
                    spawnSeatForPlayer(player);
                }
            }

        } catch (Exception e) {
            spawnedFor.remove(player.getUniqueId());
            plugin.getLogger().warning("[NpcFant] Error en spawnForPlayer: " + e.getMessage());
        }
    }

    public void despawnForAll() {
        UUID oldProfileUuid = this.profileUuid;
        WrapperPlayServerDestroyEntities destroyPacket =
            new WrapperPlayServerDestroyEntities(entityId, seatEntityId);
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                PacketEvents.getAPI().getPlayerManager().sendPacket(p, destroyPacket);
                if (npcType == NPCType.PLAYER) {
                    sendRemoveFromTabList(p, oldProfileUuid);
                }
            } catch (Exception ignored) {}
        }
        spawnedFor.clear();
        seatSpawnedFor.clear();
        teamCreatedFor.clear();
        lastLookAngles.clear();
    }

    public void despawnForPlayer(Player player) {
        despawnForPlayerInternal(player, this.profileUuid);
    }

    private void despawnForPlayerInternal(Player player, UUID targetProfileUuid) {
        if (player == null) return;
        despawnSeatForPlayer(player);
        if (!spawnedFor.remove(player.getUniqueId())) return;
        try {
            WrapperPlayServerDestroyEntities destroyPacket =
                new WrapperPlayServerDestroyEntities(entityId);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroyPacket);
            if (npcType == NPCType.PLAYER) {
                sendRemoveFromTabList(player, targetProfileUuid);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[NpcFant] Error al despawnear: " + e.getMessage());
        }
    }

    public void removePlayerFromTracking(UUID playerId) {
        spawnedFor.remove(playerId);
        teamCreatedFor.remove(playerId);
        seatSpawnedFor.remove(playerId);
        lastLookAngles.remove(playerId);
    }

    public void respawnForAll() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::respawnForAll);
            return;
        }
        Set<UUID> hadSpawned = new HashSet<>(spawnedFor);
        this.respawning = true;
        despawnForAll();
        this.profileUuid = UUID.randomUUID();

        if (hadSpawned.isEmpty()) {
            this.respawning = false;
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            this.respawning = false;
            for (UUID uid : hadSpawned) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null && p.isOnline()) {
                    spawnForPlayer(p);
                }
            }
        }, 4L);
    }

    public void updateSkin(SkinData newSkin) {
        if (newSkin != null) {
            this.skinData = newSkin;
        }
        if (!spawnedFor.isEmpty()) {
            respawnForAll();
        } else {
            this.profileUuid = UUID.randomUUID();
        }
    }

    public void updateMetadataForAll() {
        for (UUID uid : new HashSet<>(spawnedFor)) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) {
                sendMetadataPacket(p);
            }
        }
    }

    public void updateEquipmentForAll() {
        for (UUID uid : new HashSet<>(spawnedFor)) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) {
                sendEquipmentPackets(p);
            }
        }
    }

    public void swingMainArm() {
        playAnimation(WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM);
    }

    public void playHappyParticles() {
        if (location.getWorld() == null) return;
        try {
            org.bukkit.Particle p;
            try {
                p = org.bukkit.Particle.valueOf("HAPPY_VILLAGER");
            } catch (Exception e) {
                p = org.bukkit.Particle.valueOf("VILLAGER_HAPPY");
            }
            location.getWorld().spawnParticle(
                p,
                location.clone().add(0, 1.8, 0),
                10,
                0.3, 0.3, 0.3,
                0.05
            );
        } catch (Exception ignored) {}
    }

    public void playAnimation(WrapperPlayServerEntityAnimation.EntityAnimationType animationType) {
        for (UUID uid : new HashSet<>(spawnedFor)) {
            Player p = Bukkit.getPlayer(uid);
            if (p == null || !p.isOnline()) continue;
            try {
                WrapperPlayServerEntityAnimation animPacket =
                    new WrapperPlayServerEntityAnimation(entityId, animationType);
                PacketEvents.getAPI().getPlayerManager().sendPacket(p, animPacket);
            } catch (Exception ignored) {}
        }
    }

    public void teleport(Location newLocation) {
        this.location = newLocation.clone();
        for (UUID uid : new HashSet<>(spawnedFor)) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) {
                despawnForPlayer(p);
            }
        }
        spawnedFor.clear();
        seatSpawnedFor.clear();

        if (newLocation.getWorld() != null) {
            for (Player p : newLocation.getWorld().getPlayers()) {
                if (p.isOnline() && p.getLocation().distanceSquared(newLocation) <= (48.0 * 48.0)) {
                    spawnForPlayer(p);
                }
            }
        }
    }

    public void lookAt(Player target) {
        if (target == null || !target.isOnline() || location.getWorld() == null) return;
        if (!target.getWorld().equals(location.getWorld())) return;

        Location eye  = location.clone().add(0, 1.62, 0);
        Location tEye = target.getEyeLocation();
        double   dx   = tEye.getX() - eye.getX();
        double   dy   = tEye.getY() - eye.getY();
        double   dz   = tEye.getZ() - eye.getZ();
        double   dist = Math.sqrt(dx * dx + dz * dz);
        float    yaw   = (float)(Math.toDegrees(Math.atan2(-dx, dz)));
        while (yaw > 180f) yaw -= 360f;
        while (yaw < -180f) yaw += 360f;
        float    pitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));
        if (pitch < -90f) pitch = -90f;
        if (pitch > 90f)  pitch = 90f;

        sendRotationPackets(target, yaw, pitch);
    }

    private void sendRotationPackets(Player player, float yaw, float pitch) {
        if (player == null || !player.isOnline()) return;
        UUID uid = player.getUniqueId();
        float[] last = lastLookAngles.get(uid);
        if (last != null && Math.abs(last[0] - yaw) < 0.5f && Math.abs(last[1] - pitch) < 0.5f) {
            return;
        }
        lastLookAngles.put(uid, new float[]{yaw, pitch});

        try {
            if (this.pose == NPCPose.SITTING) {
                WrapperPlayServerEntityRotation seatRot =
                    new WrapperPlayServerEntityRotation(seatEntityId, yaw, 0f, true);
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, seatRot);
                WrapperPlayServerEntityHeadLook seatHead =
                    new WrapperPlayServerEntityHeadLook(seatEntityId, yaw);
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, seatHead);
            } else {
                if (!sendNativePositionSync(player, entityId, location, yaw, pitch)) {
                    WrapperPlayServerEntityRotation rotPacket =
                        new WrapperPlayServerEntityRotation(entityId, yaw, pitch, true);
                    PacketEvents.getAPI().getPlayerManager().sendPacket(player, rotPacket);
                }
            }
            WrapperPlayServerEntityHeadLook headLookPacket =
                new WrapperPlayServerEntityHeadLook(entityId, yaw);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, headLookPacket);
        } catch (Exception ignored) {}
    }

    private static volatile boolean nativeSyncInitialized = false;
    private static java.lang.reflect.Constructor<?> nativeVec3Ctor;
    private static java.lang.reflect.Method nativePosPathOfMethod;
    private static java.lang.reflect.Constructor<?> nativeSyncPacketCtor;
    private static java.lang.reflect.Method nativeGetHandleMethod;
    private static java.lang.reflect.Field nativeConnectionField;
    private static java.lang.reflect.Method nativeSendPacketMethod;

    private static boolean initNativeSync() {
        if (nativeSyncInitialized) return nativeSyncPacketCtor != null;
        synchronized (FakePlayerNPC.class) {
            if (nativeSyncInitialized) return nativeSyncPacketCtor != null;
            try {
                Class<?> vec3Class = Class.forName("net.minecraft.world.phys.Vec3");
                nativeVec3Ctor = vec3Class.getConstructor(double.class, double.class, double.class);

                Class<?> posPathClass = Class.forName("net.minecraft.world.entity.PositionPath");
                nativePosPathOfMethod = posPathClass.getMethod("of", vec3Class);

                Class<?> syncPacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket");
                nativeSyncPacketCtor = syncPacketClass.getConstructor(int.class, posPathClass, float.class, float.class, boolean.class);

                Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
                nativeGetHandleMethod = craftPlayerClass.getMethod("getHandle");

                Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
                nativeConnectionField = serverPlayerClass.getField("connection");

                Class<?> packetClass = Class.forName("net.minecraft.network.protocol.Packet");
                Class<?> connectionClass = Class.forName("net.minecraft.server.network.ServerCommonPacketListenerImpl");
                nativeSendPacketMethod = connectionClass.getMethod("send", packetClass);
            } catch (Throwable t) {
                nativeSyncPacketCtor = null;
            }
            nativeSyncInitialized = true;
            return nativeSyncPacketCtor != null;
        }
    }

    private static boolean sendNativePositionSync(Player player, int entityId, Location loc, float yaw, float pitch) {
        if (!initNativeSync()) return false;
        try {
            Object vec3 = nativeVec3Ctor.newInstance(loc.getX(), loc.getY(), loc.getZ());
            Object posPath = nativePosPathOfMethod.invoke(null, vec3);
            Object packet = nativeSyncPacketCtor.newInstance(entityId, posPath, yaw, pitch, true);

            Object serverPlayer = nativeGetHandleMethod.invoke(player);
            Object connection = nativeConnectionField.get(serverPlayer);
            nativeSendPacketMethod.invoke(connection, packet);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void spawnSeatForPlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        if (!seatSpawnedFor.add(player.getUniqueId())) return;
        try {
            WrapperPlayServerSpawnEntity spawnSeat = new WrapperPlayServerSpawnEntity(
                seatEntityId,
                Optional.of(UUID.randomUUID()),
                EntityTypes.ARMOR_STAND,
                new Vector3d(location.getX(), location.getY() + 0.30, location.getZ()),
                0f,
                location.getYaw(),
                location.getYaw(),
                0,
                Optional.empty()
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnSeat);

            List<EntityData<?>> seatMeta = new ArrayList<>();
            seatMeta.add(new EntityData<>(0, EntityDataTypes.BYTE, (byte) 0x20));
            seatMeta.add(new EntityData<>(15, EntityDataTypes.BYTE, (byte) (0x10 | 0x08 | 0x01)));
            WrapperPlayServerEntityMetadata metaSeatPacket = new WrapperPlayServerEntityMetadata(seatEntityId, seatMeta);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, metaSeatPacket);

            WrapperPlayServerSetPassengers mountPacket =
                new WrapperPlayServerSetPassengers(seatEntityId, new int[]{ entityId });
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, mountPacket);
        } catch (Exception ignored) {}
    }

    private void despawnSeatForPlayer(Player player) {
        if (player == null) return;
        if (!seatSpawnedFor.remove(player.getUniqueId())) return;
        try {
            WrapperPlayServerDestroyEntities destroy =
                new WrapperPlayServerDestroyEntities(seatEntityId);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroy);
        } catch (Exception ignored) {}
    }

    private String getSafeProfileName() {
        String colored = HologramLine.colorize(name);
        String stripped = ChatColor.stripColor(colored);
        if (stripped == null || stripped.trim().isEmpty()) {
            stripped = "NPC_" + (entityId % 10000);
        }
        stripped = stripped.replaceAll("[^a-zA-Z0-9_]", "_");
        if (stripped.length() > 16) {
            stripped = stripped.substring(0, 16);
        }
        return stripped;
    }

    private NamedTextColor resolveTeamColor(String colorName) {
        if (colorName == null) return NamedTextColor.WHITE;
        return switch (colorName.toUpperCase()) {
            case "RED"          -> NamedTextColor.RED;
            case "GOLD"         -> NamedTextColor.GOLD;
            case "YELLOW"       -> NamedTextColor.YELLOW;
            case "GREEN"        -> NamedTextColor.GREEN;
            case "AQUA"         -> NamedTextColor.AQUA;
            case "BLUE"         -> NamedTextColor.BLUE;
            case "LIGHT_PURPLE" -> NamedTextColor.LIGHT_PURPLE;
            default             -> NamedTextColor.WHITE;
        };
    }

    private void sendHideNametagTeamPacket(Player player) {
        try {
            if (!teamCreatedFor.add(player.getUniqueId())) return;
            String teamName = "npc_" + (entityId % 100000);
            String profileName = getSafeProfileName();
            NamedTextColor teamColor = resolveTeamColor(this.glowColor);
            WrapperPlayServerTeams.ScoreBoardTeamInfo teamInfo =
                new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                    Component.empty(),
                    Component.empty(),
                    Component.empty(),
                    WrapperPlayServerTeams.NameTagVisibility.NEVER,
                    WrapperPlayServerTeams.CollisionRule.NEVER,
                    teamColor,
                    WrapperPlayServerTeams.OptionData.NONE
                );
            WrapperPlayServerTeams teamPacket = new WrapperPlayServerTeams(
                teamName,
                WrapperPlayServerTeams.TeamMode.CREATE,
                teamInfo,
                Collections.singletonList(profileName)
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, teamPacket);
        } catch (Exception ignored) {}
    }

    public void updateTeamForAll() {
        String teamName = "npc_" + (entityId % 100000);
        NamedTextColor teamColor = resolveTeamColor(this.glowColor);
        WrapperPlayServerTeams.ScoreBoardTeamInfo teamInfo =
            new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                Component.empty(),
                Component.empty(),
                Component.empty(),
                WrapperPlayServerTeams.NameTagVisibility.NEVER,
                WrapperPlayServerTeams.CollisionRule.NEVER,
                teamColor,
                WrapperPlayServerTeams.OptionData.NONE
            );
        WrapperPlayServerTeams teamPacket = new WrapperPlayServerTeams(
            teamName,
            WrapperPlayServerTeams.TeamMode.UPDATE,
            teamInfo,
            Collections.emptyList()
        );
        for (UUID uid : new HashSet<>(spawnedFor)) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(p, teamPacket);
            }
        }
    }

    public void setGlowColor(String color) {
        this.glowColor = (color != null && !color.equalsIgnoreCase("off")) ? color.toUpperCase() : null;
        this.glowing = this.glowColor != null;
        updateTeamForAll();
        updateMetadataForAll();
    }

    private void sendAddToTabList(Player player, UUID activeProfileUuid) {
        List<TextureProperty> textures = new ArrayList<>();
        if (skinData != null && skinData.getValue() != null && !skinData.getValue().trim().isEmpty()) {
            String sig = skinData.getSignature() != null && !skinData.getSignature().trim().isEmpty()
                ? skinData.getSignature().trim()
                : null;
            textures.add(new TextureProperty("textures", skinData.getValue().trim(), sig));
        }

        String profileName = getSafeProfileName();
        UserProfile profile = new UserProfile(activeProfileUuid, profileName, textures);

        Component displayComponent = LegacyComponentSerializer.legacySection()
            .deserialize(HologramLine.colorize(name));

        EnumSet<WrapperPlayServerPlayerInfoUpdate.Action> actions = EnumSet.of(
            WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
            WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_GAME_MODE,
            WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED,
            WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LATENCY,
            WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_DISPLAY_NAME
        );

        WrapperPlayServerPlayerInfoUpdate infoUpdate = new WrapperPlayServerPlayerInfoUpdate(
            actions,
            new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                profile,
                false,
                0,
                GameMode.SURVIVAL,
                displayComponent,
                null
            )
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, infoUpdate);
    }

    private void sendSpawnPacket(Player player, UUID activeProfileUuid) {
        EntityType entityType = resolveEntityType(npcType);
        UUID spawnUuid = (npcType == NPCType.PLAYER) ? activeProfileUuid : uuid;
        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
            entityId,
            Optional.of(spawnUuid),
            entityType,
            new Vector3d(location.getX(), location.getY(), location.getZ()),
            location.getPitch(),
            location.getYaw(),
            location.getYaw(),
            0,
            Optional.empty()
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnPacket);
    }

    private void sendMetadataPacket(Player player) {
        try {
            List<EntityData<?>> baseMetadata = new ArrayList<>();

            byte flags = 0;
            if (pose == NPCPose.SNEAKING) flags |= 0x02;
            if (pose == NPCPose.SWIMMING) flags |= 0x10;
            if (invisible)                flags |= 0x20;
            if (glowing)                  flags |= 0x40;
            baseMetadata.add(new EntityData<>(0, EntityDataTypes.BYTE, flags));

            byte livingFlags = 0;
            if (pose == NPCPose.SPIN_ATTACK) livingFlags |= 0x04;
            baseMetadata.add(new EntityData<>(8, EntityDataTypes.BYTE, livingFlags));

            EntityPose pePose = switch (pose) {
                case SNEAKING    -> EntityPose.CROUCHING;
                case SLEEPING    -> EntityPose.SLEEPING;
                case SWIMMING    -> EntityPose.SWIMMING;
                case SPIN_ATTACK -> EntityPose.SPIN_ATTACK;
                case DYING       -> EntityPose.DYING;
                case SITTING     -> EntityPose.STANDING;
                default          -> EntityPose.STANDING;
            };
            baseMetadata.add(new EntityData<>(6, EntityDataTypes.ENTITY_POSE, pePose));

            WrapperPlayServerEntityMetadata metaPacket =
                new WrapperPlayServerEntityMetadata(entityId, baseMetadata);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, metaPacket);

            if (npcType == NPCType.PLAYER) {
                ServerVersion ver = PacketEvents.getAPI().getServerManager().getVersion();
                if (ver.isOlderThanOrEquals(ServerVersion.V_1_21_1)) {
                    List<EntityData<?>> skinLayers = Collections.singletonList(
                        new EntityData<>(17, EntityDataTypes.BYTE, (byte) 0x7F)
                    );
                    PacketEvents.getAPI().getPlayerManager().sendPacket(
                        player,
                        new WrapperPlayServerEntityMetadata(entityId, skinLayers)
                    );
                }
            }
        } catch (Exception ignored) {}
    }

    private void sendEquipmentPackets(Player player) {
        if (equipment == null) return;
        try {
            List<Equipment> eqList = new ArrayList<>();
            org.bukkit.inventory.ItemStack mainHand = equipment.getMainHand();
            if ((mainHand == null || mainHand.getType().isAir()) && pose == NPCPose.SPIN_ATTACK) {
                mainHand = new org.bukkit.inventory.ItemStack(org.bukkit.Material.TRIDENT);
            }
            eqList.add(new Equipment(EquipmentSlot.MAIN_HAND,   toPeItem(mainHand)));
            eqList.add(new Equipment(EquipmentSlot.OFF_HAND,    toPeItem(equipment.getOffHand())));
            eqList.add(new Equipment(EquipmentSlot.HELMET,      toPeItem(equipment.getHelmet())));
            eqList.add(new Equipment(EquipmentSlot.CHEST_PLATE, toPeItem(equipment.getChestplate())));
            eqList.add(new Equipment(EquipmentSlot.LEGGINGS,    toPeItem(equipment.getLeggings())));
            eqList.add(new Equipment(EquipmentSlot.BOOTS,       toPeItem(equipment.getBoots())));

            WrapperPlayServerEntityEquipment eqPacket =
                new WrapperPlayServerEntityEquipment(entityId, eqList);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, eqPacket);
        } catch (Exception e) {
            plugin.getLogger().warning("[NpcFant] Error enviando equipamiento: " + e.getMessage());
        }
    }

    private ItemStack toPeItem(org.bukkit.inventory.ItemStack bukkitItem) {
        if (bukkitItem == null || bukkitItem.getType().isAir()) {
            return ItemStack.EMPTY;
        }
        return SpigotConversionUtil.fromBukkitItemStack(bukkitItem);
    }

    private EntityType resolveEntityType(NPCType type) {
        return EntityTypes.PLAYER;
    }

    private void sendRemoveFromTabList(Player player, UUID targetProfileUuid) {
        try {
            WrapperPlayServerPlayerInfoRemove removePacket =
                new WrapperPlayServerPlayerInfoRemove(Collections.singletonList(targetProfileUuid));
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, removePacket);
        } catch (Exception ignored) {}
    }

    public int      getEntityId()  { return entityId; }
    public UUID     getUUID()      { return uuid; }
    public String   getName()      { return name; }
    public void     setName(String n) {
        this.name = HologramLine.colorize(n);
        respawnForAll();
    }
    public Location getLocation()  { return location.clone(); }
    public SkinData getSkinData()  { return skinData; }
    public boolean  isRespawning() { return respawning; }
    public boolean  isSpawnedFor(UUID uid) { return respawning || spawnedFor.contains(uid); }
    public Set<UUID> getSpawnedFor() { return Collections.unmodifiableSet(spawnedFor); }

    public NPCType getNpcType() { return npcType; }
    public void setNpcType(NPCType type) {
        this.npcType = NPCType.PLAYER;
        if (!spawnedFor.isEmpty()) {
            respawnForAll();
        }
    }

    public NPCPose getPose() { return pose; }
    public void setPose(NPCPose newPose) {
        NPCPose prevPose = this.pose;
        this.pose = newPose != null ? newPose : NPCPose.STANDING;
        if (prevPose == NPCPose.SITTING && this.pose != NPCPose.SITTING) {
            for (UUID uid : new HashSet<>(seatSpawnedFor)) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null && p.isOnline()) {
                    despawnSeatForPlayer(p);
                }
            }
            seatSpawnedFor.clear();
        }
        if (!spawnedFor.isEmpty()) {
            respawnForAll();
        } else {
            updateMetadataForAll();
        }
    }

    public NPCEquipment getEquipment() { return equipment; }
    public void setEquipment(NPCEquipment equipment) {
        this.equipment = equipment != null ? equipment : new NPCEquipment();
        updateEquipmentForAll();
    }

    public boolean isInvisible() { return invisible; }
    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
        updateMetadataForAll();
    }

    public boolean isGlowing() { return glowing; }
    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
        updateMetadataForAll();
    }

    public boolean isShowNametag() { return showNametag; }
    public void setShowNametag(boolean showNametag) {
        this.showNametag = showNametag;
        updateMetadataForAll();
    }
}
