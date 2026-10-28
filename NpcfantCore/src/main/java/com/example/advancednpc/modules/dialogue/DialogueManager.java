package com.example.advancednpc.modules.dialogue;

import com.example.advancednpc.AdvancedNPCPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class DialogueManager implements Listener {

    private final AdvancedNPCPlugin plugin;


    private final Map<UUID, DialogueSession> activeSessions = new HashMap<>();

    public DialogueManager(AdvancedNPCPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }






    public void startDialogue(Player player, UUID npcId, DialogueNode root) {
        DialogueSession session = new DialogueSession(player.getUniqueId(), npcId, root);
        activeSessions.put(player.getUniqueId(), session);
        displayNode(player, root);
    }


    public boolean hasActiveSession(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }


    public void endDialogue(UUID playerId) {
        activeSessions.remove(playerId);
    }





    private void displayNode(Player player, DialogueNode node) {

        player.sendMessage(ChatColor.DARK_GRAY + "──────────────────────────────");


        final long[] delay = {0};
        for (String line : node.getMessages()) {
            long d = delay[0];
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(ChatColor.GOLD + "▶ " + ChatColor.translateAlternateColorCodes('&', line));
                }
            }, d);
            delay[0] += 30L;
        }


        long optionDelay = delay[0] + 5L;
        if (!node.getOptions().isEmpty()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                player.sendMessage(" ");
                for (int i = 0; i < node.getOptions().size(); i++) {
                    DialogueOption option = node.getOptions().get(i);
                    player.sendMessage(ChatColor.YELLOW + "[" + (i + 1) + "] " +
                            ChatColor.WHITE + option.getLabel());
                }
                player.sendMessage(ChatColor.DARK_GRAY + "──────────────────────────────");
                player.sendMessage(ChatColor.GRAY + "Escribe el número de tu respuesta en el chat.");
            }, optionDelay);
        } else {

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(ChatColor.DARK_GRAY + "──────────────────────────────");
                }
                endDialogue(player.getUniqueId());
            }, optionDelay);
        }
    }





    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uid = player.getUniqueId();

        if (!activeSessions.containsKey(uid)) return;

        event.setCancelled(true);

        DialogueSession session = activeSessions.get(uid);
        DialogueNode currentNode = session.getCurrentNode();
        List<DialogueOption> options = currentNode.getOptions();

        String input = event.getMessage().trim();
        int choice;
        try {
            choice = Integer.parseInt(input) - 1;
        } catch (NumberFormatException ex) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(ChatColor.RED + "Por favor escribe un número válido.")
            );
            return;
        }

        if (choice < 0 || choice >= options.size()) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(ChatColor.RED + "Opción inválida. Elige entre 1 y " + options.size() + ".")
            );
            return;
        }

        DialogueOption selected = options.get(choice);


        Bukkit.getScheduler().runTask(plugin, () -> {
            if (selected.getActions() != null) {
                selected.getActions().forEach(a -> a.execute(player, session.getNpcId()));
            }


            DialogueNode next = selected.getNextNode();
            if (next != null) {
                session.setCurrentNode(next);
                displayNode(player, next);
            } else {
                endDialogue(uid);
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        activeSessions.remove(event.getPlayer().getUniqueId());
    }
}
