package com.example.advancednpc.npc;

import com.example.advancednpc.modules.skin.NPCStorage;
import com.example.advancednpc.modules.skin.SkinData;
import com.example.advancednpc.modules.skin.SkinManager;
import com.example.advancednpc.api.INPC;
import com.example.advancednpc.api.INPCManager;
import com.example.advancednpc.modules.hologram.Hologram;
import com.example.advancednpc.modules.hologram.HologramManager;
import com.example.advancednpc.modules.interaction.CooldownManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class NPCManager implements INPCManager, Listener {

    private final JavaPlugin      plugin;
    private final HologramManager hologramManager;
    private final CooldownManager cooldownManager;
    private       SkinManager     skinManager;
    private       NPCStorage      storage;


    private final Map<UUID, NPCEntity> npcs = new LinkedHashMap<>();

    private final Map<Integer, NPCEntity> entityIdMap = new HashMap<>();

    public NPCManager(JavaPlugin plugin, HologramManager hologramManager,
                      CooldownManager cooldownManager) {
        this.plugin          = plugin;
        this.hologramManager = hologramManager;
        this.cooldownManager = cooldownManager;
    }


    public void setSkinManager(SkinManager sm) { this.skinManager = sm; }

    public void setStorage(NPCStorage s)       { this.storage = s; }





    @Override
    public INPC createNPC(String name, Location location) {
        UUID       id     = UUID.randomUUID();
        SkinData   skin   = skinManager != null ? skinManager.getDefaultSkin() : null;
        NPCEntity  entity = new NPCEntity(id, name, location, plugin, skin);

        npcs.put(id, entity);
        entityIdMap.put(entity.getFakePlayer().getEntityId(), entity);


        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            entity.getFakePlayer().spawnForAll();
        }, 2L);

        if (storage != null) storage.save(entity);

        plugin.getLogger().info("[NpcFant] NPC '" + name + "' creado (ID: " + id + ")");
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
            for (NPCEntity npc : npcs.values()) {
                npc.getFakePlayer().spawnForPlayer(player);
            }
        }, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {

        UUID uid = e.getPlayer().getUniqueId();
        for (NPCEntity npc : npcs.values()) {
            npc.getFakePlayer().getSpawnedFor();
        }
    }






    public Optional<NPCEntity> getByEntityId(int entityId) {
        return Optional.ofNullable(entityIdMap.get(entityId));
    }


    public Optional<NPCEntity> getNearestNPC(Location loc, double maxDistance) {
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

        INPC cloned = createNPC(original.getName(), newLocation);

        if (original.getFakePlayer().getSkinData() != null) {
            ((NPCEntity) cloned).getFakePlayer()
                .updateSkin(original.getFakePlayer().getSkinData());
        }
        return cloned;
    }


    public void moveNPC(UUID id, Location newLocation) {
        NPCEntity entity = npcs.get(id);
        if (entity == null) return;
        entity.teleport(newLocation);

        if (entity.getHologramId() != null) {
            hologramManager.getHologram(entity.getHologramId())
                .ifPresent(h -> h.move(newLocation.clone().add(0, 2.2, 0)));
        }
    }


    public void applySkinAsync(UUID npcId, String type, String input,
                               org.bukkit.entity.Player requester) {
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
                if (sd.isPresent()) {
                    entity.getFakePlayer().updateSkin(sd.get());
                    entity.setSkin(sd.get().getValue(), sd.get().getSignature());
                    if (storage != null) storage.save(entity);
                    if (requester != null)
                        requester.sendMessage("§a[NpcFant] Skin aplicada correctamente.");
                } else {
                    if (requester != null)
                        requester.sendMessage("§c[NpcFant] No se encontró la skin '" + input + "'.");
                }
            });
        });
    }


    public Hologram attachHologram(UUID npcId, String... lines) {
        NPCEntity entity = npcs.get(npcId);
        if (entity == null) return null;

        Location holLoc = entity.getLocation().clone().add(0, 2.2, 0);

        Hologram h = hologramManager.createHologram(holLoc, lines);
        entity.setHologramId(h.getId());

        if (storage != null) storage.save(entity);
        return h;
    }


    public void loadAll() {
        if (storage == null) return;
        List<NPCEntity> loaded = storage.loadAll(plugin);
        for (NPCEntity entity : loaded) {
            npcs.put(entity.getId(), entity);
            entityIdMap.put(entity.getFakePlayer().getEntityId(), entity);
        }
        plugin.getLogger().info("[NpcFant] " + loaded.size() + " NPC(s) cargados desde el almacenamiento.");


        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (NPCEntity entity : npcs.values()) {
                entity.getFakePlayer().spawnForAll();

                if (entity.getHologramLines() != null && !entity.getHologramLines().isEmpty()
                        && entity.getHologramId() == null) {
                    Hologram h = hologramManager.createHologram(
                        entity.getLocation().clone().add(0, 2.2, 0),
                        entity.getHologramLines().toArray(new String[0]));
                    entity.setHologramId(h.getId());
                }
            }
        }, 40L);
    }

    public int      count()           { return npcs.size(); }
    public boolean  isNPC(UUID id)    { return npcs.containsKey(id); }
    public SkinManager getSkinManager() { return skinManager; }
    public Map<UUID, NPCEntity> getNpcsMap() { return Collections.unmodifiableMap(npcs); }
}
