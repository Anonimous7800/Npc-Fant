package com.example.advancednpc.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class TranslationManager {

    private final JavaPlugin plugin;
    private final String langCode;

    private FileConfiguration defaultLangConfig;
    private FileConfiguration activeLangConfig;

    private static final List<String> SUPPORTED_LANGS = Arrays.asList(
        "de", "en", "es", "fr", "hr", "hu", "it", "nl", "no", "pt", "sv", "zh"
    );

    public TranslationManager(JavaPlugin plugin, String langCode) {
        this.plugin = plugin;
        this.langCode = langCode.toLowerCase();
        setupLangFolder();
        loadTranslations();
    }


    private void setupLangFolder() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }


        for (String lang : SUPPORTED_LANGS) {
            File langFile = new File(langDir, lang + ".yml");
            if (!langFile.exists()) {
                try {
                    plugin.saveResource("lang/" + lang + ".yml", false);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("No se pudo copiar lang/" + lang + ".yml desde los recursos.");
                }
            }
        }
    }


    public void loadTranslations() {

        File langDir = new File(plugin.getDataFolder(), "lang");
        File defaultFile = new File(langDir, "en.yml");
        if (defaultFile.exists()) {
            this.defaultLangConfig = YamlConfiguration.loadConfiguration(defaultFile);
        } else {

            InputStream in = plugin.getResource("lang/en.yml");
            if (in != null) {
                this.defaultLangConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        }


        File activeFile = new File(langDir, langCode + ".yml");
        if (activeFile.exists()) {
            this.activeLangConfig = YamlConfiguration.loadConfiguration(activeFile);
        } else {
            plugin.getLogger().warning("Idioma '" + langCode + "' no encontrado en el directorio 'lang/'. Usando 'en.yml' de fallback.");
            this.activeLangConfig = this.defaultLangConfig;
        }
    }


    public String getMessage(String path) {
        String msg = null;
        if (activeLangConfig != null) {
            msg = activeLangConfig.getString(path);
        }
        if (msg == null && defaultLangConfig != null) {
            msg = defaultLangConfig.getString(path);
        }
        if (msg == null) {
            return ChatColor.RED + "Missing translation: " + path;
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }


    public String getPrefix() {
        return getMessage("prefix");
    }
}
