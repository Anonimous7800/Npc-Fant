package com.example.advancednpc.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface INPC {


    java.util.UUID getId();


    String getName();


    void setName(String name);


    Location getLocation();


    void teleport(Location location);


    void lookAtPlayer(Player player);


    void remove();


    boolean isSpawned();
}
