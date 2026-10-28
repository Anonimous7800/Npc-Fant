package com.example.advancednpc.modules.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Optional;

public class UsernameSkinProvider implements SkinProvider {

    private static final String PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SESSION_URL  = "https://sessionserver.mojang.com/session/minecraft/profile/";

    @Override
    public String getType() { return "USERNAME"; }

    @Override
    public Optional<SkinData> fetchSkin(String username) {
        try {

            String uuid = fetchUUID(username);
            if (uuid == null) return Optional.empty();


            return fetchSkinByUUID(uuid);

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String fetchUUID(String username) throws Exception {
        URL url = new URL(PROFILE_URL + username);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        if (conn.getResponseCode() != 200) return null;

        JsonObject json = JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
        return json.get("id").getAsString();
    }

    private Optional<SkinData> fetchSkinByUUID(String uuid) throws Exception {
        URL url = new URL(SESSION_URL + uuid + "?unsigned=false");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        if (conn.getResponseCode() != 200) return Optional.empty();

        JsonObject json     = JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
        JsonObject property = json.getAsJsonArray("properties").get(0).getAsJsonObject();

        String value     = property.get("value").getAsString();
        String signature = property.has("signature") ? property.get("signature").getAsString() : "";

        return Optional.of(new SkinData(value, signature));
    }
}
