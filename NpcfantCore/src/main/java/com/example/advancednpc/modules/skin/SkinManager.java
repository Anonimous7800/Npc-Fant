package com.example.advancednpc.modules.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

public class SkinManager {

    private static final Map<String, String> PRESET_FALLBACK_USERS = Map.of(
        "steve",    "MHF_Steve",
        "alex",     "MHF_Alex",
        "guard",    "Knight",
        "merchant", "MHF_Villager",
        "warrior",  "Notch",
        "mage",     "jeb_"
    );

    private final Plugin               plugin;
    private final SkinCache            cache;
    private final UsernameSkinProvider usernameProvider;

    private final Map<String, SkinData> presetSkins        = new LinkedHashMap<>();
    private final Map<String, SkinData> localSkins         = new LinkedHashMap<>();
    private final Map<String, String>   localUsernames     = new LinkedHashMap<>();
    private final Map<String, File>     localPngFiles      = new LinkedHashMap<>();
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
        localUsernames.clear();
        localPngFiles.clear();
        presetDisplayNames.clear();
        localDisplayNames.clear();
        loadPresetSkins();
        loadLocalSkins();
        plugin.getLogger().info("[NpcFant] SkinManager: " + presetSkins.size()
            + " skins predefinidas, " + localDisplayNames.size() + " skins locales detectadas.");
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
        File skinsRoot = new File(plugin.getDataFolder(), "skins");
        if (!skinsRoot.exists()) skinsRoot.mkdirs();
        File customDir = new File(skinsRoot, "custom");
        if (!customDir.exists()) customDir.mkdirs();

        scanDirectoryForLocalSkins(skinsRoot, true);
        scanDirectoryForLocalSkins(customDir, false);
    }

    private void scanDirectoryForLocalSkins(File dir, boolean isRootSkinsDir) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) continue;
            String nameLower = f.getName().toLowerCase();
            if (isRootSkinsDir && nameLower.equals("skins.yml")) continue;

            if (nameLower.endsWith(".yml") || nameLower.endsWith(".yaml")) {
                try {
                    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                    String baseName  = f.getName().replaceAll("(?i)\\.ya?ml$", "");
                    String key       = baseName.toLowerCase();
                    String value     = cfg.getString("value", "");
                    String signature = cfg.getString("signature", "");
                    String username  = cfg.getString("username", cfg.getString("player", ""));
                    String display   = cfg.getString("display-name", cfg.getString("name", baseName));

                    if (!value.isEmpty()) {
                        localSkins.put(key, new SkinData(value, signature));
                        localDisplayNames.put(key, display);
                    } else if (!username.isEmpty()) {
                        localUsernames.put(key, username);
                        localDisplayNames.put(key, display);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING,
                        "[NpcFant] Error leyendo skin local: " + f.getName(), e);
                }
            } else if (nameLower.endsWith(".png")) {
                String baseName = f.getName().substring(0, f.getName().length() - 4);
                String key = baseName.toLowerCase();
                localPngFiles.put(key, f);
                localDisplayNames.putIfAbsent(key, baseName);
            }
        }
    }

    public Optional<SkinData> getSkin(String type, String input) {
        String key = input.toLowerCase();

        Optional<SkinData> cached = cache.get(type + ":" + key);
        if (cached.isPresent()) return cached;

        Optional<SkinData> result = Optional.empty();
        switch (type.toLowerCase()) {
            case "preset": {
                SkinData preset = presetSkins.get(key);
                if (preset != null && preset.getSignature() != null && !preset.getSignature().isEmpty()) {
                    result = Optional.of(preset);
                } else {
                    String fallbackUser = PRESET_FALLBACK_USERS.getOrDefault(key, input);
                    result = usernameProvider.fetchSkin(fallbackUser);
                    result.ifPresent(sd -> presetSkins.put(key, sd));
                }
                break;
            }
            case "file": {
                SkinData local = localSkins.get(key);
                if (local != null && local.getSignature() != null && !local.getSignature().isEmpty()) {
                    result = Optional.of(local);
                } else if (localUsernames.containsKey(key)) {
                    result = usernameProvider.fetchSkin(localUsernames.get(key));
                    result.ifPresent(sd -> localSkins.put(key, sd));
                } else if (localPngFiles.containsKey(key)) {
                    result = uploadPngToMineSkin(localPngFiles.get(key));
                    result.ifPresent(sd -> localSkins.put(key, sd));
                } else if (local != null) {
                    result = Optional.of(local);
                }
                break;
            }
            case "username":
                result = usernameProvider.fetchSkin(input);
                break;
            default:
                result = Optional.ofNullable(presetSkins.get(key));
                if (result.isEmpty()) result = getSkin("file", key);
                break;
        }

        result.ifPresent(sd -> cache.put(type + ":" + key, sd));
        return result;
    }

    private Optional<SkinData> uploadPngToMineSkin(File pngFile) {
        String boundary = "----NpcFantMineSkinBoundary" + System.currentTimeMillis();
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("https://api.mineskin.org/generate/upload").openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "NpcFant/2.0");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream out = conn.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true)) {
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(pngFile.getName()).append("\"\r\n");
                writer.append("Content-Type: image/png\r\n\r\n");
                writer.flush();
                Files.copy(pngFile.toPath(), out);
                out.flush();
                writer.append("\r\n--").append(boundary).append("--\r\n");
                writer.flush();
            }

            if (conn.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject texture = json.getAsJsonObject("data").getAsJsonObject("texture");
                    String value = texture.get("value").getAsString();
                    String signature = texture.get("signature").getAsString();
                    SkinData sd = new SkinData(value, signature);

                    File cachedYml = new File(pngFile.getParentFile(), pngFile.getName().substring(0, pngFile.getName().length() - 4) + ".yml");
                    YamlConfiguration cfg = new YamlConfiguration();
                    cfg.set("display-name", pngFile.getName().substring(0, pngFile.getName().length() - 4));
                    cfg.set("value", value);
                    cfg.set("signature", signature);
                    cfg.save(cachedYml);
                    return Optional.of(sd);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[NpcFant] No se pudo subir " + pngFile.getName() + " a MineSkin: " + e.getMessage());
        }
        return Optional.empty();
    }

    public SkinData getDefaultSkin() {
        SkinData steve = presetSkins.get("steve");
        if (steve != null && steve.getSignature() != null && !steve.getSignature().isEmpty()) {
            return steve;
        }
        return new SkinData("", "");
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
        localDisplayNames.forEach((k, v) -> all.put("file:"   + k, v));
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
