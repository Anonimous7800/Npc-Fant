package com.example.advancednpc.commands;

import com.example.advancednpc.AdvancedNPCPlugin;
import com.example.advancednpc.modules.hologram.Hologram;
import com.example.advancednpc.modules.hologram.HologramManager;
import com.example.advancednpc.npc.NPCEntity;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class HoloCommand implements CommandExecutor, TabCompleter {

    private final AdvancedNPCPlugin plugin;
    private final HologramManager hologramManager;

    public HoloCommand(AdvancedNPCPlugin plugin) {
        this.plugin = plugin;
        this.hologramManager = plugin.getHologramManager();
    }

    private String getPrefix() {
        return "§6[NpcFant] §r";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getPrefix() + "§cSolo jugadores pueden usar este comando.");
            return true;
        }

        if (!player.hasPermission("npcfant.hologram") && !player.isOp()) {
            player.sendMessage(getPrefix() + "§cNo tienes permisos para usar este comando.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "gui":
            case "menu": {
                Optional<NPCEntity> target = plugin.getNpcManager().getNearestNPC(player.getLocation(), 15.0);
                if (target.isPresent()) {
                    if (plugin.getNpcManager().getHologramGUI() != null) {
                        plugin.getNpcManager().getHologramGUI().openGUI(player, target.get());
                    }
                } else {
                    player.sendMessage(getPrefix() + "§cNo hay ningún NPC cercano para abrir el editor de hologramas visual.");
                }
                break;
            }
            case "create": {
                if (args.length < 2) {
                    player.sendMessage(getPrefix() + "§cUso: /" + label + " create <nombre> [texto inicial...]");
                    return true;
                }
                String name = args[1].toLowerCase();
                if (hologramManager.getNamedHologram(name).isPresent()) {
                    player.sendMessage(getPrefix() + "§cYa existe un holograma con el nombre '" + name + "'.");
                    return true;
                }
                Location loc = player.getLocation().clone().add(0, 1.2, 0);
                List<String> initialLines = new ArrayList<>();
                if (args.length >= 3) {
                    initialLines.add(String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                } else {
                    initialLines.add("§e§l" + name.toUpperCase());
                    initialLines.add("§7Usa /holo add " + name + " <texto>");
                }
                hologramManager.createNamedHologram(name, loc, initialLines);
                player.sendMessage(getPrefix() + "§aHolograma '§e" + name + "§a' creado en tu ubicación.");
                break;
            }

            case "remove":
            case "delete": {
                if (args.length < 2) {
                    player.sendMessage(getPrefix() + "§cUso: /" + label + " remove <nombre>");
                    return true;
                }
                String name = args[1].toLowerCase();
                if (hologramManager.removeNamedHologram(name)) {
                    player.sendMessage(getPrefix() + "§aHolograma '§e" + name + "§a' eliminado correctamente.");
                } else {
                    player.sendMessage(getPrefix() + "§cNo se encontró ningún holograma con el nombre '" + name + "'.");
                }
                break;
            }

            case "add": {
                if (args.length < 3) {
                    player.sendMessage(getPrefix() + "§cUso: /" + label + " add <nombre> <texto...>");
                    return true;
                }
                String name = args[1].toLowerCase();
                Optional<Hologram> opt = hologramManager.getNamedHologram(name);
                if (opt.isEmpty()) {
                    player.sendMessage(getPrefix() + "§cHolograma '" + name + "' no encontrado.");
                    return true;
                }
                String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                Hologram h = opt.get();
                h.addLine(text);
                hologramManager.saveHolograms();
                player.sendMessage(getPrefix() + "§aLínea añadida al holograma '§e" + name + "§a'.");
                break;
            }

            case "set":
            case "edit": {
                if (args.length < 4) {
                    player.sendMessage(getPrefix() + "§cUso: /" + label + " set <nombre> <índice> <texto...>");
                    return true;
                }
                String name = args[1].toLowerCase();
                Optional<Hologram> opt = hologramManager.getNamedHologram(name);
                if (opt.isEmpty()) {
                    player.sendMessage(getPrefix() + "§cHolograma '" + name + "' no encontrado.");
                    return true;
                }
                try {
                    int index = Integer.parseInt(args[2]);
                    Hologram h = opt.get();
                    if (index < 0 || index >= h.getLines().size()) {
                        player.sendMessage(getPrefix() + "§cÍndice inválido (0 a " + Math.max(0, h.getLines().size() - 1) + ").");
                        return true;
                    }
                    String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                    h.setLine(index, text);
                    hologramManager.saveHolograms();
                    player.sendMessage(getPrefix() + "§aLínea #" + index + " actualizada en '§e" + name + "§a'.");
                } catch (NumberFormatException e) {
                    player.sendMessage(getPrefix() + "§cEl índice debe ser un número entero.");
                }
                break;
            }

            case "insert": {
                if (args.length < 4) {
                    player.sendMessage(getPrefix() + "§cUso: /" + label + " insert <nombre> <índice> <texto...>");
                    return true;
                }
                String name = args[1].toLowerCase();
                Optional<Hologram> opt = hologramManager.getNamedHologram(name);
                if (opt.isEmpty()) {
                    player.sendMessage(getPrefix() + "§cHolograma '" + name + "' no encontrado.");
                    return true;
                }
                try {
                    int index = Integer.parseInt(args[2]);
                    Hologram h = opt.get();
                    String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                    h.insertLine(index, text);
                    hologramManager.saveHolograms();
                    player.sendMessage(getPrefix() + "§aLínea insertada en posición #" + index + " en '§e" + name + "§a'.");
                } catch (NumberFormatException e) {
                    player.sendMessage(getPrefix() + "§cEl índice debe ser un número entero.");
                }
                break;
            }

            case "removeline":
            case "delline": {
                if (args.length < 3) {
                    player.sendMessage(getPrefix() + "§cUso: /" + label + " removeline <nombre> <índice>");
                    return true;
                }
                String name = args[1].toLowerCase();
                Optional<Hologram> opt = hologramManager.getNamedHologram(name);
                if (opt.isEmpty()) {
                    player.sendMessage(getPrefix() + "§cHolograma '" + name + "' no encontrado.");
                    return true;
                }
                try {
                    int index = Integer.parseInt(args[2]);
                    Hologram h = opt.get();
                    if (index < 0 || index >= h.getLines().size()) {
                        player.sendMessage(getPrefix() + "§cÍndice fuera de rango.");
                        return true;
                    }
                    h.removeLine(index);
                    hologramManager.saveHolograms();
                    player.sendMessage(getPrefix() + "§aLínea #" + index + " eliminada de '§e" + name + "§a'.");
                } catch (NumberFormatException e) {
                    player.sendMessage(getPrefix() + "§cEl índice debe ser un número entero.");
                }
                break;
            }

            case "move":
            case "moveto": {
                if (args.length < 2) {
                    player.sendMessage(getPrefix() + "§cUso: /" + label + " move <nombre>");
                    return true;
                }
                String name = args[1].toLowerCase();
                Optional<Hologram> opt = hologramManager.getNamedHologram(name);
                if (opt.isEmpty()) {
                    player.sendMessage(getPrefix() + "§cHolograma '" + name + "' no encontrado.");
                    return true;
                }
                Location newLoc = player.getLocation().clone().add(0, 1.2, 0);
                opt.get().move(newLoc);
                hologramManager.saveHolograms();
                player.sendMessage(getPrefix() + "§aHolograma '§e" + name + "§a' movido a tu posición actual.");
                break;
            }

            case "tp":
            case "teleport": {
                if (args.length < 2) {
                    player.sendMessage(getPrefix() + "§cUso: /" + label + " tp <nombre>");
                    return true;
                }
                String name = args[1].toLowerCase();
                Optional<Hologram> opt = hologramManager.getNamedHologram(name);
                if (opt.isEmpty()) {
                    player.sendMessage(getPrefix() + "§cHolograma '" + name + "' no encontrado.");
                    return true;
                }
                Location loc = opt.get().getLocation();
                player.teleport(loc);
                player.sendMessage(getPrefix() + "§aTeletransportado al holograma '§e" + name + "§a'.");
                break;
            }

            case "list": {
                Map<String, Hologram> all = hologramManager.getNamedHolograms();
                if (all.isEmpty()) {
                    player.sendMessage(getPrefix() + "§7No hay hologramas creados en el mapa.");
                    return true;
                }
                player.sendMessage("§6════ Hologramas en el Mapa (" + all.size() + ") ════");
                for (Map.Entry<String, Hologram> e : all.entrySet()) {
                    Location l = e.getValue().getLocation();
                    String wName = l.getWorld() != null ? l.getWorld().getName() : "?";
                    player.sendMessage("§e• " + e.getKey() + " §7(" + wName + " " + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() + ") §f— §a" + e.getValue().getLines().size() + " líneas");
                }
                break;
            }

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6════ NpcFant — Hologramas ════");
        player.sendMessage("§e/holo create <nombre> [texto] §7— Crea un holograma en el mapa");
        player.sendMessage("§e/holo remove <nombre>         §7— Elimina un holograma");
        player.sendMessage("§e/holo add <nombre> <texto>    §7— Añade una línea al holograma");
        player.sendMessage("§e/holo set <nombre> <i> <txt>  §7— Edita una línea específica");
        player.sendMessage("§e/holo insert <nombre> <i> <txt> §7— Inserta una línea en posición");
        player.sendMessage("§e/holo removeline <nombre> <i> §7— Elimina una línea específica");
        player.sendMessage("§e/holo move <nombre>           §7— Mueve el holograma a tu posición");
        player.sendMessage("§e/holo tp <nombre>             §7— Te teletransporta al holograma");
        player.sendMessage("§e/holo list                    §7— Lista todos los hologramas");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList("create", "remove", "add", "set", "insert", "removeline", "move", "tp", "list");
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("create") && !args[0].equalsIgnoreCase("list")) {
            return hologramManager.getNamedHolograms().keySet().stream()
                .filter(k -> k.startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}

