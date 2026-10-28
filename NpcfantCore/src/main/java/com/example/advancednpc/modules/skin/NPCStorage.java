package com.example.advancednpc.modules.skin;

import com.example.advancednpc.npc.NPCEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class NPCStorage {

    private final Plugin plugin;
    private final File   dataFile;

    public NPCStorage(Plugin plugin) {
        this.plugin   = plugin;
        File dataDir  = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) dataDir.mkdirs();
        this.dataFile = new File(dataDir, "npcs.yml");
    }


    public void save(NPCEntity npc) {
        try {
            YamlConfiguration cfg = loadConfig();
            String path = "npcs." + npc.getId().toString();
            Location loc = npc.getLocation();

            cfg.set(path + ".name",           npc.getName());
            cfg.set(path + ".world",          loc.getWorld() != null ? loc.getWorld().getName() : "world");
            cfg.set(path + ".x",              loc.getX());
            cfg.set(path + ".y",              loc.getY());
            cfg.set(path + ".z",              loc.getZ());
            cfg.set(path + ".yaw",            (double) loc.getYaw());
            cfg.set(path + ".pitch",          (double) loc.getPitch());
            cfg.set(path + ".skin-value",     npc.getSkinValue()     != null ? npc.getSkinValue()     : "");
            cfg.set(path + ".skin-signature", npc.getSkinSignature() != null ? npc.getSkinSignature() : "");
            cfg.set(path + ".hologram-lines", npc.getHologramLines());

            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[NpcFant] Error al guardar NPC: " + npc.getId(), e);
        }
    }


    public void saveAll(Collection<NPCEntity> npcs) {
        for (NPCEntity npc : npcs) save(npc);
    }


    public void delete(UUID id) {
        try {
            YamlConfiguration cfg = loadConfig();
            cfg.set("npcs." + id.toString(), null);
            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[NpcFant] Error al eliminar NPC: " + id, e);
        }
    }


    public List<NPCEntity> loadAll(Plugin plugin) {
        List<NPCEntity> result = new ArrayList<>();
        if (!dataFile.exists()) return result;

        YamlConfiguration cfg = loadConfig();
        var section = cfg.getConfigurationSection("npcs");
        if (section == null) return result;

        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID   uuid  = UUID.fromString(uuidStr);
                String path  = "npcs." + uuidStr;

                String name      = cfg.getString(path + ".name", "NPC");
                String worldName = cfg.getString(path + ".world", "world");
                double x         = cfg.getDouble(path + ".x", 0);
                double y         = cfg.getDouble(path + ".y", 64);
                double z         = cfg.getDouble(path + ".z", 0);
                float  yaw       = (float) cfg.getDouble(path + ".yaw", 0);
                float  pitch     = (float) cfg.getDouble(path + ".pitch", 0);
                String skinVal   = cfg.getString(path + ".skin-value",     "");
                String skinSig   = cfg.getString(path + ".skin-signature", "");
                List<String> holoLines = cfg.getStringList(path + ".hologram-lines");

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("[NpcFant] Mundo '" + worldName
                        + "' no encontrado para NPC " + uuidStr + ". Omitiendo.");
                    continue;
                }

                Location loc  = new Location(world, x, y, z, yaw, pitch);
                SkinData skin = skinVal.isEmpty()
                    ? null
                    : new SkinData(skinVal, skinSig);

                NPCEntity entity = new NPCEntity(uuid, name, loc, plugin, skin);
                entity.setHologramLines(holoLines);
                result.add(entity);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                    "[NpcFant] Error al cargar NPC: " + uuidStr, e);
            }
        }
        return result;
    }

    private YamlConfiguration loadConfig() {
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); }
            catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "[NpcFant] No se pudo crear npcs.yml", e);
            }
        }
        return YamlConfiguration.loadConfiguration(dataFile);
    }
}
