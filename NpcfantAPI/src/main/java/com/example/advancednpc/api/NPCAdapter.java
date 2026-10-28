package com.example.advancednpc.api;

import org.bukkit.Location;

public abstract class NPCAdapter {

    private static INPCManager activeManager;

    public static void setActiveManager(INPCManager manager) {
        activeManager = manager;
    }

    public static INPCManager getActiveManager() {
        return activeManager;
    }

    public abstract INPC spawnNPC(String name, Location location);

    public abstract boolean isSupported();
}
