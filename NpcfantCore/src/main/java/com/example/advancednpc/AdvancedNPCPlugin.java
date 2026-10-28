package com.example.advancednpc;

import com.example.advancednpc.commands.NPCCommand;
import com.example.advancednpc.commands.SkinGUICommand;
import com.example.advancednpc.config.ConfigManager;
import com.example.advancednpc.config.TranslationManager;
import com.example.advancednpc.modules.dialogue.DialogueManager;
import com.example.advancednpc.modules.gui.SkinMenuGUI;
import com.example.advancednpc.modules.hologram.HologramManager;
import com.example.advancednpc.modules.interaction.CooldownManager;
import com.example.advancednpc.modules.skin.NPCStorage;
import com.example.advancednpc.modules.skin.SkinManager;
import com.example.advancednpc.npc.NPCManager;
import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;

public class AdvancedNPCPlugin extends JavaPlugin {

    private static AdvancedNPCPlugin instance;

    private NPCManager         npcManager;
    private ConfigManager      configManager;
    private TranslationManager translationManager;
    private HologramManager    hologramManager;
    private CooldownManager    cooldownManager;
    private DialogueManager    dialogueManager;
    private SkinManager        skinManager;
    private SkinMenuGUI        skinMenuGUI;
    private NPCStorage         npcStorage;

    @Override
    public void onLoad() {

        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings()
            .reEncodeByDefault(false)
            .checkForUpdates(false);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        instance = this;


        PacketEvents.getAPI().init();

        saveDefaultConfig();
        this.configManager      = new ConfigManager(this);
        this.translationManager = new TranslationManager(this, configManager.getLanguage());


        long cooldownSecs    = getConfig().getLong("settings.interaction-cooldown-seconds", 2L);
        this.cooldownManager = new CooldownManager(cooldownSecs);
        this.hologramManager = new HologramManager(this);
        this.npcManager      = new NPCManager(this, hologramManager, cooldownManager);
        this.skinManager     = new SkinManager(this);
        this.npcStorage      = new NPCStorage(this);
        this.dialogueManager = new DialogueManager(this);
        this.skinMenuGUI     = new SkinMenuGUI(this);


        this.npcManager.setSkinManager(skinManager);
        this.npcManager.setStorage(npcStorage);


        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");


        NPCCommand    npcCmd    = new NPCCommand(this);
        SkinGUICommand skinGuiCmd = new SkinGUICommand(this, skinMenuGUI);

        if (getCommand("npcfant") != null) {
            getCommand("npcfant").setExecutor(npcCmd);
            getCommand("npcfant").setTabCompleter(npcCmd);
        }
        if (getCommand("skingui") != null) {
            getCommand("skingui").setExecutor(skinGuiCmd);
            getCommand("skingui").setTabCompleter(skinGuiCmd);
        }


        getServer().getPluginManager().registerEvents(npcManager, this);
        getServer().getPluginManager().registerEvents(skinMenuGUI, this);


        getServer().getScheduler().runTaskLater(this, () -> npcManager.loadAll(), 40L);

        getLogger().info("╔══════════════════════════════════╗");
        getLogger().info("║    NpcFant v2.0.0 — ACTIVO       ║");
        getLogger().info("║    by anonimous1221               ║");
        getLogger().info("║    PacketEvents: 1.17 → 26.3     ║");
        getLogger().info("╚══════════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        if (npcManager     != null) npcManager.shutdown();
        if (hologramManager != null) hologramManager.shutdown();
        if (cooldownManager != null) cooldownManager.clearAll();


        PacketEvents.getAPI().terminate();

        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
        getLogger().info("[NpcFant] Plugin desactivado correctamente.");
    }


    public static AdvancedNPCPlugin getInstance()       { return instance; }
    public NPCManager          getNpcManager()          { return npcManager; }
    public ConfigManager       getConfigManager()       { return configManager; }
    public TranslationManager  getTranslationManager()  { return translationManager; }
    public HologramManager     getHologramManager()     { return hologramManager; }
    public CooldownManager     getCooldownManager()     { return cooldownManager; }
    public DialogueManager     getDialogueManager()     { return dialogueManager; }
    public SkinManager         getSkinManager()         { return skinManager; }
    public SkinMenuGUI         getSkinMenuGUI()         { return skinMenuGUI; }
    public NPCStorage          getNpcStorage()          { return npcStorage; }
}
