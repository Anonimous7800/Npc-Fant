package com.example.advancednpc.modules.hologram;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class HologramManager {

    private final Plugin plugin;
    private final Map<UUID, Hologram> holograms = new LinkedHashMap<>();

    public HologramManager(Plugin plugin) {
        this.plugin = plugin;
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


    public Optional<Hologram> getHologram(UUID id) {
        return Optional.ofNullable(holograms.get(id));
    }


    public Collection<Hologram> getAllHolograms() {
        return Collections.unmodifiableCollection(holograms.values());
    }


    public void removeHologram(UUID id) {
        Hologram h = holograms.remove(id);
        if (h != null) h.delete();
    }


    public void shutdown() {
        holograms.values().forEach(Hologram::delete);
        holograms.clear();
    }
}
