package com.example.advancednpc.modules.interaction;

import com.example.advancednpc.api.action.INPCAction;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class PlayerCommandAction implements INPCAction {

    public static final String TYPE = "PLAYER_COMMAND";
    private final String command;

    public PlayerCommandAction(String command) {
        this.command = command;
    }

    @Override
    public String getType() { return TYPE; }

    @Override
    public void execute(Player player, UUID npcId) {
        String cmd = command.replace("{player}", player.getName())
                            .replace("{npc}",    npcId.toString());
        player.performCommand(cmd);
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of("type", TYPE, "command", command);
    }

    public String getCommand() { return command; }
}
