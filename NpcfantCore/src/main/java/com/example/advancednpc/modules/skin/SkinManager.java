package com.example.advancednpc.modules.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SkinManager {

    private static final Map<String, String> PRESET_FALLBACK_USERS = Map.of(
        "steve",    "MHF_Steve",
        "alex",     "MHF_Alex",
        "guard",    "Knight",
        "guardia",  "Knight",
        "merchant", "MHF_Villager",
        "mercader", "MHF_Villager",
        "warrior",  "Notch",
        "guerrero", "Notch",
        "mage",     "jeb_",
        "mago",     "jeb_"
    );

    private static final Map<String, String> BUILTIN_PNG_TO_PRESET = Map.of(
        "steve",    "steve",
        "alex",     "alex",
        "guardia",  "guard",
        "guard",    "guard",
        "mercader", "merchant",
        "merchant", "merchant",
        "guerrero", "warrior",
        "warrior",  "warrior",
        "mago",     "mage",
        "mage",     "mage"
    );

    private final Plugin               plugin;
    private final SkinCache            cache;
    private final UsernameSkinProvider usernameProvider;

    private final Map<String, SkinData> presetSkins        = new ConcurrentHashMap<>();
    private final Map<String, SkinData> localSkins         = new ConcurrentHashMap<>();
    private final Map<String, String>   localUsernames     = new ConcurrentHashMap<>();
    private final Map<String, File>     localPngFiles      = new ConcurrentHashMap<>();
    private final Map<String, String>   presetDisplayNames = new ConcurrentHashMap<>();
    private final Map<String, String>   localDisplayNames  = new ConcurrentHashMap<>();

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
            + " skins predefinidas, " + localPngFiles.size() + " skins PNG detectadas.");
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
        File cacheDir = new File(skinsRoot, ".cache");
        if (!cacheDir.exists()) cacheDir.mkdirs();

        scanDirectoryForLocalSkins(skinsRoot, true, cacheDir);
    }

    private void scanDirectoryForLocalSkins(File dir, boolean isRootSkinsDir, File cacheDir) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                if (!f.getName().startsWith(".")) {
                    scanDirectoryForLocalSkins(f, false, cacheDir);
                }
                continue;
            }
            String nameLower = f.getName().toLowerCase();
            if (isRootSkinsDir && nameLower.equals("skins.yml")) continue;

            if (nameLower.endsWith(".png")) {
                String baseName = f.getName().substring(0, f.getName().length() - 4);
                String key = baseName.toLowerCase();
                localPngFiles.put(key, f);
                localDisplayNames.put(key, "§b" + baseName + ".png");

                Optional<SkinData> cachedDisk = readCachedPngSkin(key, f, cacheDir);
                if (cachedDisk.isPresent()) {
                    localSkins.put(key, cachedDisk.get());
                } else {
                    String mappedPreset = BUILTIN_PNG_TO_PRESET.get(key);
                    if (mappedPreset != null && presetSkins.containsKey(mappedPreset)) {
                        localSkins.put(key, presetSkins.get(mappedPreset));
                    } else {
                        localSkins.putIfAbsent(key, getDefaultSkin());
                    }
                }
            } else if (nameLower.endsWith(".yml") || nameLower.endsWith(".yaml")) {
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
                        localDisplayNames.putIfAbsent(key, display);
                    } else if (!username.isEmpty()) {
                        localUsernames.put(key, username);
                        localDisplayNames.putIfAbsent(key, display);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING,
                        "[NpcFant] Error leyendo skin local: " + f.getName(), e);
                }
            }
        }
    }

    private Optional<SkinData> readCachedPngSkin(String key, File pngFile, File cacheDir) {
        try {
            File cacheFile = new File(cacheDir, key + ".cache.yml");
            if (!cacheFile.exists()) return Optional.empty();
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(cacheFile);
            long cachedSize = cfg.getLong("size", -1L);
            long cachedMod  = cfg.getLong("lastModified", -1L);
            String value    = cfg.getString("value", "");
            String sig      = cfg.getString("signature", "");
            if (!value.isEmpty() && !sig.isEmpty()
                    && cachedSize == pngFile.length()
                    && cachedMod == pngFile.lastModified()) {
                return Optional.of(new SkinData(value, sig));
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    private void saveCachedPngSkin(String key, File pngFile, SkinData sd) {
        try {
            File cacheDir = new File(plugin.getDataFolder(), "skins/.cache");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            File cacheFile = new File(cacheDir, key + ".cache.yml");
            YamlConfiguration cfg = new YamlConfiguration();
            cfg.set("size", pngFile.length());
            cfg.set("lastModified", pngFile.lastModified());
            cfg.set("value", sd.getValue());
            cfg.set("signature", sd.getSignature());
            cfg.save(cacheFile);
        } catch (Exception ignored) {}
    }

    public static String normalizeSkinKey(String input) {
        if (input == null) return "";
        String key = input.trim().toLowerCase();
        if (key.endsWith(".png")) {
            key = key.substring(0, key.length() - 4);
        } else if (key.endsWith(".yml")) {
            key = key.substring(0, key.length() - 4);
        } else if (key.endsWith(".yaml")) {
            key = key.substring(0, key.length() - 5);
        }
        return key;
    }

    public boolean hasLocalPng(String input) {
        return localPngFiles.containsKey(normalizeSkinKey(input));
    }

    public Optional<SkinData> getSkin(String type, String input) {
        String key = normalizeSkinKey(input);
        String rawInput = input.endsWith(".png") ? input.substring(0, input.length() - 4) : input;

        String effectiveType = type.toLowerCase();
        if (effectiveType.equals("png")) {
            effectiveType = "file";
        } else if (effectiveType.equals("auto")) {
            if (localPngFiles.containsKey(key) || localSkins.containsKey(key) || localUsernames.containsKey(key)) {
                effectiveType = "file";
            } else if (presetSkins.containsKey(key)) {
                effectiveType = "preset";
            } else {
                effectiveType = "username";
            }
        }

        Optional<SkinData> cached = cache.get(effectiveType + ":" + key);
        if (cached.isPresent() && cached.get().getSignature() != null && !cached.get().getSignature().isEmpty()) {
            return cached;
        }

        Optional<SkinData> result = Optional.empty();
        switch (effectiveType) {
            case "preset": {
                SkinData preset = presetSkins.get(key);
                if (preset != null && preset.getSignature() != null && !preset.getSignature().isEmpty()) {
                    result = Optional.of(preset);
                } else {
                    String fallbackUser = PRESET_FALLBACK_USERS.getOrDefault(key, rawInput);
                    result = usernameProvider.fetchSkin(fallbackUser);
                    result.ifPresent(sd -> presetSkins.put(key, sd));
                }
                break;
            }
            case "file": {
                File cacheDir = new File(plugin.getDataFolder(), "skins/.cache");
                if (localPngFiles.containsKey(key)) {
                    File pngFile = localPngFiles.get(key);
                    result = readCachedPngSkin(key, pngFile, cacheDir);
                    if (result.isEmpty()) {
                        result = uploadPngToMineSkin(pngFile);
                    }
                    if (result.isEmpty()) {
                        String fallbackUser = PRESET_FALLBACK_USERS.getOrDefault(key, rawInput);
                        result = usernameProvider.fetchSkin(fallbackUser);
                    }
                    if (result.isPresent()) {
                        SkinData sd = result.get();
                        localSkins.put(key, sd);
                        saveCachedPngSkin(key, pngFile, sd);
                    }
                }
                if (result.isEmpty()) {
                    SkinData local = localSkins.get(key);
                    if (local != null && local.getSignature() != null && !local.getSignature().isEmpty()) {
                        result = Optional.of(local);
                    } else if (localUsernames.containsKey(key)) {
                        result = usernameProvider.fetchSkin(localUsernames.get(key));
                        result.ifPresent(sd -> localSkins.put(key, sd));
                    } else if (local != null && !local.getValue().isEmpty()) {
                        result = Optional.of(local);
                    }
                }
                break;
            }
            case "username":
                result = usernameProvider.fetchSkin(rawInput);
                break;
            default:
                if (localPngFiles.containsKey(key)) {
                    result = getSkin("file", key);
                } else if (presetSkins.containsKey(key)) {
                    result = getSkin("preset", key);
                } else {
                    result = usernameProvider.fetchSkin(rawInput);
                }
                break;
        }

        final String finalType = effectiveType;
        result.ifPresent(sd -> cache.put(finalType + ":" + key, sd));
        return result;
    }

    private Optional<SkinData> uploadPngToMineSkin(File pngFile) {
        String boundary = "----NpcFantMineSkinBoundary" + System.currentTimeMillis();
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("https://api.mineskin.org/generate/upload?visibility=1").openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "NpcFant/2.0 (Minecraft Plugin)");
            conn.setRequestProperty("Accept", "application/json");
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

            int status = conn.getResponseCode();
            if (status >= 200 && status < 300) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (json.has("data")) {
                        JsonObject data = json.getAsJsonObject("data");
                        if (data.has("texture")) {
                            JsonObject texture = data.getAsJsonObject("texture");
                            String value = texture.get("value").getAsString();
                            String signature = texture.has("signature") ? texture.get("signature").getAsString() : "";
                            if (!value.isEmpty() && !signature.isEmpty()) {
                                return Optional.of(new SkinData(value, signature));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "[NpcFant] MineSkin fallback activado para " + pngFile.getName() + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    public SkinData getDefaultSkin() {
        SkinData steve = presetSkins.get("steve");
        if (steve != null && steve.getValue() != null && !steve.getValue().isEmpty()) {
            return steve;
        }
        return new SkinData("", "");
    }

    public Map<String, SkinData> getPresetSkins()  { return Collections.unmodifiableMap(presetSkins); }
    public Map<String, SkinData> getLocalSkins()   { return Collections.unmodifiableMap(localSkins); }
    public Map<String, File>     getLocalPngFiles(){ return Collections.unmodifiableMap(localPngFiles); }

    public String getPresetDisplayName(String key) {
        return presetDisplayNames.getOrDefault(normalizeSkinKey(key), key);
    }

    public String getLocalDisplayName(String key) {
        return localDisplayNames.getOrDefault(normalizeSkinKey(key), key + ".png");
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
