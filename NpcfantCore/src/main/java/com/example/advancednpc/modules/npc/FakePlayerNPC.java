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
    private       boolean      showNametag  = true;
    private volatile boolean   respawning   = false;
    private final Plugin       plugin;

    private final Set<UUID> spawnedFor     = ConcurrentHashMap.newKeySet();
    private final Set<UUID> teamCreatedFor = ConcurrentHashMap.newKeySet();

    public FakePlayerNPC(UUID uuid, String name, Location location, Plugin plugin) {
        this.uuid        = uuid;
        this.profileUuid = uuid;
        this.name        = HologramLine.colorize(name);
        this.location    = location.clone();
        this.plugin      = plugin;
        this.entityId    = NEXT_ENTITY_ID.getAndIncrement();
        this.skinData    = new SkinData(STEVE_VALUE, STEVE_SIGNATURE);
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
            }

        } catch (Exception e) {
            spawnedFor.remove(player.getUniqueId());
            plugin.getLogger().warning("[NpcFant] Error en spawnForPlayer: " + e.getMessage());
        }
    }

    public void despawnForAll() {
        UUID oldProfileUuid = this.profileUuid;
        for (UUID uid : new HashSet<>(spawnedFor)) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) {
                despawnForPlayerInternal(p, oldProfileUuid);
            }
        }
        spawnedFor.clear();
    }

    public void despawnForPlayer(Player player) {
        despawnForPlayerInternal(player, this.profileUuid);
    }

    private void despawnForPlayerInternal(Player player, UUID targetProfileUuid) {
        if (player == null) return;
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
            if (p == null || !p.isOnline()) continue;
            if (!p.getWorld().equals(newLocation.getWorld())) {
                despawnForPlayer(p);
                continue;
            }
            try {
                WrapperPlayServerEntityTeleport teleportPacket = new WrapperPlayServerEntityTeleport(
                    entityId,
                    new Vector3d(newLocation.getX(), newLocation.getY(), newLocation.getZ()),
                    newLocation.getYaw(),
                    newLocation.getPitch(),
                    true
                );
                PacketEvents.getAPI().getPlayerManager().sendPacket(p, teleportPacket);
                sendRotationPackets(p, newLocation.getYaw(), newLocation.getPitch());
            } catch (Exception e) {
                plugin.getLogger().warning("[NpcFant] Error al teletransportar NPC: " + e.getMessage());
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
        float    pitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));

        sendRotationPackets(target, yaw, pitch);
    }

    private void sendRotationPackets(Player player, float yaw, float pitch) {
        try {
            WrapperPlayServerEntityHeadLook headLookPacket =
                new WrapperPlayServerEntityHeadLook(entityId, yaw);
            WrapperPlayServerEntityRotation rotPacket =
                new WrapperPlayServerEntityRotation(entityId, yaw, pitch, true);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, headLookPacket);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, rotPacket);
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

    private void sendHideNametagTeamPacket(Player player) {
        try {
            if (!teamCreatedFor.add(player.getUniqueId())) return;
            String teamName = "npc_" + (entityId % 100000);
            String profileName = getSafeProfileName();
            WrapperPlayServerTeams.ScoreBoardTeamInfo teamInfo =
                new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                    Component.empty(),
                    Component.empty(),
                    Component.empty(),
                    WrapperPlayServerTeams.NameTagVisibility.NEVER,
                    WrapperPlayServerTeams.CollisionRule.NEVER,
                    NamedTextColor.WHITE,
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

    private boolean hasValidSignedTexture() {
        return skinData != null
            && skinData.getValue() != null
            && !skinData.getValue().trim().isEmpty()
            && skinData.getSignature() != null
            && !skinData.getSignature().trim().isEmpty();
    }

    private void sendAddToTabList(Player player, UUID activeProfileUuid) {
        List<TextureProperty> textures = new ArrayList<>();
        if (hasValidSignedTexture()) {
            textures.add(new TextureProperty("textures", skinData.getValue().trim(), skinData.getSignature().trim()));
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
            if (invisible)                flags |= 0x20;
            if (glowing)                  flags |= 0x40;
            baseMetadata.add(new EntityData<>(0, EntityDataTypes.BYTE, flags));

            EntityPose pePose = switch (pose) {
                case SNEAKING    -> EntityPose.CROUCHING;
                case SLEEPING    -> EntityPose.SLEEPING;
                case SWIMMING    -> EntityPose.SWIMMING;
                case SPIN_ATTACK -> EntityPose.SPIN_ATTACK;
                case DYING       -> EntityPose.DYING;
                case SITTING     -> EntityPose.SITTING;
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
            eqList.add(new Equipment(EquipmentSlot.MAIN_HAND,   toPeItem(equipment.getMainHand())));
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
        if (type == null || type == NPCType.PLAYER) return EntityTypes.PLAYER;
        return switch (type) {
            case VILLAGER         -> EntityTypes.VILLAGER;
            case WANDERING_TRADER -> EntityTypes.WANDERING_TRADER;
            case COW              -> EntityTypes.COW;
            case PIG              -> EntityTypes.PIG;
            case SHEEP            -> EntityTypes.SHEEP;
            case CHICKEN          -> EntityTypes.CHICKEN;
            case HORSE            -> EntityTypes.HORSE;
            case DONKEY           -> EntityTypes.DONKEY;
            case MULE             -> EntityTypes.MULE;
            case LLAMA            -> EntityTypes.LLAMA;
            case CAT              -> EntityTypes.CAT;
            case DOG, WOLF        -> EntityTypes.WOLF;
            case RABBIT           -> EntityTypes.RABBIT;
            case TURTLE           -> EntityTypes.TURTLE;
            case AXOLOTL          -> EntityTypes.AXOLOTL;
            case FROG             -> EntityTypes.FROG;
            case SNIFFER          -> EntityTypes.SNIFFER;
            case ALLAY            -> EntityTypes.ALLAY;
            case POLAR_BEAR       -> EntityTypes.POLAR_BEAR;
            case BEE              -> EntityTypes.BEE;
            case GOAT             -> EntityTypes.GOAT;
            case PANDA            -> EntityTypes.PANDA;
            case FOX              -> EntityTypes.FOX;
            case IRON_GOLEM       -> EntityTypes.IRON_GOLEM;
            case SNOWMAN          -> EntityTypes.SNOW_GOLEM;
            case ZOMBIE           -> EntityTypes.ZOMBIE;
            case SKELETON         -> EntityTypes.SKELETON;
            case CREEPER          -> EntityTypes.CREEPER;
            case SPIDER           -> EntityTypes.SPIDER;
            case ENDERMAN         -> EntityTypes.ENDERMAN;
            case WITCH            -> EntityTypes.WITCH;
            case PILLAGER         -> EntityTypes.PILLAGER;
            case VINDICATOR       -> EntityTypes.VINDICATOR;
            case EVOKER           -> EntityTypes.EVOKER;
            case PHANTOM          -> EntityTypes.PHANTOM;
            case DROWNED          -> EntityTypes.DROWNED;
            case HUSK             -> EntityTypes.HUSK;
            case STRAY            -> EntityTypes.STRAY;
            case WARDEN           -> EntityTypes.WARDEN;
            case BREEZE           -> EntityTypes.BREEZE;
            case ENDER_DRAGON     -> EntityTypes.ENDER_DRAGON;
            case WITHER           -> EntityTypes.WITHER;
            case ELDER_GUARDIAN   -> EntityTypes.ELDER_GUARDIAN;
            case BLAZE            -> EntityTypes.BLAZE;
            case GHAST            -> EntityTypes.GHAST;
            case MAGMA_CUBE       -> EntityTypes.MAGMA_CUBE;
            case PIGLIN           -> EntityTypes.PIGLIN;
            case PIGLIN_BRUTE     -> EntityTypes.PIGLIN_BRUTE;
            case ZOMBIFIED_PIGLIN -> EntityTypes.ZOMBIFIED_PIGLIN;
            case STRIDER          -> EntityTypes.STRIDER;
            case HOGLIN           -> EntityTypes.HOGLIN;
            case ZOGLIN           -> EntityTypes.ZOGLIN;
            case SQUID            -> EntityTypes.SQUID;
            case GLOW_SQUID       -> EntityTypes.GLOW_SQUID;
            case FISH_COD         -> EntityTypes.COD;
            case FISH_SALMON      -> EntityTypes.SALMON;
            case DOLPHIN          -> EntityTypes.DOLPHIN;
            case GUARDIAN         -> EntityTypes.GUARDIAN;
            default               -> EntityTypes.PLAYER;
        };
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
        this.npcType = type != null ? type : NPCType.PLAYER;
        if (!spawnedFor.isEmpty()) {
            respawnForAll();
        }
    }

    public NPCPose getPose() { return pose; }
    public void setPose(NPCPose pose) {
        this.pose = pose != null ? pose : NPCPose.STANDING;
        updateMetadataForAll();
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
