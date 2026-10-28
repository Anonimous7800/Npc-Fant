package com.example.advancednpc.modules.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileReader;
import java.util.Optional;
import java.util.logging.Level;

public class LocalFileSkinProvider implements SkinProvider {

    private final File folder;

    public LocalFileSkinProvider(File dataFolder) {
        this.folder = new File(dataFolder, "skins/custom");
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    @Override
    public String getType() {
        return "FILE";
    }

    @Override
    public Optional<SkinData> fetchSkin(String input) {
        String fileName = input;


        File file = new File(folder, fileName);
        if (!file.exists()) {
            file = new File(folder, fileName + ".yml");
        }
        if (!file.exists()) {
            file = new File(folder, fileName + ".json");
        }

        if (!file.exists()) {
            return Optional.empty();
        }

        try {
            if (file.getName().endsWith(".json")) {
                return parseJson(file);
            } else {
                return parseYaml(file);
            }
        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger().log(Level.WARNING, "[NpcFant] Error leyendo skin local " + file.getName(), e);
            return Optional.empty();
        }
    }

    private Optional<SkinData> parseYaml(File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String value = cfg.getString("value", "");
        String signature = cfg.getString("signature", "");
        if (value.isEmpty()) return Optional.empty();
        return Optional.of(new SkinData(value, signature));
    }

    private Optional<SkinData> parseJson(File file) throws Exception {
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("data")) {
                JsonObject data = json.getAsJsonObject("data");
                if (data.has("texture")) {
                    JsonObject texture = data.getAsJsonObject("texture");
                    String value = texture.get("value").getAsString();
                    String signature = texture.has("signature") ? texture.get("signature").getAsString() : "";
                    return Optional.of(new SkinData(value, signature));
                }
            }
            return Optional.empty();
        }
    }
}
