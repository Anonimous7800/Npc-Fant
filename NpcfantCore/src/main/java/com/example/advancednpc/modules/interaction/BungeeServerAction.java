package com.example.advancednpc.modules.interaction;

import com.example.advancednpc.api.action.INPCAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class BungeeServerAction implements INPCAction {

    public static final String TYPE = "BUNGEE_SERVER";
    private final String serverName;

    public BungeeServerAction(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public String getType() { return TYPE; }

    @Override
    public void execute(Player player, UUID npcId) {
        try {
            com.google.common.io.ByteArrayDataOutput out =
                    com.google.common.io.ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(
                    Bukkit.getPluginManager().getPlugin("NpcFant"),
                    "BungeeCord",
                    out.toByteArray()
            );
        } catch (Exception e) {
            Bukkit.getLogger().warning("[NpcFant] BungeeServerAction: no se pudo enviar al servidor '"
                    + serverName + "'. ¿Está configurado el canal BungeeCord?");
        }
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of("type", TYPE, "server", serverName);
    }

    public String getServerName() { return serverName; }
}
