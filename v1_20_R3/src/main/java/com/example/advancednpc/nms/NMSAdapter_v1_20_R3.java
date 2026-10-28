package com.example.advancednpc.nms;

import com.example.advancednpc.api.INPC;
import com.example.advancednpc.api.INPCManager;
import com.example.advancednpc.api.NPCAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class NMSAdapter_v1_20_R3 extends NPCAdapter {

    private static final String TARGET_VERSION = "v1_20_R3";

    @Override
    public INPC spawnNPC(String name, Location location) {
        INPCManager manager = NPCAdapter.getActiveManager();
        if (manager == null) {
            throw new IllegalStateException("NpcFant manager no esta activo.");
        }
        return manager.createNPC(name, location);
    }

    @Override
    public boolean isSupported() {
        try {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            String[] parts = pkg.split("\\.");
            if (parts.length > 3) {
                return parts[3].equalsIgnoreCase(TARGET_VERSION);
            }
        } catch (Exception ignored) {}
        String bukkitVer = Bukkit.getServer().getBukkitVersion();
        String prefix = TARGET_VERSION.substring(1).replace("_R", ".").split("\\.")[0]
            + "." + TARGET_VERSION.substring(1).split("_")[1];
        return bukkitVer != null && bukkitVer.startsWith(prefix);
    }
}