package com.example.advancednpc.commands;

import com.example.advancednpc.AdvancedNPCPlugin;
import com.example.advancednpc.modules.gui.SkinMenuGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class SkinGUICommand implements CommandExecutor, TabCompleter {

    private final AdvancedNPCPlugin plugin;
    private final SkinMenuGUI       gui;

    public SkinGUICommand(AdvancedNPCPlugin plugin, SkinMenuGUI gui) {
        this.plugin = plugin;
        this.gui    = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "[NpcFant] Solo los jugadores pueden usar este comando.");
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("npcfant.skingui")) {
            player.sendMessage(ChatColor.RED + "[NpcFant] No tienes permiso para usar esto.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("open")) {

            gui.openMenu(player, 0);
            return true;
        }


        ItemStack openerItem = gui.createOpenerItem();
        if (player.getInventory().firstEmpty() == -1) {

            player.getWorld().dropItemNaturally(player.getLocation(), openerItem);
            player.sendMessage(ChatColor.YELLOW + "[NpcFant] Inventario lleno. "
                + "El ítem fue soltado en el suelo.");
        } else {
            player.getInventory().addItem(openerItem);
        }
        player.sendMessage(ChatColor.AQUA + "[NpcFant] " + ChatColor.WHITE
            + "Recibiste el " + ChatColor.AQUA + "✦ Editor de Skins" + ChatColor.WHITE
            + ". ¡Haz clic derecho para usarlo!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 1) return Collections.singletonList("open");
        return Collections.emptyList();
    }
}
