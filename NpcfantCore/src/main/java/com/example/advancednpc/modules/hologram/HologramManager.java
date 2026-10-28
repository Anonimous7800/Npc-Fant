package com.example.advancednpc.modules.hologram;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {

    private final Plugin plugin;
    private final Map<UUID, Hologram> holograms = new LinkedHashMap<>();
    private final Map<String, Hologram> namedHolograms = new ConcurrentHashMap<>();
    private final Map<UUID, String> idToName = new ConcurrentHashMap<>();
    private final File dataFile;
    private BukkitTask trackerTask;

    public HologramManager(Plugin plugin) {
        this.plugin = plugin;
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) dataDir.mkdirs();
        this.dataFile = new File(dataDir, "holograms.yml");

        startTracker();
        Bukkit.getScheduler().runTaskLater(plugin, this::loadHolograms, 20L);
    }

    private void startTracker() {
        this.trackerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            if (players.isEmpty() || holograms.isEmpty()) return;

            for (Hologram h : holograms.values()) {
                if (!h.isAlive()) continue;
                Location hLoc = h.getLocation();
                if (hLoc.getWorld() == null) continue;

                for (Player p : players) {
                    if (!p.getWorld().equals(hLoc.getWorld())) {
                        if (h.isSpawnedFor(p.getUniqueId())) {
                            h.despawnForPlayer(p);
                        }
                        continue;
                    }

                    double distSq = p.getLocation().distanceSquared(hLoc);
                    boolean isSpawned = h.isSpawnedFor(p.getUniqueId());

                    if (!isSpawned && distSq <= 48.0 * 48.0) {
                        h.spawnForPlayer(p);
                    } else if (isSpawned && distSq > 56.0 * 56.0) {
                        h.despawnForPlayer(p);
                    }
                }
            }
        }, 20L, 10L);
    }

    public Hologram createHologram(Location location, String... lines) {
        UUID id = UUID.randomUUID();
        Hologram hologram = new Hologram(id, plugin, location);
        for (String line : lines) {
            hologram.addLine(line);
        }
        holograms.put(id, hologram);
        return hologram;
    }

    public Hologram createHologram(UUID id, Location location, List<String> lines) {
        Hologram hologram = new Hologram(id, plugin, location);
        for (String line : lines) {
            hologram.addLine(line);
        }
        holograms.put(id, hologram);
        return hologram;
    }

    public Hologram createNamedHologram(String name, Location location, List<String> lines) {
        String key = name.toLowerCase();
        if (namedHolograms.containsKey(key)) {
            removeNamedHologram(key);
        }
        UUID id = UUID.randomUUID();
        Hologram h = new Hologram(id, plugin, location);
        if (lines != null) {
            for (String line : lines) {
                h.addLine(line);
            }
        }
        holograms.put(id, h);
        namedHolograms.put(key, h);
        idToName.put(id, key);
        saveHolograms();
        return h;
    }

    public Optional<Hologram> getNamedHologram(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(namedHolograms.get(name.toLowerCase()));
    }

    public boolean removeNamedHologram(String name) {
        if (name == null) return false;
        String key = name.toLowerCase();
        Hologram h = namedHolograms.remove(key);
        if (h != null) {
            idToName.remove(h.getId());
            holograms.remove(h.getId());
            h.delete();
            saveHolograms();
            return true;
        }
        return false;
    }

    public Map<String, Hologram> getNamedHolograms() {
        return Collections.unmodifiableMap(namedHolograms);
    }

    public Optional<Hologram> getHologram(UUID id) {
        return Optional.ofNullable(holograms.get(id));
    }

    public Collection<Hologram> getAllHolograms() {
        return Collections.unmodifiableCollection(holograms.values());
    }

    public void removeHologram(UUID id) {
        String name = idToName.remove(id);
        if (name != null) namedHolograms.remove(name);
        Hologram h = holograms.remove(id);
        if (h != null) h.delete();
    }

    public void saveHolograms() {
        try {
            YamlConfiguration cfg = new YamlConfiguration();
            for (Map.Entry<String, Hologram> entry : namedHolograms.entrySet()) {
                String key = entry.getKey();
                Hologram h = entry.getValue();
                Location loc = h.getLocation();
                String path = "holograms." + key;

                cfg.set(path + ".world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
                cfg.set(path + ".x", loc.getX());
                cfg.set(path + ".y", loc.getY());
                cfg.set(path + ".z", loc.getZ());
                cfg.set(path + ".lines", h.getLines());
            }
            cfg.save(dataFile);
        } catch (IOException ignored) {}
    }

    public void loadHolograms() {
        if (!dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection sec = cfg.getConfigurationSection("holograms");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            String path = "holograms." + key;
            String worldName = cfg.getString(path + ".world", "world");
            double x = cfg.getDouble(path + ".x", 0);
            double y = cfg.getDouble(path + ".y", 64);
            double z = cfg.getDouble(path + ".z", 0);
            List<String> lines = cfg.getStringList(path + ".lines");

            World w = Bukkit.getWorld(worldName);
            if (w == null && !Bukkit.getWorlds().isEmpty()) {
                w = Bukkit.getWorlds().get(0);
            }
            if (w == null) continue;

            Location loc = new Location(w, x, y, z);
            UUID id = UUID.randomUUID();
            Hologram h = new Hologram(id, plugin, loc);
            for (String l : lines) {
                h.addLine(l);
            }
            holograms.put(id, h);
            namedHolograms.put(key.toLowerCase(), h);
            idToName.put(id, key.toLowerCase());
        }
    }

    public void shutdown() {
        if (trackerTask != null) {
            trackerTask.cancel();
            trackerTask = null;
        }
        saveHolograms();
        holograms.values().forEach(Hologram::delete);
        holograms.clear();
        namedHolograms.clear();
        idToName.clear();
    }
}
