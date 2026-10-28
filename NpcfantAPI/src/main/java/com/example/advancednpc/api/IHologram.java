package com.example.advancednpc.api;

import org.bukkit.Location;
import java.util.UUID;

public interface IHologram {

    UUID getId();


    void addLine(String text);


    void setLine(int index, String text);


    void removeLine(int index);


    java.util.List<String> getLines();


    void move(Location location);


    Location getLocation();


    void showTo(org.bukkit.entity.Player player);


    void hideFrom(org.bukkit.entity.Player player);


    void delete();


    boolean isAlive();
}
