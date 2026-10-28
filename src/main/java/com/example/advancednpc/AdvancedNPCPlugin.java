package com.example.advancednpc;

import org.bukkit.plugin.java.JavaPlugin;

public class AdvancedNPCPlugin extends JavaPlugin {
    private NPCManager npcManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.npcManager = new NPCManager(this);

        if (getCommand("advancednpc") != null) {
            getCommand("advancednpc").setExecutor(new NPCCommand(this));
        }

        getServer().getPluginManager().registerEvents(npcManager, this);
        getLogger().info("AdvancedNPCPlugin enabled");
    }

    @Override
    public void onDisable() {

        if (npcManager != null) {
            npcManager.shutdown();
        }
        getLogger().info("AdvancedNPCPlugin disabled");
    }

    public NPCManager getNpcManager() {
        return npcManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
