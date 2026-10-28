package com.example.advancednpc.api;

import org.bukkit.Location;

import java.util.Collection;
import java.util.UUID;

public interface INPCManager {


    INPC createNPC(String name, Location location);


    void removeNPC(UUID id);


    INPC getNPC(UUID id);


    Collection<INPC> getAllNPCs();


    void shutdown();
}
