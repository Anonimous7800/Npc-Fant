package com.example.advancednpc.modules.skin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class SkinManager {

    private final Plugin               plugin;
    private final SkinCache            cache;
    private final UsernameSkinProvider usernameProvider;


    private final Map<String, SkinData> presetSkins = new LinkedHashMap<>();

    private final Map<String, SkinData> localSkins  = new LinkedHashMap<>();

    private final Map<String, String>   presetDisplayNames = new LinkedHashMap<>();

    private final Map<String, String>   localDisplayNames  = new LinkedHashMap<>();

    public SkinManager(Plugin plugin) {
        this.plugin           = plugin;
        this.cache            = new SkinCache();
        this.usernameProvider = new UsernameSkinProvider();
        reload();
    }




    public void reload() {
        presetSkins.clear();
        localSkins.clear();
        presetDisplayNames.clear();
        localDisplayNames.clear();
        loadPresetSkins();
        loadLocalSkins();
        plugin.getLogger().info("[NpcFant] SkinManager: " + presetSkins.size()
            + " skins predefinidas, " + localSkins.size() + " skins locales cargadas.");
    }

    private void loadPresetSkins() {
        File skinsFile = new File(plugin.getDataFolder(), "skins/skins.yml");
        if (!skinsFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(skinsFile);
        var section = cfg.getConfigurationSection("skins");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String value     = section.getString(key + ".value", "");
            String signature = section.getString(key + ".signature", "");
            String display   = section.getString(key + ".display-name", key);
            if (!value.isEmpty()) {
                presetSkins.put(key.toLowerCase(), new SkinData(value, signature));
                presetDisplayNames.put(key.toLowerCase(), display);
            }
        }
    }

    private void loadLocalSkins() {
        File customDir = new File(plugin.getDataFolder(), "skins/custom");
        if (!customDir.exists()) customDir.mkdirs();
        File[] files = customDir.listFiles((d, n) -> n.endsWith(".yml") || n.endsWith(".yaml"));
        if (files == null) return;

        for (File f : files) {
            try {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                String value     = cfg.getString("value", "");
                String signature = cfg.getString("signature", "");
                String display   = cfg.getString("display-name",
                    cfg.getString("name",
                    f.getName().replace(".yml","").replace(".yaml","")));

                if (value.isEmpty()) {
                    plugin.getLogger().warning("[NpcFant] Skin local '" + f.getName()
                        + "' no tiene campo 'value'. Omitiendo.");
                    continue;
                }

                String key = f.getName().replace(".yml","").replace(".yaml","").toLowerCase();
                localSkins.put(key, new SkinData(value, signature));
                localDisplayNames.put(key, display);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                    "[NpcFant] Error leyendo skin local: " + f.getName(), e);
            }
        }
    }




    public Optional<SkinData> getSkin(String type, String input) {
        String key = input.toLowerCase();


        Optional<SkinData> cached = cache.get(type + ":" + key);
        if (cached.isPresent()) return cached;

        Optional<SkinData> result;
        switch (type.toLowerCase()) {
            case "preset":
                result = Optional.ofNullable(presetSkins.get(key));
                break;
            case "file":
                result = Optional.ofNullable(localSkins.get(key));
                break;
            case "username":
                result = usernameProvider.fetchSkin(input);
                break;
            default:

                result = Optional.ofNullable(presetSkins.get(key));
                if (!result.isPresent()) result = Optional.ofNullable(localSkins.get(key));
                break;
        }

        result.ifPresent(sd -> cache.put(type + ":" + key, sd));
        return result;
    }


    public SkinData getDefaultSkin() {
        SkinData steve = presetSkins.get("steve");
        return steve != null ? steve : new SkinData(
            com.example.advancednpc.modules.npc.FakePlayerNPC.STEVE_VALUE,
            com.example.advancednpc.modules.npc.FakePlayerNPC.STEVE_SIGNATURE
        );
    }




    public Map<String, SkinData> getPresetSkins()  { return Collections.unmodifiableMap(presetSkins); }

    public Map<String, SkinData> getLocalSkins()   { return Collections.unmodifiableMap(localSkins); }


    public String getPresetDisplayName(String key) {
        return presetDisplayNames.getOrDefault(key.toLowerCase(), key);
    }

    public String getLocalDisplayName(String key) {
        return localDisplayNames.getOrDefault(key.toLowerCase(), key);
    }


    public Map<String, String> getAllDisplayNames() {
        Map<String, String> all = new LinkedHashMap<>();
        presetDisplayNames.forEach((k, v) -> all.put("preset:" + k, v));
        localDisplayNames.forEach( (k, v) -> all.put("file:"   + k, v));
        return all;
    }


    public Map<String, SkinData> getAllSkins() {
        Map<String, SkinData> all = new LinkedHashMap<>();
        all.putAll(presetSkins);
        all.putAll(localSkins);
        return all;
    }

    public SkinCache getCache() { return cache; }
}
