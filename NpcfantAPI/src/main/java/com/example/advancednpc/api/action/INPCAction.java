package com.example.advancednpc.api.action;

import org.bukkit.entity.Player;

public interface INPCAction {


    String getType();


    void execute(Player player, java.util.UUID npcId);


    java.util.Map<String, Object> serialize();


    static INPCAction deserialize(java.util.Map<String, Object> data) {
        throw new UnsupportedOperationException("Subclasses must implement deserialize()");
    }
}
