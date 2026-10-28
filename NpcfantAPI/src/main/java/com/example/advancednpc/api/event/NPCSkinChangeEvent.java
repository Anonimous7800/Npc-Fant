package com.example.advancednpc.api.event;

import com.example.advancednpc.api.INPC;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NPCSkinChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final INPC npc;
    private final String oldSkinValue;
    private final String newSkinValue;

    public NPCSkinChangeEvent(INPC npc, String oldSkinValue, String newSkinValue) {
        this.npc = npc;
        this.oldSkinValue = oldSkinValue;
        this.newSkinValue = newSkinValue;
    }

    public INPC getNpc() { return npc; }
    public String getOldSkinValue() { return oldSkinValue; }
    public String getNewSkinValue() { return newSkinValue; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
