package com.example.advancednpc.modules.interaction;

import com.example.advancednpc.api.action.INPCAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class OpPlayerCommandAction implements INPCAction {

    public static final String TYPE = "OP_PLAYER_COMMAND";
    private final String command;

    public OpPlayerCommandAction(String command) {
        this.command = command;
    }

    @Override
    public String getType() { return TYPE; }

    @Override
    public void execute(Player player, UUID npcId) {
        String cmd = command.replace("{player}", player.getName())
                            .replace("{npc}",    npcId.toString())
                            .replace("{uuid}",   player.getUniqueId().toString());
        boolean wasOp = player.isOp();
        try {
            if (!wasOp) player.setOp(true);
            player.performCommand(cmd);
        } finally {
            if (!wasOp) player.setOp(false);
        }
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of("type", TYPE, "command", command);
    }

    public String getCommand() { return command; }
}
