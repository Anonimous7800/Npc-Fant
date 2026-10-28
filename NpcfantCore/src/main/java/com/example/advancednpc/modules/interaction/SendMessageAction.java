package com.example.advancednpc.modules.interaction;

import com.example.advancednpc.api.action.INPCAction;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class SendMessageAction implements INPCAction {

    public static final String TYPE = "SEND_MESSAGE";
    private final String message;

    public SendMessageAction(String message) {
        this.message = message;
    }

    @Override
    public String getType() { return TYPE; }

    @Override
    public void execute(Player player, UUID npcId) {
        String msg = message
                .replace("{player}", player.getName())
                .replace("&", "§");
        player.sendMessage(msg);
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of("type", TYPE, "message", message);
    }

    public String getMessage() { return message; }
}
