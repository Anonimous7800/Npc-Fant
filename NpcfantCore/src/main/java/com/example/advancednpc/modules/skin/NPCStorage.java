package com.example.advancednpc.modules.skin;

import com.example.advancednpc.api.action.INPCAction;
import com.example.advancednpc.modules.animation.NPCPose;
import com.example.advancednpc.modules.equipment.NPCEquipment;
import com.example.advancednpc.modules.interaction.*;
import com.example.advancednpc.modules.npc.NPCType;
import com.example.advancednpc.npc.NPCEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
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
            cfg.set(path + ".type",           npc.getType().name());
            cfg.set(path + ".pose",           npc.getPose().name());
            cfg.set(path + ".world",          loc.getWorld() != null ? loc.getWorld().getName() : "world");
            cfg.set(path + ".x",              loc.getX());
            cfg.set(path + ".y",              loc.getY());
            cfg.set(path + ".z",              loc.getZ());
            cfg.set(path + ".yaw",            (double) loc.getYaw());
            cfg.set(path + ".pitch",          (double) loc.getPitch());
            cfg.set(path + ".skin-value",     npc.getSkinValue()     != null ? npc.getSkinValue()     : "");
            cfg.set(path + ".skin-signature", npc.getSkinSignature() != null ? npc.getSkinSignature() : "");
            cfg.set(path + ".glow-color",     npc.getGlowColor()     != null ? npc.getGlowColor()     : "");
            cfg.set(path + ".invisible",      npc.isInvisible());
            cfg.set(path + ".look-at-player", npc.isLookAtPlayer());
            cfg.set(path + ".visible-to-all",   npc.isVisibleToAll());
            cfg.set(path + ".hologram-lines", npc.getHologramLines());

            NPCEquipment eq = npc.getEquipment();
            if (eq != null) {
                cfg.set(path + ".equipment.helmet",     eq.getHelmet());
                cfg.set(path + ".equipment.chestplate", eq.getChestplate());
                cfg.set(path + ".equipment.leggings",   eq.getLeggings());
                cfg.set(path + ".equipment.boots",      eq.getBoots());
                cfg.set(path + ".equipment.mainhand",   eq.getMainHand());
                cfg.set(path + ".equipment.offhand",    eq.getOffHand());
            }

            List<Map<String, Object>> serializedActions = new ArrayList<>();
            for (INPCAction action : npc.getActionChain().getActions()) {
                serializedActions.add(action.serialize());
            }
            cfg.set(path + ".actions", serializedActions);

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
        ConfigurationSection section = cfg.getConfigurationSection("npcs");
        if (section == null) return result;

        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID   uuid  = UUID.fromString(uuidStr);
                String path  = "npcs." + uuidStr;

                String name      = cfg.getString(path + ".name", "NPC");
                String typeStr   = cfg.getString(path + ".type", "PLAYER");
                String poseStr   = cfg.getString(path + ".pose", "STANDING");
                String worldName = cfg.getString(path + ".world", "world");
                double x         = cfg.getDouble(path + ".x", 0);
                double y         = cfg.getDouble(path + ".y", 64);
                double z         = cfg.getDouble(path + ".z", 0);
                float  yaw       = (float) cfg.getDouble(path + ".yaw", 0);
                float  pitch     = (float) cfg.getDouble(path + ".pitch", 0);
                String skinVal   = cfg.getString(path + ".skin-value",     "");
                String skinSig   = cfg.getString(path + ".skin-signature", "");
                String glowColor = cfg.getString(path + ".glow-color",     "");
                boolean invis    = cfg.getBoolean(path + ".invisible",     false);
                boolean lookAt   = cfg.getBoolean(path + ".look-at-player", true);
                boolean visAll   = cfg.getBoolean(path + ".visible-to-all", true);
                List<String> holoLines = cfg.getStringList(path + ".hologram-lines");

                World world = Bukkit.getWorld(worldName);
                if (world == null && !Bukkit.getWorlds().isEmpty()) {
                    world = Bukkit.getWorlds().get(0);
                }
                if (world == null) {
                    plugin.getLogger().warning("[NpcFant] Mundo '" + worldName
                        + "' no encontrado para NPC " + uuidStr + ". Omitiendo.");
                    continue;
                }

                NPCType npcType = NPCType.PLAYER;

                NPCPose npcPose = NPCPose.STANDING;
                try { npcPose = NPCPose.valueOf(poseStr.toUpperCase()); } catch (Exception ignored) {}

                Location loc  = new Location(world, x, y, z, yaw, pitch);
                SkinData skin = skinVal.isEmpty() ? null : new SkinData(skinVal, skinSig);

                NPCEntity entity = new NPCEntity(uuid, name, loc, plugin, skin, npcType);
                entity.setPose(npcPose);
                if (glowColor != null && !glowColor.isEmpty()) {
                    entity.setGlowColor(glowColor);
                }
                entity.setInvisible(invis);
                entity.setLookAtPlayer(lookAt);
                entity.setVisibleToAll(visAll);
                entity.setHologramLines(holoLines);

                NPCEquipment eq = new NPCEquipment();
                ItemStack helmet     = cfg.getItemStack(path + ".equipment.helmet");
                ItemStack chestplate = cfg.getItemStack(path + ".equipment.chestplate");
                ItemStack leggings   = cfg.getItemStack(path + ".equipment.leggings");
                ItemStack boots      = cfg.getItemStack(path + ".equipment.boots");
                ItemStack mainhand   = cfg.getItemStack(path + ".equipment.mainhand");
                ItemStack offhand    = cfg.getItemStack(path + ".equipment.offhand");
                eq.helmet(helmet).chestplate(chestplate).leggings(leggings)
                  .boots(boots).mainHand(mainhand).offHand(offhand);
                entity.setEquipment(eq);

                List<Map<?, ?>> actionList = cfg.getMapList(path + ".actions");
                if (actionList != null) {
                    for (Map<?, ?> map : actionList) {
                        INPCAction act = deserializeAction(map);
                        if (act != null) {
                            entity.getActionChain().addAction(act);
                        }
                    }
                }

                result.add(entity);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                    "[NpcFant] Error al cargar NPC: " + uuidStr, e);
            }
        }
        return result;
    }

    private INPCAction deserializeAction(Map<?, ?> map) {
        Object typeObj = map.get("type");
        if (typeObj == null) return null;
        String type = typeObj.toString().toUpperCase();
        switch (type) {
            case PlayerCommandAction.TYPE:
                return new PlayerCommandAction(String.valueOf(map.get("command")));
            case ConsoleCommandAction.TYPE:
                return new ConsoleCommandAction(String.valueOf(map.get("command")));
            case OpPlayerCommandAction.TYPE:
                return new OpPlayerCommandAction(String.valueOf(map.get("command")));
            case SendMessageAction.TYPE:
                return new SendMessageAction(String.valueOf(map.get("message")));
            case BungeeServerAction.TYPE:
                return new BungeeServerAction(String.valueOf(map.get("server")));
            case PlaySoundAction.TYPE: {
                String sound = String.valueOf(map.get("sound"));
                float vol = map.get("volume") instanceof Number n ? n.floatValue() : 1.0f;
                float pit = map.get("pitch") instanceof Number n ? n.floatValue() : 1.0f;
                return new PlaySoundAction(sound, vol, pit);
            }
            default:
                return null;
        }
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
