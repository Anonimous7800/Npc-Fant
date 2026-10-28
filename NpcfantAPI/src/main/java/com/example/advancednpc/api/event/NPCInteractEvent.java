package com.example.advancednpc.api.event;

import com.example.advancednpc.api.INPC;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NPCInteractEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final Player player;
    private final INPC npc;
    private final InteractType interactType;

    public enum InteractType {
        LEFT_CLICK,
        RIGHT_CLICK
    }

    public NPCInteractEvent(Player player, INPC npc, InteractType interactType) {
        this.player = player;
        this.npc = npc;
        this.interactType = interactType;
    }


    public Player getPlayer() { return player; }


    public INPC getNpc() { return npc; }


    public InteractType getInteractType() { return interactType; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
