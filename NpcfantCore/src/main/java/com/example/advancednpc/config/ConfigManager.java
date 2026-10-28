package com.example.advancednpc.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ConfigManager {

    private final JavaPlugin      plugin;
    private       FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }


    public void load() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();

        File configFile = new File(dataFolder, "config.yml");
        if (!configFile.exists()) {

            plugin.saveResource("config.yml", false);
        }




        File skinsFolder = new File(dataFolder, "skins");
        if (!skinsFolder.exists()) skinsFolder.mkdirs();

        File customSkinsFolder = new File(skinsFolder, "custom");
        if (!customSkinsFolder.exists()) customSkinsFolder.mkdirs();
        File skinsFile = new File(skinsFolder, "skins.yml");
        if (!skinsFile.exists()) {
            try {
                plugin.saveResource("skins/skins.yml", false);
            } catch (Exception e) {
                plugin.getLogger().warning("No se pudo copiar skins/skins.yml: " + e.getMessage());
            }
        }


        File hologramsFolder = new File(dataFolder, "holograms");
        if (!hologramsFolder.exists()) hologramsFolder.mkdirs();
        File hologramsFile = new File(hologramsFolder, "holograms.yml");
        if (!hologramsFile.exists()) {
            try {
                hologramsFile.createNewFile();
            } catch (Exception e) {
                plugin.getLogger().warning("No se pudo crear holograms/holograms.yml: " + e.getMessage());
            }
        }


        File dataDir = new File(dataFolder, "data");
        if (!dataDir.exists()) dataDir.mkdirs();


        File assetsDir = new File(dataFolder, "assets");
        if (!assetsDir.exists()) assetsDir.mkdirs();

        this.config = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().info("[NpcFant] Configuración y carpetas inicializadas correctamente.");
    }


    public void reload() {
        load();
    }


    public FileConfiguration getConfig() { return config; }


    public void save() { plugin.saveConfig(); }


    public String  getLanguage()          { return config.getString("settings.language", "es"); }
    public boolean isDebug()              { return config.getBoolean("settings.debug", false); }
    public long    getInteractionCooldown(){ return config.getLong("settings.interaction-cooldown-seconds", 2L); }
    public boolean isDialogueEnabled()    { return config.getBoolean("dialogue.enabled", false); }
    public String  getDialogueProvider()  { return config.getString("dialogue.provider", "SCRIPT"); }
}
