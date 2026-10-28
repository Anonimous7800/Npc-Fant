package com.example.advancednpc.modules.interaction;

import com.example.advancednpc.api.action.INPCAction;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ActionChain {

    private final List<INPCAction> actions = new ArrayList<>();

    public ActionChain() {}


    public void addAction(INPCAction action) {
        actions.add(action);
    }


    public void removeAction(int index) {
        if (index >= 0 && index < actions.size()) {
            actions.remove(index);
        }
    }


    public void execute(Player player, UUID npcId) {
        for (INPCAction action : actions) {
            action.execute(player, npcId);
        }
    }


    public List<INPCAction> getActions() {
        return Collections.unmodifiableList(actions);
    }


    public int size() {
        return actions.size();
    }


    public void clear() {
        actions.clear();
    }
}
