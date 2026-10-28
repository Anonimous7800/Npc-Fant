package com.example.advancednpc.npc;

import com.example.advancednpc.api.INPC;
import com.example.advancednpc.api.INPCManager;
import com.example.advancednpc.api.event.NPCInteractEvent;
import com.example.advancednpc.modules.animation.NPCPose;
import com.example.advancednpc.modules.hologram.Hologram;
import com.example.advancednpc.modules.hologram.HologramManager;
import com.example.advancednpc.modules.interaction.CooldownManager;
import com.example.advancednpc.modules.npc.NPCType;
import com.example.advancednpc.modules.skin.NPCStorage;
import com.example.advancednpc.modules.skin.SkinData;
import com.example.advancednpc.modules.skin.SkinManager;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NPCManager implements INPCManager, Listener {

    private static final double RENDER_DISTANCE_SQ   = 60.0 * 60.0;
    private static final double DESPAWN_DISTANCE_SQ  = 68.0 * 68.0;
    private static final double LOOK_AT_DISTANCE_SQ  = 12.0 * 12.0;

    private final JavaPlugin      plugin;
    private final HologramManager hologramManager;
    private final CooldownManager cooldownManager;
    private       SkinManager     skinManager;
    private       NPCStorage      storage;
    private       com.example.advancednpc.modules.gui.NPCEditorGUI editorGUI;
    private       BukkitTask      trackerTask;
    private       PacketListenerAbstract packetListener;
    private       int             tickCounter = 0;

    private final Map<UUID, NPCEntity> npcs = new LinkedHashMap<>();
    private final Map<Integer, NPCEntity> entityIdMap = new ConcurrentHashMap<>();

    public NPCManager(JavaPlugin plugin, HologramManager hologramManager,
                      CooldownManager cooldownManager) {
        this.plugin          = plugin;
        this.hologramManager = hologramManager;
        this.cooldownManager = cooldownManager;
        registerPacketListener();
        startTrackerTask();
    }

    public void setSkinManager(SkinManager sm) { this.skinManager = sm; }
    public void setStorage(NPCStorage s)       { this.storage = s; }
    public void setEditorGUI(com.example.advancednpc.modules.gui.NPCEditorGUI eg) { this.editorGUI = eg; }
    public com.example.advancednpc.modules.gui.NPCEditorGUI getEditorGUI() { return editorGUI; }

    private void registerPacketListener() {
        this.packetListener = new PacketListenerAbstract(PacketListenerPriority.NORMAL) {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

                Object playerObj = event.getPlayer();
                if (!(playerObj instanceof Player player)) return;

                WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
                int targetEntityId = wrapper.getEntityId();

                NPCEntity npc = entityIdMap.get(targetEntityId);
                if (npc == null) return;

                WrapperPlayClientInteractEntity.InteractAction action = wrapper.getAction();
                if (action == WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT) {
                    return;
                }
                if (action == WrapperPlayClientInteractEntity.InteractAction.INTERACT
                        && wrapper.getHand() == InteractionHand.OFF_HAND) {
                    return;
                }

                NPCInteractEvent.InteractType interactType =
                    (action == WrapperPlayClientInteractEntity.InteractAction.ATTACK)
                        ? NPCInteractEvent.InteractType.LEFT_CLICK
                        : NPCInteractEvent.InteractType.RIGHT_CLICK;

                Bukkit.getScheduler().runTask(plugin, () -> handleInteraction(player, npc, interactType));
            }
        };
        PacketEvents.getAPI().getEventManager().registerListener(this.packetListener);
    }

    private void handleInteraction(Player player, NPCEntity npc, NPCInteractEvent.InteractType type) {
        if (!player.isOnline()) return;

        if (player.isSneaking() && type == NPCInteractEvent.InteractType.LEFT_CLICK
                && (player.isOp() || player.hasPermission("npcfant.admin"))) {
            if (editorGUI != null) {
                editorGUI.openEditor(player, npc);
                return;
            }
        }

        if (!cooldownManager.canInteract(player.getUniqueId(), npc.getId())) {
            return;
        }
        cooldownManager.registerInteraction(player.getUniqueId(), npc.getId());

        NPCInteractEvent interactEvent = new NPCInteractEvent(player, npc, type);
        Bukkit.getPluginManager().callEvent(interactEvent);
        if (interactEvent.isCancelled()) return;

        npc.getActionChain().execute(player, npc.getId());
    }

    private void startTrackerTask() {
        this.trackerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
            if (onlinePlayers.isEmpty() || npcs.isEmpty()) return;

            tickCounter++;
            boolean refreshHologramPlaceholders = (tickCounter % 10 == 0);

            for (NPCEntity npc : npcs.values()) {
                if (!npc.isSpawned()) continue;
                Location npcLoc = npc.getLocation();
                if (npcLoc.getWorld() == null) continue;

                Hologram holo = npc.getHologramId() != null
                    ? hologramManager.getHologram(npc.getHologramId()).orElse(null)
                    : null;

                for (Player player : onlinePlayers) {
                    UUID pid = player.getUniqueId();
                    boolean isSpawned = npc.getFakePlayer().isSpawnedFor(pid);

                    if (!player.getWorld().equals(npcLoc.getWorld()) || !npc.canSee(player)) {
                        if (isSpawned) {
                            npc.getFakePlayer().despawnForPlayer(player);
                        }
                        if (holo != null && holo.isSpawnedFor(pid)) {
                            holo.despawnForPlayer(player);
                        }
                        continue;
                    }

                    double distSq = player.getLocation().distanceSquared(npcLoc);
                    if (!isSpawned && distSq <= RENDER_DISTANCE_SQ) {
                        npc.getFakePlayer().spawnForPlayer(player);
                        if (holo != null) holo.spawnForPlayer(player);
                    } else if (isSpawned && distSq > DESPAWN_DISTANCE_SQ) {
                        npc.getFakePlayer().despawnForPlayer(player);
                        if (holo != null) holo.despawnForPlayer(player);
                    } else if (isSpawned) {
                        if (holo != null && !holo.isSpawnedFor(pid)) {
                            holo.spawnForPlayer(player);
                        } else if (holo != null && refreshHologramPlaceholders) {
                            holo.updateTextsForPlayer(player);
                        }
                        if (npc.isLookAtPlayer() && distSq <= LOOK_AT_DISTANCE_SQ) {
                            npc.lookAtPlayer(player);
                        }
                    }
                }
            }
        }, 10L, 4L);
    }

    public Location getHologramBaseLocation(NPCEntity entity) {
        NPCPose pose = entity.getPose();
        double yOffset = switch (pose) {
            case SNEAKING           -> 1.75;
            case SITTING            -> 1.50;
            case SLEEPING, SWIMMING -> 0.70;
            default                 -> 2.05;
        };
        return entity.getLocation().clone().add(0, yOffset, 0);
    }

    public void syncHologram(NPCEntity entity) {
        if (entity == null) return;
        List<String> combinedLines = new ArrayList<>();
        if (entity.isShowNametag() && entity.getName() != null && !entity.getName().isEmpty()) {
            combinedLines.add(entity.getName());
        }
        if (entity.getHologramLines() != null && !entity.getHologramLines().isEmpty()) {
            combinedLines.addAll(entity.getHologramLines());
        }

        if (combinedLines.isEmpty()) {
            if (entity.getHologramId() != null) {
                hologramManager.removeHologram(entity.getHologramId());
                entity.setHologramId(null);
            }
            return;
        }

        Location holLoc = getHologramBaseLocation(entity);
        if (entity.getHologramId() != null && hologramManager.getHologram(entity.getHologramId()).isPresent()) {
            Hologram h = hologramManager.getHologram(entity.getHologramId()).get();
            h.move(holLoc);
            h.setAllLines(combinedLines);
        } else {
            Hologram h = hologramManager.createHologram(holLoc, combinedLines.toArray(new String[0]));
            entity.setHologramId(h.getId());
        }
    }

    @Override
    public INPC createNPC(String name, Location location) {
        return createNPC(name, location, NPCType.PLAYER);
    }

    public NPCEntity createNPC(String name, Location location, NPCType type) {
        UUID       id     = UUID.randomUUID();
        SkinData   skin   = skinManager != null ? skinManager.getDefaultSkin() : null;
        NPCEntity  entity = new NPCEntity(id, name, location, plugin, skin, type);

        npcs.put(id, entity);
        entityIdMap.put(entity.getFakePlayer().getEntityId(), entity);
        syncHologram(entity);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            entity.getFakePlayer().spawnForAll();
            if (entity.getHologramId() != null) {
                hologramManager.getHologram(entity.getHologramId()).ifPresent(Hologram::refresh);
            }
            if (type == NPCType.PLAYER && !SkinManager.isSigned(skin)) {
                applySkinAsync(id, "preset", "steve", null);
            }
        }, 2L);

        if (storage != null) storage.save(entity);

        plugin.getLogger().info("[NpcFant] NPC '" + ChatColor.stripColor(entity.getName())
            + "' (" + entity.getType() + ") creado (ID: " + id + ")");
        return entity;
    }

    @Override
    public void removeNPC(UUID id) {
        NPCEntity entity = npcs.remove(id);
        if (entity == null) return;

        entityIdMap.remove(entity.getFakePlayer().getEntityId());

        if (entity.getHologramId() != null) {
            hologramManager.removeHologram(entity.getHologramId());
        }
        entity.remove();

        if (storage != null) storage.delete(id);
    }

    @Override
    public INPC getNPC(UUID id) { return npcs.get(id); }

    @Override
    public Collection<INPC> getAllNPCs() {
        return Collections.unmodifiableCollection(npcs.values());
    }

    @Override
    public void shutdown() {
        if (trackerTask != null) {
            trackerTask.cancel();
            trackerTask = null;
        }
        if (packetListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
        }
        if (storage != null) storage.saveAll(npcs.values());
        List<UUID> ids = new ArrayList<>(npcs.keySet());
        for (UUID id : ids) {
            NPCEntity e = npcs.get(id);
            if (e != null) {
                entityIdMap.remove(e.getFakePlayer().getEntityId());
                e.remove();
            }
        }
        npcs.clear();
        entityIdMap.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            for (NPCEntity npc : npcs.values()) {
                if (npc.canSee(player)
                        && npc.getLocation().getWorld() != null
                        && npc.getLocation().getWorld().equals(player.getWorld())
                        && npc.getLocation().distanceSquared(player.getLocation()) <= RENDER_DISTANCE_SQ) {
                    npc.getFakePlayer().spawnForPlayer(player);
                    if (npc.getHologramId() != null) {
                        hologramManager.getHologram(npc.getHologramId())
                            .ifPresent(h -> h.spawnForPlayer(player));
                    }
                }
            }
        }, 15L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        UUID uid = e.getPlayer().getUniqueId();
        for (NPCEntity npc : npcs.values()) {
            npc.getFakePlayer().removePlayerFromTracking(uid);
            if (npc.getHologramId() != null) {
                hologramManager.getHologram(npc.getHologramId())
                    .ifPresent(h -> h.removePlayerFromTracking(uid));
            }
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        UUID uid = e.getPlayer().getUniqueId();
        for (NPCEntity npc : npcs.values()) {
            npc.getFakePlayer().removePlayerFromTracking(uid);
            if (npc.getHologramId() != null) {
                hologramManager.getHologram(npc.getHologramId())
                    .ifPresent(h -> h.removePlayerFromTracking(uid));
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        UUID uid = e.getPlayer().getUniqueId();
        for (NPCEntity npc : npcs.values()) {
            npc.getFakePlayer().removePlayerFromTracking(uid);
            if (npc.getHologramId() != null) {
                hologramManager.getHologram(npc.getHologramId())
                    .ifPresent(h -> h.removePlayerFromTracking(uid));
            }
        }
    }

    public Optional<NPCEntity> getByEntityId(int entityId) {
        return Optional.ofNullable(entityIdMap.get(entityId));
    }

    public Optional<NPCEntity> resolveNPC(String identifier, Player player) {
        if (identifier == null || identifier.isEmpty()) return Optional.empty();
        if (identifier.equalsIgnoreCase("nearest") || identifier.equalsIgnoreCase("cercano")) {
            return player != null ? getNearestNPC(player.getLocation(), 15.0) : Optional.empty();
        }
        try {
            UUID id = UUID.fromString(identifier);
            NPCEntity byId = npcs.get(id);
            if (byId != null) return Optional.of(byId);
        } catch (IllegalArgumentException ignored) {}

        for (NPCEntity npc : npcs.values()) {
            String cleanName = ChatColor.stripColor(npc.getName());
            if (npc.getName().equalsIgnoreCase(identifier)
                    || (cleanName != null && cleanName.equalsIgnoreCase(identifier))
                    || npc.getId().toString().startsWith(identifier.toLowerCase())) {
                return Optional.of(npc);
            }
        }
        return Optional.empty();
    }

    public Optional<NPCEntity> getNearestNPC(Location loc, double maxDistance) {
        if (loc == null || loc.getWorld() == null) return Optional.empty();
        return npcs.values().stream()
            .filter(n -> n.getLocation().getWorld() != null
                      && n.getLocation().getWorld().equals(loc.getWorld()))
            .min(Comparator.comparingDouble(n ->
                n.getLocation().distanceSquared(loc)))
            .filter(n -> n.getLocation().distance(loc) <= maxDistance);
    }

    public INPC cloneNPC(UUID originalId, Location newLocation) {
        NPCEntity original = npcs.get(originalId);
        if (original == null) return null;

        NPCEntity cloned = createNPC(original.getName(), newLocation, original.getType());
        cloned.setPose(original.getPose());
        cloned.setGlowColor(original.getGlowColor());
        cloned.setInvisible(original.isInvisible());
        cloned.setLookAtPlayer(original.isLookAtPlayer());

        if (original.getFakePlayer().getSkinData() != null) {
            SkinData sd = original.getFakePlayer().getSkinData();
            cloned.getFakePlayer().updateSkin(sd);
            cloned.setSkin(sd.getValue(), sd.getSignature());
        }
        if (original.getHologramLines() != null && !original.getHologramLines().isEmpty()) {
            cloned.setHologramLines(new ArrayList<>(original.getHologramLines()));
        }
        syncHologram(cloned);
        if (storage != null) storage.save(cloned);
        return cloned;
    }

    public void moveNPC(UUID id, Location newLocation) {
        NPCEntity entity = npcs.get(id);
        if (entity == null) return;
        entity.teleport(newLocation);
        syncHologram(entity);
        if (storage != null) storage.save(entity);
    }

    public void applySkinAsync(UUID npcId, String type, String input,
                               Player requester) {
        NPCEntity entity = npcs.get(npcId);
        if (entity == null) {
            if (requester != null)
                requester.sendMessage("§c[NpcFant] NPC no encontrado.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Optional<SkinData> sd = skinManager != null
                ? skinManager.getSkin(type, input)
                : Optional.empty();

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (sd.isPresent() && SkinManager.isSigned(sd.get())) {
                    entity.getFakePlayer().updateSkin(sd.get());
                    entity.setSkin(sd.get().getValue(), sd.get().getSignature());
                    if (storage != null) storage.save(entity);
                    if (requester != null)
                        requester.sendMessage("§a[NpcFant] Skin '" + input + "' aplicada correctamente.");
                } else {
                    if (requester != null) {
                        if ("file".equalsIgnoreCase(type) && skinManager != null && !skinManager.hasLocalPng(input)) {
                            requester.sendMessage("§c[NpcFant] No se encontró el archivo '" + input + "' en plugins/NpcFant/skins/custom/.");
                        } else {
                            requester.sendMessage("§c[NpcFant] No se pudo cargar/firmar la skin '" + input + "'. Prueba con un nombre de jugador premium o espera unos segundos si es un .png personalizado.");
                        }
                    }
                }
            });
        });
    }

    public Hologram attachHologram(UUID npcId, String... lines) {
        NPCEntity entity = npcs.get(npcId);
        if (entity == null) return null;

        List<String> extraLines = new ArrayList<>(entity.getHologramLines());
        Collections.addAll(extraLines, lines);
        entity.setHologramLines(extraLines);
        syncHologram(entity);

        if (storage != null) storage.save(entity);
        return entity.getHologramId() != null
            ? hologramManager.getHologram(entity.getHologramId()).orElse(null)
            : null;
    }

    public void saveNPC(NPCEntity entity) {
        if (entity != null) {
            syncHologram(entity);
            if (storage != null) {
                storage.save(entity);
            }
        }
    }

    public void loadAll() {
        if (storage == null) return;
        List<NPCEntity> loaded = storage.loadAll(plugin);
        for (NPCEntity entity : loaded) {
            npcs.put(entity.getId(), entity);
            entityIdMap.put(entity.getFakePlayer().getEntityId(), entity);
            syncHologram(entity);
        }
        plugin.getLogger().info("[NpcFant] " + loaded.size() + " NPC(s) cargados desde el almacenamiento.");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (NPCEntity entity : npcs.values()) {
                entity.getFakePlayer().spawnForAll();
                if (entity.getHologramId() != null) {
                    hologramManager.getHologram(entity.getHologramId()).ifPresent(Hologram::refresh);
                }
                if (entity.getType() == NPCType.PLAYER && !SkinManager.isSigned(entity.getFakePlayer().getSkinData())) {
                    applySkinAsync(entity.getId(), "preset", "steve", null);
                }
            }
        }, 20L);
    }

    public int      count()           { return npcs.size(); }
    public boolean  isNPC(UUID id)    { return npcs.containsKey(id); }
    public SkinManager getSkinManager() { return skinManager; }
    public Map<UUID, NPCEntity> getNpcsMap() { return Collections.unmodifiableMap(npcs); }
}
