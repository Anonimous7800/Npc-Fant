package com.example.advancednpc.modules.interaction;

import com.example.advancednpc.api.action.INPCAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class SequentialActionChain {

    private final List<INPCAction> actions = new ArrayList<>();

    private ClickType clickType = ClickType.ANY;

    private boolean blocking = false;

    public enum ClickType {
        LEFT_CLICK,
        RIGHT_CLICK,
        ANY
    }

    public SequentialActionChain() {}

    public void addAction(INPCAction action) {
        actions.add(action);
    }

    public void removeAction(int index) {
        if (index >= 0 && index < actions.size()) {
            actions.remove(index);
        }
    }

    public List<INPCAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public ClickType getClickType() { return clickType; }
    public void setClickType(ClickType ct) { this.clickType = ct; }

    public boolean isBlocking() { return blocking; }
    public void setBlocking(boolean b) { this.blocking = b; }


    public void execute(Player player, UUID npcId, JavaPlugin plugin,
                        Runnable onComplete) {
        executeStep(player, npcId, plugin, 0, 0L, onComplete);
    }

    private void executeStep(Player player, UUID npcId, JavaPlugin plugin,
                             int index, long accumulatedDelay, Runnable onComplete) {
        if (index >= actions.size()) {

            if (onComplete != null) {
                if (accumulatedDelay > 0) {
                    Bukkit.getScheduler().runTaskLater(plugin, onComplete, accumulatedDelay);
                } else {
                    onComplete.run();
                }
            }
            return;
        }

        INPCAction action = actions.get(index);

        if (action instanceof DelayAction delay) {

            executeStep(player, npcId, plugin, index + 1,
                    accumulatedDelay + delay.getTicks(), onComplete);
        } else {

            Runnable task = () -> {
                if (player.isOnline()) {
                    action.execute(player, npcId);
                }
                executeStep(player, npcId, plugin, index + 1, 0L, onComplete);
            };

            if (accumulatedDelay > 0) {
                Bukkit.getScheduler().runTaskLater(plugin, task, accumulatedDelay);
            } else {
                task.run();
            }
        }
    }

    public int size() { return actions.size(); }
    public void clear() { actions.clear(); }
}
