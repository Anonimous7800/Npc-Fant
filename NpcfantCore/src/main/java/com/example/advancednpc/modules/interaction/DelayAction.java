package com.example.advancednpc.modules.interaction;

import com.example.advancednpc.api.action.INPCAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;

public class DelayAction implements INPCAction {

    public static final String TYPE = "DELAY";
    private final long ticks;
    private final JavaPlugin plugin;


    public DelayAction(JavaPlugin plugin, long ticks) {
        this.plugin = plugin;
        this.ticks  = ticks;
    }

    @Override
    public String getType() { return TYPE; }

    @Override
    public void execute(Player player, UUID npcId) {

        Bukkit.getScheduler().runTaskLater(plugin, () -> {}, ticks);
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of("type", TYPE, "ticks", ticks);
    }

    public long getTicks() { return ticks; }
}
