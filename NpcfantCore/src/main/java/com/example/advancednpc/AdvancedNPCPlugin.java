package com.example.advancednpc;

import com.example.advancednpc.api.NPCAdapter;
import com.example.advancednpc.commands.NPCCommand;
import com.example.advancednpc.commands.SkinGUICommand;
import com.example.advancednpc.config.ConfigManager;
import com.example.advancednpc.config.TranslationManager;
import com.example.advancednpc.modules.dialogue.DialogueManager;
import com.example.advancednpc.modules.gui.NPCEditorGUI;
import com.example.advancednpc.modules.gui.SkinMenuGUI;
import com.example.advancednpc.modules.hologram.HologramManager;
import com.example.advancednpc.modules.interaction.CooldownManager;
import com.example.advancednpc.modules.skin.NPCStorage;
import com.example.advancednpc.modules.skin.SkinManager;
import com.example.advancednpc.npc.NPCManager;
import com.example.advancednpc.util.ProtocolPacketIdSync;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
    private NPCEditorGUI       npcEditorGUI;
    private com.example.advancednpc.modules.gui.NPCActionGUI    npcActionGUI;
    private com.example.advancednpc.modules.gui.NPCPoseGUI      npcPoseGUI;
    private com.example.advancednpc.modules.gui.NPCGlowGUI      npcGlowGUI;
    private com.example.advancednpc.modules.gui.NPCNameColorGUI npcNameColorGUI;
    private NPCStorage         npcStorage;

    @Override
    public void onLoad() {
        try {
            java.io.File skinsDir = new java.io.File(getDataFolder(), "skins/custom");
            if (!skinsDir.exists()) skinsDir.mkdirs();
        } catch (Throwable ignored) {}
        patchPacketEventsVersionBeforeBuild();
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        patchPacketEventsServerManager();
        PacketEvents.getAPI().getSettings()
            .reEncodeByDefault(false)
            .checkForUpdates(false);
        PacketEvents.getAPI().load();
        if (PacketEvents.getAPI().getServerManager().getVersion() == ServerVersion.getLatest()) {
            ProtocolPacketIdSync.synchronize(getLogger());
        }
    }

    private void patchPacketEventsVersionBeforeBuild() {
        try {
            String bukkitVersion = Bukkit.getBukkitVersion();
            for (ServerVersion v : ServerVersion.reversedValues()) {
                if (v != ServerVersion.ERROR && bukkitVersion.contains(v.getReleaseName())) {
                    return;
                }
            }

            String mcVersion;
            try {
                mcVersion = Bukkit.getMinecraftVersion();
            } catch (Throwable t) {
                int dash = bukkitVersion.indexOf('-');
                mcVersion = dash != -1 ? bukkitVersion.substring(0, dash) : bukkitVersion;
            }

            ServerVersion latest = ServerVersion.getLatest();
            Field nameField = ServerVersion.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(latest, mcVersion);
        } catch (Throwable ignored) {
        }
    }

    private void patchPacketEventsServerManager() {
        try {
            ServerManager sm = PacketEvents.getAPI().getServerManager();
            String bukkitVersion = Bukkit.getBukkitVersion();
            boolean matched = false;
            for (ServerVersion v : ServerVersion.reversedValues()) {
                if (v != ServerVersion.ERROR && bukkitVersion.contains(v.getReleaseName())) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                Field svField = sm.getClass().getDeclaredField("serverVersion");
                svField.setAccessible(true);
                svField.set(sm, ServerVersion.getLatest());
            }
        } catch (Throwable ignored) {
        }
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
        this.npcEditorGUI    = new NPCEditorGUI(this);
        this.npcActionGUI    = new com.example.advancednpc.modules.gui.NPCActionGUI(this);
        this.npcPoseGUI      = new com.example.advancednpc.modules.gui.NPCPoseGUI(this);
        this.npcGlowGUI      = new com.example.advancednpc.modules.gui.NPCGlowGUI(this);
        this.npcNameColorGUI = new com.example.advancednpc.modules.gui.NPCNameColorGUI(this);

        this.npcManager.setSkinManager(skinManager);
        this.npcManager.setStorage(npcStorage);
        this.npcManager.setEditorGUI(npcEditorGUI);
        NPCAdapter.setActiveManager(this.npcManager);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        NPCCommand     npcCmd     = new NPCCommand(this);
        SkinGUICommand skinGuiCmd = new SkinGUICommand(this, skinMenuGUI);
        com.example.advancednpc.commands.HoloCommand holoCmd = new com.example.advancednpc.commands.HoloCommand(this);

        if (getCommand("npcfant") != null) {
            getCommand("npcfant").setExecutor(npcCmd);
            getCommand("npcfant").setTabCompleter(npcCmd);
        }
        if (getCommand("skingui") != null) {
            getCommand("skingui").setExecutor(skinGuiCmd);
            getCommand("skingui").setTabCompleter(skinGuiCmd);
        }
        if (getCommand("holo") != null) {
            getCommand("holo").setExecutor(holoCmd);
            getCommand("holo").setTabCompleter(holoCmd);
        }

        getServer().getPluginManager().registerEvents(npcManager, this);
        getServer().getPluginManager().registerEvents(skinMenuGUI, this);
        getServer().getPluginManager().registerEvents(npcEditorGUI, this);
        getServer().getPluginManager().registerEvents(npcActionGUI, this);
        getServer().getPluginManager().registerEvents(npcPoseGUI, this);
        getServer().getPluginManager().registerEvents(npcGlowGUI, this);
        getServer().getPluginManager().registerEvents(npcNameColorGUI, this);

        getServer().getScheduler().runTaskLater(this, () -> npcManager.loadAll(), 40L);

        getLogger().info("╔══════════════════════════════════╗");
        getLogger().info("║    NpcFant v2.0.0 — ACTIVO       ║");
        getLogger().info("║    by anonimous1221               ║");
        getLogger().info("║    PacketEvents: 1.17 -> 26.3    ║");
        getLogger().info("╚══════════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        NPCAdapter.setActiveManager(null);
        if (npcManager      != null) npcManager.shutdown();
        if (hologramManager != null) hologramManager.shutdown();
        if (cooldownManager != null) cooldownManager.clearAll();

        PacketEvents.getAPI().terminate();

        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
        getLogger().info("[NpcFant] Plugin desactivado correctamente.");
    }

    public static AdvancedNPCPlugin getInstance()      { return instance; }
    public NPCManager         getNpcManager()          { return npcManager; }
    public ConfigManager      getConfigManager()       { return configManager; }
    public TranslationManager getTranslationManager()  { return translationManager; }
    public HologramManager    getHologramManager()     { return hologramManager; }
    public CooldownManager    getCooldownManager()     { return cooldownManager; }
    public DialogueManager    getDialogueManager()     { return dialogueManager; }
    public SkinManager        getSkinManager()         { return skinManager; }
    public SkinMenuGUI        getSkinMenuGUI()         { return skinMenuGUI; }
    public NPCEditorGUI       getNPCEditorGUI()        { return npcEditorGUI; }
    public com.example.advancednpc.modules.gui.NPCActionGUI getNPCActionGUI() { return npcActionGUI; }
    public com.example.advancednpc.modules.gui.NPCPoseGUI   getNPCPoseGUI()   { return npcPoseGUI; }
    public com.example.advancednpc.modules.gui.NPCGlowGUI   getNPCGlowGUI()   { return npcGlowGUI; }
    public com.example.advancednpc.modules.gui.NPCNameColorGUI getNPCNameColorGUI() { return npcNameColorGUI; }
    public NPCStorage         getNpcStorage()          { return npcStorage; }
}
