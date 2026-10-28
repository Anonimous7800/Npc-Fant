package com.example.advancednpc.commands;

import com.example.advancednpc.AdvancedNPCPlugin;
import com.example.advancednpc.modules.hologram.Hologram;
import com.example.advancednpc.npc.NPCEntity;
import com.example.advancednpc.npc.NPCManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class NPCCommand implements CommandExecutor, TabCompleter {

    private final AdvancedNPCPlugin plugin;
    private final NPCManager        npcManager;

    public NPCCommand(AdvancedNPCPlugin plugin) {
        this.plugin     = plugin;
        this.npcManager = plugin.getNpcManager();
    }

    private String getPrefix() {
        return plugin.getTranslationManager().getPrefix();
    }

    private String getMsg(String key) {
        return plugin.getTranslationManager().getMessage(key);
    }

    private static final List<String> SUB_COMMANDS = Arrays.asList(
        "create", "remove", "clone", "move", "tp",
        "skin", "name", "hologram", "pose", "equip",
        "glow", "invisible", "action", "visible", "reload",
        "list", "info", "copy"
    );

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getPrefix() + getMsg("commands.only-players"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {


            case "create":
                if (args.length < 3) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant create <tipo> <nombre>");
                    return true;
                }
                String type = args[1].toUpperCase();
                String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                npcManager.createNPC(name, player.getLocation());
                player.sendMessage(getPrefix() + ChatColor.GREEN + "NPC '" + name + "' creado correctamente.");
                break;


            case "remove": case "delete":
                if (args.length < 2) { player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant remove <uuid>"); return true; }
                try {
                    UUID rid = UUID.fromString(args[1]);
                    npcManager.removeNPC(rid);
                    player.sendMessage(getPrefix() + ChatColor.GREEN + "NPC eliminado.");
                } catch (IllegalArgumentException e) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "UUID invÃ¡lido.");
                }
                break;


            case "clone":
                if (args.length < 2) { player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant clone <uuid>"); return true; }
                try {
                    UUID cid = UUID.fromString(args[1]);
                    npcManager.cloneNPC(cid, player.getLocation());
                    player.sendMessage(getPrefix() + ChatColor.GREEN + "NPC clonado en tu posiciÃ³n.");
                } catch (IllegalArgumentException e) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "UUID invÃ¡lido.");
                }
                break;


            case "move":
                if (args.length < 2) { player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant move <uuid>"); return true; }
                try {
                    UUID mid = UUID.fromString(args[1]);
                    npcManager.moveNPC(mid, player.getLocation());
                    player.sendMessage(getPrefix() + ChatColor.GREEN + "NPC movido a tu posiciÃ³n.");
                } catch (IllegalArgumentException e) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "UUID invÃ¡lido.");
                }
                break;


            case "tp":
                if (args.length < 2) { player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant tp <uuid>"); return true; }
                try {
                    UUID tpId = UUID.fromString(args[1]);
                    var tpNpc = npcManager.getNPC(tpId);
                    if (tpNpc != null) player.teleport(tpNpc.getLocation());
                    else player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
                } catch (IllegalArgumentException e) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "UUID invÃ¡lido.");
                }
                break;


            case "skin":
                if (args.length < 4) { player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant skin <uuid> <preset|file|username> <valor>"); return true; }
                try {
                    UUID sid = UUID.fromString(args[1]);
                    String skinType = args[2].toLowerCase();
                    String input = args[3];

                    player.sendMessage(getPrefix() + ChatColor.YELLOW + "Buscando y aplicando skin...");
                    npcManager.applySkinAsync(sid, skinType, input, player);
                } catch (IllegalArgumentException e) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "UUID inválido.");
                }
                break;


            case "name":
                if (args.length < 3) { player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant name <uuid> <nombre>"); return true; }
                try {
                    UUID nid  = UUID.fromString(args[1]);
                    String newName = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).replace("&", "Â§");
                    var npc = (NPCEntity) npcManager.getNPC(nid);
                    if (npc != null) { npc.setName(newName); player.sendMessage(getPrefix() + ChatColor.GREEN + "Nombre actualizado."); }
                    else player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
                } catch (IllegalArgumentException e) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "UUID invÃ¡lido.");
                }
                break;


            case "hologram": case "holo":
                if (args.length < 3) { player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant hologram <uuid> <add|remove|edit> [texto]"); return true; }
                handleHologram(player, args);
                break;


            case "pose":
                if (args.length < 3) { player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant pose <uuid> <standing|sneaking|sitting|sleeping>"); return true; }
                try {
                    UUID pid = UUID.fromString(args[1]);
                    var pNpc = (NPCEntity) npcManager.getNPC(pid);
                    if (pNpc != null) {
                        try {
                            var pose = com.example.advancednpc.modules.animation.NPCPose.valueOf(args[2].toUpperCase());
                            pNpc.setPose(pose);
                            player.sendMessage(getPrefix() + ChatColor.GREEN + "Pose cambiada a " + pose.name() + ".");
                        } catch (IllegalArgumentException ex) {
                            player.sendMessage(getPrefix() + ChatColor.RED + "Pose invÃ¡lida. Opciones: standing, sneaking, sitting, sleeping");
                        }
                    } else player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
                } catch (IllegalArgumentException e) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "UUID invÃ¡lido.");
                }
                break;


            case "glow":
                if (args.length < 3) { player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant glow <uuid> <color|off>"); return true; }
                try {
                    UUID gid  = UUID.fromString(args[1]);
                    var gNpc  = (NPCEntity) npcManager.getNPC(gid);
                    if (gNpc != null) {
                        String color = args[2].equalsIgnoreCase("off") ? null : args[2].toUpperCase();
                        gNpc.setGlowColor(color);
                        player.sendMessage(getPrefix() + ChatColor.GREEN + (color == null ? "Glow desactivado." : "Glow activado en " + color + "."));
                    } else player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
                } catch (IllegalArgumentException e) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "UUID invÃ¡lido.");
                }
                break;


            case "invisible": case "invis":
                if (args.length < 2) { player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant invisible <uuid>"); return true; }
                try {
                    UUID iid  = UUID.fromString(args[1]);
                    var iNpc  = (NPCEntity) npcManager.getNPC(iid);
                    if (iNpc != null) {
                        iNpc.setInvisible(!iNpc.isInvisible());
                        player.sendMessage(getPrefix() + ChatColor.GREEN + "Invisibilidad: " + (iNpc.isInvisible() ? "activada" : "desactivada") + ".");
                    } else player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
                } catch (IllegalArgumentException e) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "UUID invÃ¡lido.");
                }
                break;


            case "reload":
                plugin.reloadConfig();
                plugin.getConfigManager().load();
                player.sendMessage(getPrefix() + ChatColor.GREEN + "ConfiguraciÃ³n recargada.");
                break;


            case "list":
                var allNpcs = npcManager.getAllNPCs();
                if (allNpcs.isEmpty()) {
                    player.sendMessage(getPrefix() + ChatColor.YELLOW + "No hay NPCs registrados.");
                } else {
                    player.sendMessage(getPrefix() + ChatColor.GOLD + "NPCs activos (" + allNpcs.size() + "):");
                    allNpcs.forEach(n -> player.sendMessage(ChatColor.GRAY + "  Â· " + ChatColor.WHITE + n.getName() + ChatColor.DARK_GRAY + " [" + n.getId() + "]"));
                }
                break;


            case "visible":
                player.sendMessage(getPrefix() + ChatColor.YELLOW + "Control de visibilidad en desarrollo.");
                break;


            case "action":
                player.sendMessage(getPrefix() + ChatColor.YELLOW + "GestiÃ³n de acciones en desarrollo.");
                break;

            default:
                sendHelp(player);
                break;
        }
        return true;
    }

    private void handleHologram(Player player, String[] args) {
        try {
            UUID hId  = UUID.fromString(args[1]);
            String sub = args[2].toLowerCase();

            switch (sub) {
                case "add":
                    if (args.length < 4) { player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant hologram <uuid> add <texto>"); return; }
                    String addText = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                    Hologram h = plugin.getNpcManager().attachHologram(hId, addText);
                    if (h != null) player.sendMessage(getPrefix() + ChatColor.GREEN + "LÃ­nea aÃ±adida al holograma.");
                    else player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
                    break;

                case "remove":
                    plugin.getHologramManager().getHologram(hId).ifPresentOrElse(
                        holo -> { holo.delete(); player.sendMessage(getPrefix() + ChatColor.GREEN + "Holograma eliminado."); },
                        ()   -> player.sendMessage(getPrefix() + ChatColor.RED + "Holograma no encontrado.")
                    );
                    break;

                case "list":
                    plugin.getHologramManager().getHologram(hId).ifPresentOrElse(
                        holo -> {
                            player.sendMessage(getPrefix() + ChatColor.GOLD + "LÃ­neas del holograma:");
                            List<String> lines = holo.getLines();
                            for (int i = 0; i < lines.size(); i++) {
                                player.sendMessage(ChatColor.GRAY + "  " + i + ": " + ChatColor.WHITE + lines.get(i));
                            }
                        },
                        () -> player.sendMessage(getPrefix() + ChatColor.RED + "Holograma no encontrado.")
                    );
                    break;

                default:
                    player.sendMessage(getPrefix() + ChatColor.RED + "Sub-comando hologram invÃ¡lido: add | remove | list");
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(getPrefix() + ChatColor.RED + "UUID invÃ¡lido.");
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "â•â•â•â• NpcFant â€” Comandos â•â•â•â•");
        player.sendMessage(ChatColor.YELLOW + "/npcfant create <tipo> <nombre>" + ChatColor.GRAY + " â€” Crea un NPC");
        player.sendMessage(ChatColor.YELLOW + "/npcfant remove <uuid>"          + ChatColor.GRAY + " â€” Elimina un NPC");
        player.sendMessage(ChatColor.YELLOW + "/npcfant clone <uuid>"           + ChatColor.GRAY + " â€” Clona un NPC");
        player.sendMessage(ChatColor.YELLOW + "/npcfant move <uuid>"            + ChatColor.GRAY + " â€” Mueve el NPC a tu posiciÃ³n");
        player.sendMessage(ChatColor.YELLOW + "/npcfant tp <uuid>"              + ChatColor.GRAY + " â€” Te teleporta al NPC");
        player.sendMessage(ChatColor.YELLOW + "/npcfant skin <uuid> <tipo> <v>" + ChatColor.GRAY + " â€” Cambia la skin");
        player.sendMessage(ChatColor.YELLOW + "/npcfant name <uuid> <nombre>"   + ChatColor.GRAY + " â€” Cambia el nombre");
        player.sendMessage(ChatColor.YELLOW + "/npcfant hologram <uuid> <sub>"  + ChatColor.GRAY + " â€” Gestiona hologramas");
        player.sendMessage(ChatColor.YELLOW + "/npcfant pose <uuid> <pose>"     + ChatColor.GRAY + " â€” Cambia la pose");
        player.sendMessage(ChatColor.YELLOW + "/npcfant glow <uuid> <color>"    + ChatColor.GRAY + " â€” Glow de colores");
        player.sendMessage(ChatColor.YELLOW + "/npcfant invisible <uuid>"       + ChatColor.GRAY + " â€” Toggle invisibilidad");
        player.sendMessage(ChatColor.YELLOW + "/npcfant action <uuid> <sub>"    + ChatColor.GRAY + " â€” Gestiona acciones");
        player.sendMessage(ChatColor.YELLOW + "/npcfant visible <uuid> <sub>"   + ChatColor.GRAY + " â€” Control de visibilidad");
        player.sendMessage(ChatColor.YELLOW + "/npcfant list"                   + ChatColor.GRAY + " â€” Lista todos los NPCs");
        player.sendMessage(ChatColor.YELLOW + "/npcfant reload"                 + ChatColor.GRAY + " â€” Recarga la config");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUB_COMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("pose")) {
                return Arrays.asList("standing", "sneaking", "sitting", "sleeping");
            }
            if (args[0].equalsIgnoreCase("skin")) {
                return Arrays.asList("username", "file", "preset");
            }
            if (args[0].equalsIgnoreCase("hologram")) {
                return Arrays.asList("add", "remove", "list");
            }
        }
        return Collections.emptyList();
    }
}
