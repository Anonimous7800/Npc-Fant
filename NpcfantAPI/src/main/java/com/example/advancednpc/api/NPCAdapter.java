package com.example.advancednpc.api;

import org.bukkit.Location;

public abstract class NPCAdapter {


    public abstract INPC spawnNPC(String name, Location location);


    public abstract boolean isSupported();
}
