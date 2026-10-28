package com.example.advancednpc.modules.skin;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.User;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;

public class UsernameSkinProvider implements SkinProvider {

    private static final String MOJANG_PROFILE_URL   = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String MCSERVICES_LOOKUP    = "https://api.minecraftservices.com/minecraft/profile/lookup/name/";
    private static final String MOJANG_SESSION_URL   = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final String ECLIPSE_UUID_URL     = "https://eclipse.skinsrestorer.net/mojang/uuid/";
    private static final String ECLIPSE_SKIN_URL     = "https://eclipse.skinsrestorer.net/mojang/skin/";
    private static final String ASHCON_URL           = "https://api.ashcon.app/mojang/v2/user/";
    private static final String PLAYERDB_URL         = "https://playerdb.co/api/player/minecraft/";
    private static final String MINETOOLS_UUID_URL   = "https://api.minetools.eu/uuid/";
    private static final String MINETOOLS_PROF_URL   = "https://api.minetools.eu/profile/";

    @Override
    public String getType() { return "USERNAME"; }

    @Override
    public Optional<SkinData> fetchSkin(String username) {
        if (username == null || username.trim().isEmpty()) return Optional.empty();
        String cleanName = username.trim();

        Optional<SkinData> fromOnline = fetchFromOnlinePlayer(cleanName);
        if (fromOnline.isPresent()) return fromOnline;

        Optional<SkinData> fromMojang = fetchFromMojang(cleanName);
        if (fromMojang.isPresent()) return fromMojang;

        Optional<SkinData> fromPaper = fetchFromPaperProfile(cleanName);
        if (fromPaper.isPresent()) return fromPaper;

        Optional<SkinData> fromEclipse = fetchFromEclipse(cleanName);
        if (fromEclipse.isPresent()) return fromEclipse;

        Optional<SkinData> fromAshcon = fetchFromAshcon(cleanName);
        if (fromAshcon.isPresent()) return fromAshcon;

        Optional<SkinData> fromPlayerDb = fetchFromPlayerDb(cleanName);
        if (fromPlayerDb.isPresent()) return fromPlayerDb;

        return fetchFromMineTools(cleanName);
    }

