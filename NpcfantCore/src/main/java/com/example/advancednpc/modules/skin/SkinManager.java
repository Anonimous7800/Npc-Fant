package com.example.advancednpc.modules.skin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SkinManager {

    private static final Map<String, List<String>> PRESET_FALLBACK_USERS = Map.ofEntries(
        Map.entry("steve",    List.of("MHF_Steve", "Steve", "Dinnerbone")),
        Map.entry("alex",     List.of("MHF_Alex", "Alex", "Grumm")),
        Map.entry("guard",    List.of("Technoblade", "Ph1LzA", "Sapnap")),
        Map.entry("guardia",  List.of("Technoblade", "Ph1LzA", "Sapnap")),
        Map.entry("merchant", List.of("GoodTimesWithScar", "Grian", "CaptainSparklez")),
        Map.entry("mercader", List.of("GoodTimesWithScar", "Grian", "CaptainSparklez")),
        Map.entry("warrior",  List.of("Notch", "Sapnap", "Illumina")),
        Map.entry("guerrero", List.of("Notch", "Sapnap", "Illumina")),
        Map.entry("mage",     List.of("jeb_", "Etho", "DanTDM")),
        Map.entry("mago",     List.of("jeb_", "Etho", "DanTDM")),
        Map.entry("jugador",  List.of("MHF_Steve", "Steve", "Notch"))
    );

    private static final Map<String, String> BUILTIN_PNG_TO_PRESET = Map.ofEntries(
        Map.entry("steve",    "steve"),
        Map.entry("alex",     "alex"),
        Map.entry("jugador",  "steve"),
        Map.entry("guardia",  "guard"),
        Map.entry("guard",    "guard"),
        Map.entry("mercader", "merchant"),
        Map.entry("merchant", "merchant"),
        Map.entry("guerrero", "warrior"),
        Map.entry("warrior",  "warrior"),
        Map.entry("mago",     "mage"),
        Map.entry("mage",     "mage")
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
        warmupPresetsAsync();
    }

    public static boolean isSigned(SkinData sd) {
        return sd != null
            && sd.getValue() != null && !sd.getValue().trim().isEmpty()
            && sd.getSignature() != null && !sd.getSignature().trim().isEmpty();
    }

    public void reload() {
        Map<String, SkinData> previousSignedPresets = new HashMap<>();
        presetSkins.forEach((k, v) -> {
            if (isSigned(v)) previousSignedPresets.put(k, v);
        });
        Map<String, SkinData> previousSignedLocals = new HashMap<>();
        localSkins.forEach((k, v) -> {
            if (isSigned(v)) previousSignedLocals.put(k, v);
        });

        presetSkins.clear();
        localSkins.clear();
        localUsernames.clear();
        localPngFiles.clear();
        presetDisplayNames.clear();
        localDisplayNames.clear();

        loadPresetSkins(previousSignedPresets);
        loadLocalSkins(previousSignedLocals);
    }

    private void warmupPresetsAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean updatedAny = false;
            for (String key : new ArrayList<>(presetSkins.keySet())) {
                SkinData current = presetSkins.get(key);
                if (!isSigned(current)) {
                    Optional<SkinData> resolved = resolvePresetSkin(key);
                    if (resolved.isPresent() && isSigned(resolved.get())) {
                        presetSkins.put(key, resolved.get());
                        updatedAny = true;
                    }
                }
            }
            for (Map.Entry<String, String> entry : BUILTIN_PNG_TO_PRESET.entrySet()) {
                String pngKey = entry.getKey();
                String presetKey = entry.getValue();
                if (localPngFiles.containsKey(pngKey) && ! isSigned(localSkins.get(pngKey))) {
                    SkinData presetSd = presetSkins.get(presetKey);
                    File pngFile = localPngFiles.get(pngKey);
                    if (isSigned(presetSd) && pngFile != null && pngFile.length() < 2048) {
                        localSkins.put(pngKey, presetSd);
                        saveCachedPngSkin(pngKey, pngFile, presetSd);
                    }
                }
            }
            if (updatedAny) {
                savePresetSkinsToDisk();
            }
        });
    }

    private synchronized void savePresetSkinsToDisk() {
        try {
            File skinsFile = new File(plugin.getDataFolder(), "skins/skins.yml");
            if (!skinsFile.exists()) return;
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(skinsFile);
            for (Map.Entry<String, SkinData> entry : presetSkins.entrySet()) {
                String key = entry.getKey();
                SkinData sd = entry.getValue();
                if (isSigned(sd)) {
                    cfg.set("skins." + key + ".value", sd.getValue());
                    cfg.set("skins." + key + ".signature", sd.getSignature());
                }
            }
            cfg.save(skinsFile);
        } catch (Exception ignored) {}
    }

    private void loadPresetSkins(Map<String, SkinData> previousSignedPresets) {
        File skinsFile = new File(plugin.getDataFolder(), "skins/skins.yml");
        if (!skinsFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(skinsFile);
        var section = cfg.getConfigurationSection("skins");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String lower     = key.toLowerCase();
            String value     = section.getString(key + ".value", "");
            String signature = section.getString(key + ".signature", "");
            String display   = section.getString(key + ".display-name", key);
            presetDisplayNames.put(lower, display);

            SkinData diskSkin = new SkinData(value, signature);
            if (isSigned(diskSkin)) {
                presetSkins.put(lower, diskSkin);
            } else if (previousSignedPresets.containsKey(lower)) {
                presetSkins.put(lower, previousSignedPresets.get(lower));
            } else if (!value.isEmpty()) {
                presetSkins.put(lower, diskSkin);
            }
        }
    }

    private void loadLocalSkins(Map<String, SkinData> previousSignedLocals) {
        File skinsRoot = new File(plugin.getDataFolder(), "skins");
        if (!skinsRoot.exists()) skinsRoot.mkdirs();
        File customDir = new File(skinsRoot, "custom");
        if (!customDir.exists()) customDir.mkdirs();
        File cacheDir = new File(skinsRoot, ".cache");
        if (!cacheDir.exists()) cacheDir.mkdirs();

        scanDirectoryForLocalSkins(skinsRoot, true, cacheDir, previousSignedLocals);
    }

    private void scanDirectoryForLocalSkins(
        File dir,
        boolean isRootSkinsDir,
        File cacheDir,
        Map<String, SkinData> previousSignedLocals
    ) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                if (!f.getName().startsWith(".")) {
                    scanDirectoryForLocalSkins(f, false, cacheDir, previousSignedLocals);
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
                if (cachedDisk.isPresent() && isSigned(cachedDisk.get())) {
                    localSkins.put(key, cachedDisk.get());
                } else if (previousSignedLocals.containsKey(key)) {
                    localSkins.put(key, previousSignedLocals.get(key));
                } else {
                    String mappedPreset = BUILTIN_PNG_TO_PRESET.get(key);
                    if (mappedPreset != null && f.length() < 2048) {
                        SkinData preset = presetSkins.get(mappedPreset);
                        if (isSigned(preset)) {
                            localSkins.put(key, preset);
                        }
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

                    if (!value.isEmpty() && !signature.isEmpty()) {
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
        if (!isSigned(sd)) return;
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

    private Optional<SkinData> resolvePresetSkin(String key) {
        String normalized = normalizeSkinKey(key);
        String mapped = BUILTIN_PNG_TO_PRESET.getOrDefault(normalized, normalized);
        SkinData preset = presetSkins.get(mapped);
        if (isSigned(preset)) {
            return Optional.of(preset);
        }
        List<String> candidates = PRESET_FALLBACK_USERS.getOrDefault(mapped, List.of(key));
        for (String user : candidates) {
            Optional<SkinData> fetched = usernameProvider.fetchSkin(user);
            if (fetched.isPresent() && isSigned(fetched.get())) {
                presetSkins.put(mapped, fetched.get());
                savePresetSkinsToDisk();
                return fetched;
            }
        }
        return Optional.empty();
    }

    public Optional<SkinData> getSkin(String type, String input) {
        if (input == null || input.trim().isEmpty()) return Optional.empty();
        String trimmed = input.trim();

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return generateFromUrlMineSkin(trimmed);
        }

        String key = normalizeSkinKey(trimmed);
        String rawInput = trimmed.toLowerCase().endsWith(".png")
            ? trimmed.substring(0, trimmed.length() - 4)
            : trimmed;

        String effectiveType = type == null ? "auto" : type.toLowerCase();
        if (effectiveType.equals("png")) {
            effectiveType = "file";
        } else if (effectiveType.equals("auto")) {
            if (localPngFiles.containsKey(key) || localSkins.containsKey(key) || localUsernames.containsKey(key)) {
                effectiveType = "file";
            } else if (presetSkins.containsKey(key) || BUILTIN_PNG_TO_PRESET.containsKey(key)) {
                effectiveType = "preset";
            } else {
                effectiveType = "username";
            }
        }

        Optional<SkinData> cached = cache.get(effectiveType + ":" + key);
        if (cached.isPresent() && isSigned(cached.get())) {
            return cached;
        }

        Optional<SkinData> result = Optional.empty();
        switch (effectiveType) {
            case "preset": {
                result = resolvePresetSkin(key);
                break;
            }
            case "file": {
                File cacheDir = new File(plugin.getDataFolder(), "skins/.cache");
                if (localPngFiles.containsKey(key)) {
                    File pngFile = localPngFiles.get(key);
                    result = readCachedPngSkin(key, pngFile, cacheDir);

                    if (result.isEmpty() && BUILTIN_PNG_TO_PRESET.containsKey(key) && pngFile.length() < 2048) {
                        result = resolvePresetSkin(BUILTIN_PNG_TO_PRESET.get(key));
                    }

                    if (result.isEmpty()) {
                        result = uploadPngToMineSkin(pngFile);
                    }

                    if (result.isEmpty()) {
                        if (PRESET_FALLBACK_USERS.containsKey(key)) {
                            result = resolvePresetSkin(key);
                        } else {
                            result = usernameProvider.fetchSkin(rawInput);
                        }
                    }

                    if (result.isPresent() && isSigned(result.get())) {
                        SkinData sd = result.get();
                        localSkins.put(key, sd);
                        saveCachedPngSkin(key, pngFile, sd);
                    }
                }
                if (result.isEmpty()) {
                    SkinData local = localSkins.get(key);
                    if (isSigned(local)) {
                        result = Optional.of(local);
                    } else if (localUsernames.containsKey(key)) {
                        result = usernameProvider.fetchSkin(localUsernames.get(key));
                        result.filter(SkinManager::isSigned).ifPresent(sd -> localSkins.put(key, sd));
                    } else if (BUILTIN_PNG_TO_PRESET.containsKey(key)) {
                        result = resolvePresetSkin(BUILTIN_PNG_TO_PRESET.get(key));
                    }
                }
                break;
            }
            case "username":
                result = usernameProvider.fetchSkin(rawInput);
                if (result.isEmpty() && PRESET_FALLBACK_USERS.containsKey(key)) {
                    result = resolvePresetSkin(key);
                }
                break;
            default:
                if (localPngFiles.containsKey(key)) {
                    result = getSkin("file", key);
                } else if (presetSkins.containsKey(key) || BUILTIN_PNG_TO_PRESET.containsKey(key)) {
                    result = resolvePresetSkin(key);
                } else {
                    result = usernameProvider.fetchSkin(rawInput);
                }
                break;
        }

        if (result.isPresent() && isSigned(result.get())) {
            cache.put(effectiveType + ":" + key, result.get());
            return result;
        }
        return Optional.empty();
    }

    private byte[] normalizeSkinPng(File pngFile) throws IOException {
        BufferedImage src = ImageIO.read(pngFile);
        if (src == null) {
            return Files.readAllBytes(pngFile.toPath());
        }
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dst = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        if (w == 64 && h == 32) {
            g.drawImage(src, 0, 0, null);
            g.drawImage(src.getSubimage(0, 16, 16, 16), 16, 48, null);
            g.drawImage(src.getSubimage(40, 16, 16, 16), 32, 48, null);
        } else if (w == 64 && h == 64) {
            g.drawImage(src, 0, 0, null);
        } else {
            g.drawImage(src, 0, 0, 64, 64, null);
        }
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(dst, "png", baos);
        return baos.toByteArray();
    }

    private String getMineSkinApiKey() {
        try {
            String key = plugin.getConfig().getString("skins.mineskin-api-key", "");
            if (key == null || key.trim().isEmpty()) {
                key = plugin.getConfig().getString("mineskin-api-key", "");
            }
            return key != null ? key.trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private void applyMineSkinHeaders(HttpURLConnection conn) {
        conn.setRequestProperty("User-Agent", "NpcFant/2.0 (Minecraft Plugin)");
        conn.setRequestProperty("MineSkin-User-Agent", "NpcFant/2.0");
        conn.setRequestProperty("Accept", "application/json");
        String apiKey = getMineSkinApiKey();
        if (!apiKey.isEmpty()) {
            conn.setRequestProperty("Authorization", apiKey.startsWith("Bearer ") ? apiKey : "Bearer " + apiKey);
        }
    }

    private byte[] buildMultipartPayload(String boundary, String fileName, byte[] pngBytes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(pngBytes.length + 512);
        String header = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"visibility\"\r\n\r\n"
            + "public\r\n"
            + "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
            + "Content-Type: image/png\r\n\r\n";
        baos.write(header.getBytes(StandardCharsets.UTF_8));
        baos.write(pngBytes);
        String footer = "\r\n--" + boundary + "--\r\n";
        baos.write(footer.getBytes(StandardCharsets.UTF_8));
        return baos.toByteArray();
    }

    private Optional<SkinData> uploadPngToMineSkin(File pngFile) {
        try {
            byte[] pngBytes = normalizeSkinPng(pngFile);

            Optional<SkinData> v2Queue = submitMineSkinMultipart("https://api.mineskin.org/v2/queue", pngFile.getName(), pngBytes);
            if (v2Queue.isPresent()) return v2Queue;

            Optional<SkinData> v2Gen = submitMineSkinMultipart("https://api.mineskin.org/v2/generate", pngFile.getName(), pngBytes);
            if (v2Gen.isPresent()) return v2Gen;

            Optional<SkinData> v1Up = submitMineSkinMultipart("https://api.mineskin.org/generate/upload?visibility=1", pngFile.getName(), pngBytes);
            if (v1Up.isPresent()) return v1Up;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[NpcFant] No se pudo procesar PNG en MineSkin (" + pngFile.getName() + "): " + e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<SkinData> submitMineSkinMultipart(String endpointUrl, String fileName, byte[] pngBytes) {
        for (int attempt = 0; attempt < 2; attempt++) {
            HttpURLConnection conn = null;
            try {
                String boundary = "----NpcFantBoundary" + System.currentTimeMillis() + "_" + attempt;
                byte[] payload = buildMultipartPayload(boundary, fileName, pngBytes);

                conn = (HttpURLConnection) new URL(endpointUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(15000);
                applyMineSkinHeaders(conn);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setFixedLengthStreamingMode(payload.length);

                try (OutputStream out = conn.getOutputStream()) {
                    out.write(payload);
                    out.flush();
                }

                int status = conn.getResponseCode();
                InputStream stream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
                if (stream == null) return Optional.empty();

                JsonObject json;
                try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    JsonElement parsed = JsonParser.parseReader(reader);
                    if (parsed == null || !parsed.isJsonObject()) return Optional.empty();
                    json = parsed.getAsJsonObject();
                }

                if (status == 429 && attempt == 0) {
                    long waitMs = 3000L;
                    if (json.has("delay")) {
                        waitMs = Math.min(6000L, Math.max(1500L, json.get("delay").getAsLong() * 1000L));
                    } else if (json.has("nextRequest")) {
                        long diff = json.get("nextRequest").getAsLong() - System.currentTimeMillis();
                        if (diff > 0 && diff < 8000L) waitMs = diff;
                    }
                    Thread.sleep(waitMs);
                    continue;
                }

                Optional<SkinData> direct = extractMineSkinTexture(json);
                if (direct.isPresent()) return direct;

                if (json.has("job") && json.get("job").isJsonObject()) {
                    JsonObject job = json.getAsJsonObject("job");
                    if (job.has("id") && !job.get("id").isJsonNull()) {
                        String jobId = job.get("id").getAsString();
                        Optional<SkinData> polled = pollMineSkinJob(jobId);
                        if (polled.isPresent()) return polled;
                    }
                }
                return Optional.empty();
            } catch (Exception ignored) {
                return Optional.empty();
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        return Optional.empty();
    }

    private Optional<SkinData> pollMineSkinJob(String jobId) {
        String jobUrl = "https://api.mineskin.org/v2/queue/" + jobId;
        for (int i = 0; i < 12; i++) {
            HttpURLConnection conn = null;
            try {
                Thread.sleep(1200L);
                conn = (HttpURLConnection) new URL(jobUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(8000);
                applyMineSkinHeaders(conn);

                int status = conn.getResponseCode();
                if (status < 200 || status >= 300) continue;

                JsonObject json;
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonElement parsed = JsonParser.parseReader(reader);
                    if (parsed == null || !parsed.isJsonObject()) continue;
                    json = parsed.getAsJsonObject();
                }

                Optional<SkinData> extracted = extractMineSkinTexture(json);
                if (extracted.isPresent()) return extracted;

                if (json.has("job") && json.get("job").isJsonObject()) {
                    JsonObject job = json.getAsJsonObject("job");
                    String jobStatus = job.has("status") && !job.get("status").isJsonNull()
                        ? job.get("status").getAsString()
                        : "";
                    if ("failed".equalsIgnoreCase(jobStatus)) {
                        return Optional.empty();
                    }
                    if ("completed".equalsIgnoreCase(jobStatus) && job.has("result") && !job.get("result").isJsonNull()) {
                        String skinUuid = job.get("result").getAsString();
                        Optional<SkinData> byUuid = fetchMineSkinByUuid(skinUuid);
                        if (byUuid.isPresent()) return byUuid;
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        return Optional.empty();
    }

    private Optional<SkinData> fetchMineSkinByUuid(String skinUuid) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL("https://api.mineskin.org/v2/skins/" + skinUuid).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(8000);
            applyMineSkinHeaders(conn);
            if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonElement parsed = JsonParser.parseReader(reader);
                    if (parsed != null && parsed.isJsonObject()) {
                        return extractMineSkinTexture(parsed.getAsJsonObject());
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return Optional.empty();
    }

    private Optional<SkinData> generateFromUrlMineSkin(String skinUrl) {
        HttpURLConnection conn = null;
        try {
            JsonObject reqBody = new JsonObject();
            reqBody.addProperty("url", skinUrl);
            reqBody.addProperty("visibility", "public");
            byte[] payload = reqBody.toString().getBytes(StandardCharsets.UTF_8);

            conn = (HttpURLConnection) new URL("https://api.mineskin.org/v2/queue").openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            applyMineSkinHeaders(conn);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setFixedLengthStreamingMode(payload.length);

            try (OutputStream out = conn.getOutputStream()) {
                out.write(payload);
                out.flush();
            }

            if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    Optional<SkinData> direct = extractMineSkinTexture(json);
                    if (direct.isPresent()) return direct;
                    if (json.has("job") && json.get("job").isJsonObject()) {
                        JsonObject job = json.getAsJsonObject("job");
                        if (job.has("id") && !job.get("id").isJsonNull()) {
                            return pollMineSkinJob(job.get("id").getAsString());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return Optional.empty();
    }

    private Optional<SkinData> extractMineSkinTexture(JsonObject json) {
        if (json == null) return Optional.empty();
        try {
            if (json.has("skin") && json.get("skin").isJsonObject()) {
                JsonObject skin = json.getAsJsonObject("skin");
                Optional<SkinData> fromSkin = extractTextureFromSkinObject(skin);
                if (fromSkin.isPresent()) return fromSkin;
            }
            if (json.has("data") && json.get("data").isJsonObject()) {
                JsonObject data = json.getAsJsonObject("data");
                Optional<SkinData> fromData = extractTextureFromSkinObject(data);
                if (fromData.isPresent()) return fromData;
            }
            return extractTextureFromSkinObject(json);
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    private Optional<SkinData> extractTextureFromSkinObject(JsonObject obj) {
        if (obj == null || !obj.has("texture") || !obj.get("texture").isJsonObject()) {
            return Optional.empty();
        }
        JsonObject texture = obj.getAsJsonObject("texture");
        if (texture.has("data") && texture.get("data").isJsonObject()) {
            JsonObject data = texture.getAsJsonObject("data");
            String value = data.has("value") && !data.get("value").isJsonNull() ? data.get("value").getAsString() : "";
            String sig   = data.has("signature") && !data.get("signature").isJsonNull() ? data.get("signature").getAsString() : "";
            if (!value.isEmpty() && !sig.isEmpty()) {
                return Optional.of(new SkinData(value, sig));
            }
        }
        String value = texture.has("value") && !texture.get("value").isJsonNull() ? texture.get("value").getAsString() : "";
        String sig   = texture.has("signature") && !texture.get("signature").isJsonNull() ? texture.get("signature").getAsString() : "";
        if (!value.isEmpty() && !sig.isEmpty()) {
            return Optional.of(new SkinData(value, sig));
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
