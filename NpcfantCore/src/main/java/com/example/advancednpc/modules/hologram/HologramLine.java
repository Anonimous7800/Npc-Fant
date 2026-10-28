package com.example.advancednpc.modules.hologram;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HologramLine {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private String rawText;

    public HologramLine(String rawText) {
        this.rawText = rawText != null ? rawText : "";
    }

    public String getRawText() {
        return rawText;
    }

    public void setText(String text) {
        this.rawText = text != null ? text : "";
    }

    public String getDisplayText() {
        return colorize(rawText);
    }

    public String getDisplayText(Player player) {
        if (rawText == null) return "";
        String text = rawText;

        if (player != null) {
            text = text
                .replace("{player}", player.getName())
                .replace("{displayname}", player.getDisplayName())
                .replace("{world}", player.getWorld().getName())
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("{max_players}", String.valueOf(Bukkit.getMaxPlayers()));

            try {
                if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                    Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                    Method method = papi.getMethod("setPlaceholders", Player.class, String.class);
                    Object result = method.invoke(null, player, text);
                    if (result instanceof String s) {
                        text = s;
                    }
                }
            } catch (Exception ignored) {}
        }

        return colorize(text);
    }

    public static String colorize(String input) {
        if (input == null || input.isEmpty()) return "";
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hex = "#" + matcher.group(1);
            try {
                matcher.appendReplacement(buffer, ChatColor.of(hex).toString());
            } catch (Exception e) {
                matcher.appendReplacement(buffer, "");
            }
        }
        matcher.appendTail(buffer);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public boolean isEmpty() {
        return rawText == null || rawText.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "HologramLine{text=" + rawText + "}";
    }
}