    private Optional<SkinData> fetchFromOnlinePlayer(String username) {
        try {
            Player online = Bukkit.getPlayerExact(username);
            if (online == null) return Optional.empty();

            try {
                User peUser = PacketEvents.getAPI().getPlayerManager().getUser(online);
                if (peUser != null && peUser.getProfile() != null) {
                    for (TextureProperty prop : peUser.getProfile().getTextureProperties()) {
                        if ("textures".equalsIgnoreCase(prop.getName())
                                && prop.getValue() != null && !prop.getValue().isEmpty()
                                && prop.getSignature() != null && !prop.getSignature().isEmpty()) {
                            return Optional.of(new SkinData(prop.getValue(), prop.getSignature()));
                        }
                    }
                }
            } catch (Throwable ignored) {}

            return extractFromPaperProfileObject(online.getPlayerProfile());
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    private Optional<SkinData> fetchFromPaperProfile(String username) {
        try {
            Method createProfile = Bukkit.class.getMethod("createProfile", String.class);
            Object profile = createProfile.invoke(null, username);
            if (profile == null) return Optional.empty();
            Method complete = profile.getClass().getMethod("complete", boolean.class);
            complete.invoke(profile, true);
            return extractFromPaperProfileObject(profile);
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    private Optional<SkinData> extractFromPaperProfileObject(Object profile) {
        try {
            if (profile == null) return Optional.empty();
            Method getProperties = profile.getClass().getMethod("getProperties");
            Object propsObj = getProperties.invoke(profile);
            if (propsObj instanceof Collection<?> props) {
                for (Object prop : props) {
                    if (prop == null) continue;
                    Method getName = prop.getClass().getMethod("getName");
                    String name = (String) getName.invoke(prop);
                    if ("textures".equalsIgnoreCase(name)) {
                        Method getValue = prop.getClass().getMethod("getValue");
                        Method getSignature = prop.getClass().getMethod("getSignature");
                        String value = (String) getValue.invoke(prop);
                        String sig = (String) getSignature.invoke(prop);
                        if (value != null && !value.isEmpty() && sig != null && !sig.isEmpty()) {
                            return Optional.of(new SkinData(value, sig));
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return Optional.empty();
    }

    private Optional<SkinData> fetchFromMojang(String username) {
        try {
            String uuid = fetchUUIDFromUrl(MOJANG_PROFILE_URL + username);
            if (uuid == null || uuid.isEmpty()) {
                uuid = fetchUUIDFromUrl(MCSERVICES_LOOKUP + username);
            }
            if (uuid == null || uuid.isEmpty()) return Optional.empty();
            return fetchSkinByUUID(MOJANG_SESSION_URL + uuid + "?unsigned=false");
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<SkinData> fetchFromEclipse(String username) {
        try {
            JsonObject uuidJson = getJson(ECLIPSE_UUID_URL + username);
            if (uuidJson == null) return Optional.empty();
            String uuid = null;
            if (uuidJson.has("uuid") && !uuidJson.get("uuid").isJsonNull()) {
                uuid = uuidJson.get("uuid").getAsString();
            } else if (uuidJson.has("id") && !uuidJson.get("id").isJsonNull()) {
                uuid = uuidJson.get("id").getAsString();
            }
            if (uuid == null || uuid.isEmpty()) return Optional.empty();
            return fetchSkinByUUID(ECLIPSE_SKIN_URL + uuid.replace("-", ""));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<SkinData> fetchFromAshcon(String username) {
        try {
            JsonObject json = getJson(ASHCON_URL + username);
            if (json == null || !json.has("textures")) return Optional.empty();
            JsonObject textures = json.getAsJsonObject("textures");
            if (!textures.has("raw")) return Optional.empty();
            JsonObject raw = textures.getAsJsonObject("raw");
            String value = raw.has("value") ? raw.get("value").getAsString() : "";
            String sig   = raw.has("signature") ? raw.get("signature").getAsString() : "";
            if (!value.isEmpty() && !sig.isEmpty()) {
                return Optional.of(new SkinData(value, sig));
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    private Optional<SkinData> fetchFromPlayerDb(String username) {
        try {
            JsonObject json = getJson(PLAYERDB_URL + username);
            if (json == null || !json.has("data")) return Optional.empty();
            JsonObject data = json.getAsJsonObject("data");
            if (!data.has("player")) return Optional.empty();
            JsonObject player = data.getAsJsonObject("player");
            if (player.has("properties") && player.get("properties").isJsonArray()) {
                Optional<SkinData> parsed = parsePropertiesArray(player.getAsJsonArray("properties"));
                if (parsed.isPresent()) return parsed;
            }
            if (player.has("raw_id")) {
                String uuid = player.get("raw_id").getAsString();
                return fetchSkinByUUID(MOJANG_SESSION_URL + uuid + "?unsigned=false");
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    private Optional<SkinData> fetchFromMineTools(String username) {
        try {
            JsonObject uuidJson = getJson(MINETOOLS_UUID_URL + username);
            if (uuidJson == null || !uuidJson.has("id") || uuidJson.get("id").isJsonNull()) {
                return Optional.empty();
            }
            String uuid = uuidJson.get("id").getAsString();
            if (uuid.isEmpty() || "null".equalsIgnoreCase(uuid)) return Optional.empty();
            JsonObject profJson = getJson(MINETOOLS_PROF_URL + uuid);
            if (profJson != null && profJson.has("raw")) {
                JsonObject raw = profJson.getAsJsonObject("raw");
                if (raw.has("properties") && raw.get("properties").isJsonArray()) {
                    return parsePropertiesArray(raw.getAsJsonArray("properties"));
                }
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    private String fetchUUIDFromUrl(String urlStr) {
        try {
            JsonObject json = getJson(urlStr);
            if (json != null && json.has("id") && !json.get("id").isJsonNull()) {
                return json.get("id").getAsString().replace("-", "");
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Optional<SkinData> fetchSkinByUUID(String urlStr) {
        try {
            JsonObject json = getJson(urlStr);
            if (json == null) return Optional.empty();
            if (json.has("properties") && json.get("properties").isJsonArray()) {
                return parsePropertiesArray(json.getAsJsonArray("properties"));
            }
            if (json.has("property") && json.get("property").isJsonObject()) {
                JsonObject prop = json.getAsJsonObject("property");
                String value = prop.has("value") ? prop.get("value").getAsString() : "";
                String sig   = prop.has("signature") ? prop.get("signature").getAsString() : "";
                if (!value.isEmpty() && !sig.isEmpty()) {
                    return Optional.of(new SkinData(value, sig));
                }
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    private Optional<SkinData> parsePropertiesArray(JsonArray arr) {
        for (JsonElement elem : arr) {
            if (!elem.isJsonObject()) continue;
            JsonObject prop = elem.getAsJsonObject();
            String value = prop.has("value") && !prop.get("value").isJsonNull() ? prop.get("value").getAsString() : "";
            String sig   = prop.has("signature") && !prop.get("signature").isJsonNull() ? prop.get("signature").getAsString() : "";
            if (!value.isEmpty() && !sig.isEmpty()) {
                return Optional.of(new SkinData(value, sig));
            }
        }
        return Optional.empty();
    }

    private JsonObject getJson(String urlStr) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(7000);
            conn.setRequestProperty("User-Agent", "NpcFant/2.0 (Minecraft Plugin)");
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonElement parsed = JsonParser.parseReader(reader);
                    if (parsed != null && parsed.isJsonObject()) {
                        return parsed.getAsJsonObject();
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }
}
