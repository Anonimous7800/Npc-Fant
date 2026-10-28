package com.example.advancednpc.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ConfigManager {

    private static final String[] DEFAULT_PNG_SKINS = {
        "skins/steve.png",
        "skins/alex.png",
        "skins/jugador.png",
        "skins/custom/jugador.png",
        "skins/custom/guerrero.png",
        "skins/custom/mago.png",
        "skins/custom/guardia.png",
        "skins/custom/mercader.png"
    };

    private final JavaPlugin        plugin;
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

        File oldExampleYml = new File(customSkinsFolder, "ejemplo_jugador.yml");
        if (oldExampleYml.exists()) {
            oldExampleYml.delete();
        }
        File oldDefaultYml = new File(skinsFolder, "default.yml");
        if (oldDefaultYml.exists()) {
            oldDefaultYml.delete();
        }

        File skinsFile = new File(skinsFolder, "skins.yml");
        if (!skinsFile.exists()) {
            try {
                plugin.saveResource("skins/skins.yml", false);
            } catch (Exception e) {
                plugin.getLogger().warning("No se pudo copiar skins/skins.yml: " + e.getMessage());
            }
        }

        for (String resPath : DEFAULT_PNG_SKINS) {
            File target = new File(dataFolder, resPath);
            if (!target.exists()) {
                try {
                    plugin.saveResource(resPath, false);
                } catch (Exception ignored) {
                }
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

        File logoDir = new File(dataFolder, "logo");
        if (!logoDir.exists()) logoDir.mkdirs();
        File logoFile = new File(logoDir, "logo.png");
        if (!logoFile.exists()) {
            try {
                plugin.saveResource("logo/logo.png", false);
            } catch (Exception ignored) {}
        }

        this.config = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().info("[NpcFant] Configuración y carpetas de skins (.png) inicializadas correctamente.");
    }

    public void reload() {
        load();
    }

    public FileConfiguration getConfig() { return config; }

    public void save() { plugin.saveConfig(); }

    public String  getLanguage()            { return config.getString("settings.language", "es"); }
    public boolean isDebug()                { return config.getBoolean("settings.debug", false); }
    public long    getInteractionCooldown() { return config.getLong("settings.interaction-cooldown-seconds", 2L); }
    public boolean isDialogueEnabled()      { return config.getBoolean("dialogue.enabled", false); }
    public String  getDialogueProvider()    { return config.getString("dialogue.provider", "SCRIPT"); }
}
