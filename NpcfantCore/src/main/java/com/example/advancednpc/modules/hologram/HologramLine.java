package com.example.advancednpc.modules.hologram;

public class HologramLine {

    private String rawText;

    public HologramLine(String rawText) {
        this.rawText = rawText;
    }


    public String getRawText() { return rawText; }


    public void setText(String text) { this.rawText = text; }


    public String getDisplayText() {
        return rawText.replace("&", "§");
    }


    public String getDisplayText(org.bukkit.entity.Player player) {
        String text = getDisplayText();

        try {
            if (org.bukkit.Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                Class<?> papi   = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                java.lang.reflect.Method method = papi.getMethod("setPlaceholders", org.bukkit.entity.Player.class, String.class);
                text = (String) method.invoke(null, player, text);
            }
        } catch (Exception ignored) {}
        return text;
    }


    public boolean isEmpty() {
        return rawText == null || rawText.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "HologramLine{text=" + rawText + "}";
    }
}
