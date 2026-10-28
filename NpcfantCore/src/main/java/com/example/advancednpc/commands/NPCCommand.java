package com.example.advancednpc.commands;

import com.example.advancednpc.AdvancedNPCPlugin;
import com.example.advancednpc.api.action.INPCAction;
import com.example.advancednpc.modules.animation.NPCPose;
import com.example.advancednpc.modules.equipment.NPCEquipment;
import com.example.advancednpc.modules.hologram.Hologram;
import com.example.advancednpc.modules.interaction.*;
import com.example.advancednpc.modules.npc.NPCType;
import com.example.advancednpc.npc.NPCEntity;
import com.example.advancednpc.npc.NPCManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
        "skin", "setskin", "name", "hologram", "pose", "equip",
        "glow", "invisible", "action", "visible", "reload",
        "list", "info", "copy", "look", "animate", "nametag"
    );

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getPrefix() + getMsg("commands.only-players"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "create": {
                if (args.length < 2) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant create <tipo|nombre> [nombre]");
                    return true;
                }
                NPCType npcType = NPCType.PLAYER;
                String name;
                if (args.length == 2) {
                    name = args[1];
                } else {
                    try {
                        npcType = NPCType.valueOf(args[1].toUpperCase());
                        name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    } catch (IllegalArgumentException ex) {
                        name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                    }
                }
                NPCEntity created = npcManager.createNPC(name, player.getLocation(), npcType);
                player.sendMessage(getPrefix() + ChatColor.GREEN + "NPC '" + ChatColor.RESET + created.getName()
                    + ChatColor.GREEN + "' (" + npcType.name() + ") creado. ID: " + ChatColor.YELLOW + created.getId());
                break;
            }

            case "remove":
            case "delete": {
                if (args.length < 2) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant remove <uuid|nombre|nearest>");
                    return true;
                }
                Optional<NPCEntity> target = npcManager.resolveNPC(args[1], player);
                if (target.isPresent()) {
                    npcManager.removeNPC(target.get().getId());
                    player.sendMessage(getPrefix() + ChatColor.GREEN + "NPC eliminado correctamente.");
                } else {
                    player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
                }
                break;
            }

            case "clone":
            case "copy": {
                if (args.length < 2) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant " + args[0].toLowerCase() + " <uuid|nombre|nearest>");
                    return true;
                }
                Optional<NPCEntity> target = npcManager.resolveNPC(args[1], player);
                if (target.isPresent()) {
                    npcManager.cloneNPC(target.get().getId(), player.getLocation());
                    player.sendMessage(getPrefix() + ChatColor.GREEN + "NPC clonado en tu posición.");
                } else {
                    player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
                }
                break;
            }

            case "move":
            case "tphere": {
                if (args.length < 2) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant move <uuid|nombre|nearest>");
                    return true;
                }
                Optional<NPCEntity> target = npcManager.resolveNPC(args[1], player);
                if (target.isPresent()) {
                    npcManager.moveNPC(target.get().getId(), player.getLocation());
                    player.sendMessage(getPrefix() + ChatColor.GREEN + "NPC movido a tu posición.");
                } else {
                    player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
                }
                break;
            }

            case "tp": {
                if (args.length < 2) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant tp <uuid|nombre|nearest>");
                    return true;
                }
                Optional<NPCEntity> target = npcManager.resolveNPC(args[1], player);
                if (target.isPresent()) {
                    player.teleport(target.get().getLocation());
                    player.sendMessage(getPrefix() + ChatColor.GREEN + "Teletransportado al NPC.");
                } else {
                    player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
                }
                break;
            }

            case "skin":
            case "setskin": {
                if (args.length < 3) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant skin <npc> <archivo.png|jugador> o /npcfant skin <npc> <png|file|username|preset> <valor>");
                    return true;
                }
                Optional<NPCEntity> target = npcManager.resolveNPC(args[1], player);
                if (target.isEmpty()) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
                    return true;
                }
                plugin.getSkinManager().reload();
                String skinType;
                String input;
                if (args.length == 3) {
                    input = args[2];
                    if (input.toLowerCase().endsWith(".png") || plugin.getSkinManager().hasLocalPng(input)) {
                        skinType = "file";
                    } else {
                        skinType = "auto";
                    }
                } else {
                    skinType = args[2].toLowerCase();
                    if (skinType.equals("png")) skinType = "file";
                    input = args[3];
                }
                player.sendMessage(getPrefix() + ChatColor.YELLOW + "Importando y aplicando skin '" + input + "'...");
                npcManager.applySkinAsync(target.get().getId(), skinType, input, player);
                break;
            }

            case "name": {
                if (args.length < 3) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant name <uuid|nombre|nearest> <nuevoNombre>");
                    return true;
                }
                Optional<NPCEntity> target = npcManager.resolveNPC(args[1], player);
                if (target.isPresent()) {
                    String newName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    target.get().setName(newName);
                    npcManager.saveNPC(target.get());
                    player.sendMessage(getPrefix() + ChatColor.GREEN + "Nombre actualizado a: " + ChatColor.RESET + target.get().getName());
                } else {
                    player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
                }
                break;
            }

            case "hologram":
            case "holo": {
                if (args.length < 3) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant hologram <uuid|nombre|nearest> <add|set|remove|list> [texto]");
                    return true;
                }
                handleHologram(player, args);
                break;
            }

            case "pose": {
                if (args.length < 3) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant pose <uuid|nombre|nearest> <standing|sneaking|sitting|sleeping|swimming>");
                    return true;
                }
                Optional<NPCEntity> target = npcManager.resolveNPC(args[1], player);
                if (target.isPresent()) {
                    try {
                        NPCPose pose = NPCPose.valueOf(args[2].toUpperCase());
                        target.get().setPose(pose);
                        npcManager.saveNPC(target.get());
                        player.sendMessage(getPrefix() + ChatColor.GREEN + "Pose cambiada a " + pose.name() + ".");
                    } catch (IllegalArgumentException ex) {
                        player.sendMessage(getPrefix() + ChatColor.RED + "Pose inválida. Opciones: standing, sneaking, sitting, sleeping, swimming");
                    }
                } else {
                    player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
                }
                break;
            }

            case "equip": {
                if (args.length < 3) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant equip <uuid|nombre|nearest> <helmet|chestplate|leggings|boots|mainhand|offhand|clear>");
                    return true;
                }
                handleEquip(player, args);
                break;
            }

            case "glow": {
                if (args.length < 3) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant glow <uuid|nombre|nearest> <color|off>");
                    return true;
                }
                Optional<NPCEntity> target = npcManager.resolveNPC(args[1], player);
                if (target.isPresent()) {
                    String color = args[2].equalsIgnoreCase("off") ? null : args[2].toUpperCase();
                    target.get().setGlowColor(color);
                    npcManager.saveNPC(target.get());
                    player.sendMessage(getPrefix() + ChatColor.GREEN + (color == null ? "Glow desactivado." : "Glow activado en " + color + "."));
                } else {
                    player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
                }
                break;
            }

            case "invisible":
            case "invis": {
                if (args.length < 2) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant invisible <uuid|nombre|nearest>");
                    return true;
                }
                Optional<NPCEntity> target = npcManager.resolveNPC(args[1], player);
                if (target.isPresent()) {
                    NPCEntity iNpc = target.get();
                    iNpc.setInvisible(!iNpc.isInvisible());
                    npcManager.saveNPC(iNpc);
                    player.sendMessage(getPrefix() + ChatColor.GREEN + "Invisibilidad: " + (iNpc.isInvisible() ? "activada" : "desactivada") + ".");
                } else {
                    player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
                }
                break;
            }

            case "look": {
                if (args.length < 2) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant look <uuid|nombre|nearest>");
                    return true;
                }
                Optional<NPCEntity> target = npcManager.resolveNPC(args[1], player);
                if (target.isPresent()) {
                    NPCEntity lNpc = target.get();
                    lNpc.setLookAtPlayer(!lNpc.isLookAtPlayer());
                    npcManager.saveNPC(lNpc);
                    player.sendMessage(getPrefix() + ChatColor.GREEN + "Mirar al jugador: " + (lNpc.isLookAtPlayer() ? "activado" : "desactivado") + ".");
                } else {
                    player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
                }
                break;
            }

            case "nametag": {
                if (args.length < 2) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant nametag <uuid|nombre|nearest>");
                    return true;
                }
                Optional<NPCEntity> target = npcManager.resolveNPC(args[1], player);
                if (target.isPresent()) {
                    NPCEntity nNpc = target.get();
                    nNpc.setShowNametag(!nNpc.isShowNametag());
                    npcManager.saveNPC(nNpc);
                    player.sendMessage(getPrefix() + ChatColor.GREEN + "Nametag del NPC: " + (nNpc.isShowNametag() ? "visible" : "oculto") + ".");
                } else {
                    player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
                }
                break;
            }

            case "animate": {
                if (args.length < 2) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant animate <uuid|nombre|nearest> [swing|crit|magic|damage]");
                    return true;
                }
                Optional<NPCEntity> target = npcManager.resolveNPC(args[1], player);
                if (target.isEmpty()) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
                    return true;
                }
                String anim = args.length >= 3 ? args[2].toLowerCase() : "swing";
                var animTypes = com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation.EntityAnimationType.values();
                var peAnim = switch (anim) {
                    case "offhand" -> com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFF_HAND;
                    case "damage", "hurt" -> com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation.EntityAnimationType.HURT;
                    case "crit"    -> animTypes.length > 4 ? animTypes[4] : com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM;
                    case "magic"   -> animTypes.length > 5 ? animTypes[5] : com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM;
                    default        -> com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM;
                };
                target.get().getFakePlayer().playAnimation(peAnim);
                player.sendMessage(getPrefix() + ChatColor.GREEN + "Animación '" + anim + "' ejecutada.");
                break;
            }

            case "action": {
                if (args.length < 3) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant action <uuid|nombre|nearest> <add|remove|list|clear> [tipo] [valor]");
                    return true;
                }
                handleAction(player, args);
                break;
            }

            case "visible": {
                if (args.length < 3) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant visible <uuid|nombre|nearest> <all|only|add|remove> [jugador]");
                    return true;
                }
                handleVisible(player, args);
                break;
            }

            case "info": {
                String idArg = args.length >= 2 ? args[1] : "nearest";
                Optional<NPCEntity> target = npcManager.resolveNPC(idArg, player);
                if (target.isEmpty()) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
                    return true;
                }
                NPCEntity npc = target.get();
                Location loc = npc.getLocation();
                player.sendMessage(ChatColor.GOLD + "════ Información del NPC ════");
                player.sendMessage(ChatColor.YELLOW + "Nombre: " + ChatColor.WHITE + npc.getName());
                player.sendMessage(ChatColor.YELLOW + "UUID: " + ChatColor.GRAY + npc.getId());
                player.sendMessage(ChatColor.YELLOW + "Entity ID: " + ChatColor.GRAY + npc.getFakePlayer().getEntityId());
                player.sendMessage(ChatColor.YELLOW + "Tipo: " + ChatColor.WHITE + npc.getType().name());
                player.sendMessage(ChatColor.YELLOW + "Pose: " + ChatColor.WHITE + npc.getPose().name());
                player.sendMessage(ChatColor.YELLOW + "Mundo: " + ChatColor.WHITE + (loc.getWorld() != null ? loc.getWorld().getName() : "N/A")
                    + String.format(" (%.1f, %.1f, %.1f)", loc.getX(), loc.getY(), loc.getZ()));
                player.sendMessage(ChatColor.YELLOW + "Acciones: " + ChatColor.WHITE + npc.getActionChain().size());
                player.sendMessage(ChatColor.YELLOW + "Líneas de holograma: " + ChatColor.WHITE + npc.getHologramLines().size());
                break;
            }

            case "reload": {
                plugin.reloadConfig();
                plugin.getConfigManager().load();
                plugin.getSkinManager().reload();
                player.sendMessage(getPrefix() + ChatColor.GREEN + "Configuración y skins recargadas.");
                break;
            }

            case "list": {
                var allNpcs = npcManager.getAllNPCs();
                if (allNpcs.isEmpty()) {
                    player.sendMessage(getPrefix() + ChatColor.YELLOW + "No hay NPCs registrados.");
                } else {
                    player.sendMessage(getPrefix() + ChatColor.GOLD + "NPCs activos (" + allNpcs.size() + "):");
                    for (var n : allNpcs) {
                        NPCEntity ne = (NPCEntity) n;
                        Location l = ne.getLocation();
                        player.sendMessage(ChatColor.GRAY + " · " + ChatColor.WHITE + ne.getName()
                            + ChatColor.YELLOW + " (" + ne.getType().name() + ") "
                            + ChatColor.DARK_GRAY + "[" + ne.getId().toString().substring(0, 8) + "...] "
                            + ChatColor.GRAY + String.format("(%.0f, %.0f, %.0f)", l.getX(), l.getY(), l.getZ()));
                    }
                }
                break;
            }

            default:
                sendHelp(player);
                break;
        }
        return true;
    }

    private void handleEquip(Player player, String[] args) {
        Optional<NPCEntity> target = npcManager.resolveNPC(args[1], player);
        if (target.isEmpty()) {
            player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
            return;
        }
        NPCEntity npc = target.get();
        String slotStr = args[2].toLowerCase();

        if (slotStr.equals("clear")) {
            npc.setEquipment(new NPCEquipment());
            npcManager.saveNPC(npc);
            player.sendMessage(getPrefix() + ChatColor.GREEN + "Equipamiento del NPC limpiado.");
            return;
        }

        ItemStack handItem = player.getInventory().getItemInMainHand();
        ItemStack toEquip = (handItem == null || handItem.getType() == Material.AIR) ? null : handItem.clone();

        NPCEquipment eq = npc.getEquipment();
        switch (slotStr) {
            case "helmet", "head", "casco" -> eq.helmet(toEquip);
            case "chestplate", "chest", "pechera" -> eq.chestplate(toEquip);
            case "leggings", "legs", "pantalones" -> eq.leggings(toEquip);
            case "boots", "feet", "botas" -> eq.boots(toEquip);
            case "mainhand", "hand", "mano" -> eq.mainHand(toEquip);
            case "offhand", "secundaria" -> eq.offHand(toEquip);
            default -> {
                player.sendMessage(getPrefix() + ChatColor.RED + "Slot inválido. Usa: helmet, chestplate, leggings, boots, mainhand, offhand, clear");
                return;
            }
        }
        npc.updateEquipment();
        npcManager.saveNPC(npc);
        player.sendMessage(getPrefix() + ChatColor.GREEN + "Equipamiento actualizado en slot " + slotStr + ".");
    }

    private void handleAction(Player player, String[] args) {
        Optional<NPCEntity> target = npcManager.resolveNPC(args[1], player);
        if (target.isEmpty()) {
            player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
            return;
        }
        NPCEntity npc = target.get();
        String sub = args[2].toLowerCase();

        switch (sub) {
            case "list" -> {
                List<INPCAction> actions = npc.getActionChain().getActions();
                if (actions.isEmpty()) {
                    player.sendMessage(getPrefix() + ChatColor.YELLOW + "Este NPC no tiene acciones configuradas.");
                    return;
                }
                player.sendMessage(getPrefix() + ChatColor.GOLD + "Acciones del NPC (" + actions.size() + "):");
                for (int i = 0; i < actions.size(); i++) {
                    INPCAction a = actions.get(i);
                    player.sendMessage(ChatColor.GRAY + "  " + i + ": " + ChatColor.AQUA + a.getType()
                        + ChatColor.WHITE + " -> " + a.serialize());
                }
            }
            case "clear" -> {
                npc.getActionChain().clear();
                npcManager.saveNPC(npc);
                player.sendMessage(getPrefix() + ChatColor.GREEN + "Todas las acciones del NPC fueron eliminadas.");
            }
            case "remove" -> {
                if (args.length < 4) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant action <npc> remove <índice>");
                    return;
                }
                try {
                    int idx = Integer.parseInt(args[3]);
                    npc.getActionChain().removeAction(idx);
                    npcManager.saveNPC(npc);
                    player.sendMessage(getPrefix() + ChatColor.GREEN + "Acción #" + idx + " eliminada.");
                } catch (NumberFormatException e) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Índice inválido.");
                }
            }
            case "add" -> {
                if (args.length < 5) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant action <npc> add <cmd|console|opcmd|message|sound|server> <valor...>");
                    return;
                }
                String actionType = args[3].toLowerCase();
                String payload = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                INPCAction action = switch (actionType) {
                    case "cmd", "player", "player_command" -> new PlayerCommandAction(payload);
                    case "console", "console_command"      -> new ConsoleCommandAction(payload);
                    case "op", "opcmd", "op_command"       -> new OpPlayerCommandAction(payload);
                    case "msg", "message", "send_message"  -> new SendMessageAction(payload);
                    case "server", "bungee"                -> new BungeeServerAction(payload);
                    case "sound", "play_sound"             -> new PlaySoundAction(payload, 1.0f, 1.0f);
                    default -> null;
                };
                if (action == null) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Tipo de acción inválido. Usa: cmd, console, opcmd, message, sound, server");
                    return;
                }
                npc.getActionChain().addAction(action);
                npcManager.saveNPC(npc);
                player.sendMessage(getPrefix() + ChatColor.GREEN + "Acción '" + action.getType() + "' añadida al NPC.");
            }
            default -> player.sendMessage(getPrefix() + ChatColor.RED + "Subcomando inválido: add, remove, list, clear");
        }
    }

    private void handleVisible(Player player, String[] args) {
        Optional<NPCEntity> target = npcManager.resolveNPC(args[1], player);
        if (target.isEmpty()) {
            player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
            return;
        }
        NPCEntity npc = target.get();
        String sub = args[2].toLowerCase();

        switch (sub) {
            case "all" -> {
                npc.setVisibleToAll(true);
                npc.getFakePlayer().spawnForAll();
                npcManager.saveNPC(npc);
                player.sendMessage(getPrefix() + ChatColor.GREEN + "El NPC ahora es visible para todos.");
            }
            case "only" -> {
                npc.setVisibleToAll(false);
                npcManager.saveNPC(npc);
                player.sendMessage(getPrefix() + ChatColor.YELLOW + "El NPC ahora solo es visible para la lista permitida.");
            }
            case "add" -> {
                if (args.length < 4) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant visible <npc> add <jugador>");
                    return;
                }
                Player p = Bukkit.getPlayer(args[3]);
                if (p == null) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Jugador no conectado.");
                    return;
                }
                npc.getVisibleTo().add(p.getUniqueId());
                npc.getFakePlayer().spawnForPlayer(p);
                npcManager.saveNPC(npc);
                player.sendMessage(getPrefix() + ChatColor.GREEN + p.getName() + " ahora puede ver el NPC.");
            }
            case "remove" -> {
                if (args.length < 4) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant visible <npc> remove <jugador>");
                    return;
                }
                Player p = Bukkit.getPlayer(args[3]);
                if (p == null) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Jugador no conectado.");
                    return;
                }
                npc.getVisibleTo().remove(p.getUniqueId());
                npc.getFakePlayer().despawnForPlayer(p);
                npcManager.saveNPC(npc);
                player.sendMessage(getPrefix() + ChatColor.GREEN + p.getName() + " ya no puede ver el NPC.");
            }
            default -> player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant visible <npc> <all|only|add|remove> [jugador]");
        }
    }

    private void handleHologram(Player player, String[] args) {
        Optional<NPCEntity> target = npcManager.resolveNPC(args[1], player);
        if (target.isEmpty()) {
            player.sendMessage(getPrefix() + ChatColor.RED + "NPC no encontrado.");
            return;
        }
        NPCEntity npc = target.get();
        String sub = args[2].toLowerCase();

        switch (sub) {
            case "add": {
                if (args.length < 4) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant hologram <npc> add <texto>");
                    return;
                }
                String addText = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                Hologram h = npcManager.attachHologram(npc.getId(), addText);
                if (h != null) player.sendMessage(getPrefix() + ChatColor.GREEN + "Línea añadida al holograma.");
                else player.sendMessage(getPrefix() + ChatColor.RED + "Error al añadir holograma.");
                break;
            }

            case "set":
            case "edit": {
                if (args.length < 5) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant hologram <npc> set <índice> <texto>");
                    return;
                }
                try {
                    int lineIdx = Integer.parseInt(args[3]);
                    String newText = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                    List<String> extraLines = new ArrayList<>(npc.getHologramLines());
                    if (lineIdx < 0 || lineIdx >= extraLines.size()) {
                        player.sendMessage(getPrefix() + ChatColor.RED + "Índice fuera de rango (0 - " + Math.max(0, extraLines.size() - 1) + ").");
                        return;
                    }
                    extraLines.set(lineIdx, newText);
                    npc.setHologramLines(extraLines);
                    npcManager.saveNPC(npc);
                    player.sendMessage(getPrefix() + ChatColor.GREEN + "Línea #" + lineIdx + " actualizada.");
                } catch (NumberFormatException e) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Número de línea inválido.");
                }
                break;
            }

            case "insert": {
                if (args.length < 5) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant hologram <npc> insert <índice> <texto>");
                    return;
                }
                try {
                    int lineIdx = Integer.parseInt(args[3]);
                    String newText = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                    List<String> extraLines = new ArrayList<>(npc.getHologramLines());
                    int safeIdx = Math.max(0, Math.min(lineIdx, extraLines.size()));
                    extraLines.add(safeIdx, newText);
                    npc.setHologramLines(extraLines);
                    npcManager.saveNPC(npc);
                    player.sendMessage(getPrefix() + ChatColor.GREEN + "Línea insertada en posición #" + safeIdx + ".");
                } catch (NumberFormatException e) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Número de línea inválido.");
                }
                break;
            }

            case "removeline":
            case "del": {
                if (args.length < 4) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Uso: /npcfant hologram <npc> removeline <índice>");
                    return;
                }
                try {
                    int lineIdx = Integer.parseInt(args[3]);
                    List<String> extraLines = new ArrayList<>(npc.getHologramLines());
                    if (lineIdx < 0 || lineIdx >= extraLines.size()) {
                        player.sendMessage(getPrefix() + ChatColor.RED + "Índice fuera de rango.");
                        return;
                    }
                    extraLines.remove(lineIdx);
                    npc.setHologramLines(extraLines);
                    npcManager.saveNPC(npc);
                    player.sendMessage(getPrefix() + ChatColor.GREEN + "Línea #" + lineIdx + " eliminada.");
                } catch (NumberFormatException e) {
                    player.sendMessage(getPrefix() + ChatColor.RED + "Número de línea inválido.");
                }
                break;
            }

            case "remove":
            case "clear": {
                npc.setHologramLines(Collections.emptyList());
                npcManager.saveNPC(npc);
                player.sendMessage(getPrefix() + ChatColor.GREEN + "Líneas adicionales del holograma eliminadas.");
                break;
            }

            case "list": {
                List<String> lines = npc.getHologramLines();
                player.sendMessage(getPrefix() + ChatColor.GOLD + "Holograma de " + ChatColor.WHITE + npc.getName() + ChatColor.GOLD + ":");
                player.sendMessage(ChatColor.GRAY + "  [Nametag]: " + (npc.isShowNametag() ? ChatColor.WHITE + npc.getName() : ChatColor.RED + "Oculto"));
                if (lines.isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "  (Sin líneas adicionales. Usa /npcfant hologram <npc> add <texto>)");
                } else {
                    for (int i = 0; i < lines.size(); i++) {
                        player.sendMessage(ChatColor.GRAY + "  " + i + ": " + ChatColor.WHITE + lines.get(i));
                    }
                }
                break;
            }

            default:
                player.sendMessage(getPrefix() + ChatColor.RED + "Subcomando inválido: add | set | insert | removeline | clear | list");
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "════ NpcFant — Comandos ════");
        player.sendMessage(ChatColor.YELLOW + "/npcfant create <tipo> <nombre>"     + ChatColor.GRAY + " — Crea un NPC");
        player.sendMessage(ChatColor.YELLOW + "/npcfant remove <npc>"               + ChatColor.GRAY + " — Elimina un NPC");
        player.sendMessage(ChatColor.YELLOW + "/npcfant clone <npc>"                + ChatColor.GRAY + " — Clona un NPC");
        player.sendMessage(ChatColor.YELLOW + "/npcfant move <npc>"                 + ChatColor.GRAY + " — Mueve el NPC a tu posición");
        player.sendMessage(ChatColor.YELLOW + "/npcfant tp <npc>"                   + ChatColor.GRAY + " — Te teletransporta al NPC");
        player.sendMessage(ChatColor.YELLOW + "/npcfant skin <npc> <tipo> <valor>"  + ChatColor.GRAY + " — Cambia la skin");
        player.sendMessage(ChatColor.YELLOW + "/npcfant name <npc> <nombre>"        + ChatColor.GRAY + " — Cambia el nombre");
        player.sendMessage(ChatColor.YELLOW + "/npcfant nametag <npc>"              + ChatColor.GRAY + " — Alterna mostrar nametag");
        player.sendMessage(ChatColor.YELLOW + "/npcfant hologram <npc> <sub>"       + ChatColor.GRAY + " — Gestiona hologramas");
        player.sendMessage(ChatColor.YELLOW + "/npcfant pose <npc> <pose>"          + ChatColor.GRAY + " — Cambia la pose");
        player.sendMessage(ChatColor.YELLOW + "/npcfant animate <npc> [anim]"       + ChatColor.GRAY + " — Reproduce una animación");
        player.sendMessage(ChatColor.YELLOW + "/npcfant equip <npc> <slot>"         + ChatColor.GRAY + " — Equipa el ítem en tu mano");
        player.sendMessage(ChatColor.YELLOW + "/npcfant glow <npc> <color|off>"     + ChatColor.GRAY + " — Activa/desactiva glow");
        player.sendMessage(ChatColor.YELLOW + "/npcfant invisible <npc>"            + ChatColor.GRAY + " — Alterna invisibilidad");
        player.sendMessage(ChatColor.YELLOW + "/npcfant look <npc>"                 + ChatColor.GRAY + " — Alterna mirar al jugador");
        player.sendMessage(ChatColor.YELLOW + "/npcfant action <npc> <sub>"         + ChatColor.GRAY + " — Gestiona acciones al clic");
        player.sendMessage(ChatColor.YELLOW + "/npcfant visible <npc> <sub>"        + ChatColor.GRAY + " — Control de visibilidad");
        player.sendMessage(ChatColor.YELLOW + "/npcfant info [npc]"                 + ChatColor.GRAY + " — Muestra detalles del NPC");
        player.sendMessage(ChatColor.YELLOW + "/npcfant list"                       + ChatColor.GRAY + " — Lista todos los NPCs");
        player.sendMessage(ChatColor.YELLOW + "/npcfant reload"                     + ChatColor.GRAY + " — Recarga la configuración");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUB_COMMANDS.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        String sub = args[0].toLowerCase();
        if (args.length == 2) {
            if (sub.equals("create")) {
                return Arrays.stream(NPCType.values())
                    .map(Enum::name)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            if (!sub.equals("list") && !sub.equals("reload")) {
                List<String> suggestions = new ArrayList<>();
                suggestions.add("nearest");
                for (var n : npcManager.getAllNPCs()) {
                    suggestions.add(ChatColor.stripColor(n.getName()));
                    suggestions.add(n.getId().toString());
                }
                return suggestions.stream()
                    .filter(s -> s != null && s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        if (args.length == 3) {
            switch (sub) {
                case "pose":
                    return Arrays.asList("standing", "sneaking", "sitting", "sleeping", "swimming");
                case "skin":
                case "setskin": {
                    List<String> opts = new ArrayList<>(Arrays.asList("png", "file", "username", "preset"));
                    plugin.getSkinManager().getLocalPngFiles().keySet().forEach(k -> opts.add(k + ".png"));
                    return opts.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                }
                case "hologram":
                case "holo":
                    return Arrays.asList("add", "set", "insert", "removeline", "clear", "list");
                case "animate":
                    return Arrays.asList("swing", "offhand", "crit", "magic", "damage");
                case "equip":
                    return Arrays.asList("helmet", "chestplate", "leggings", "boots", "mainhand", "offhand", "clear");
                case "action":
                    return Arrays.asList("add", "remove", "list", "clear");
                case "visible":
                    return Arrays.asList("all", "only", "add", "remove");
                case "glow":
                    return Arrays.asList("RED", "GOLD", "GREEN", "AQUA", "BLUE", "LIGHT_PURPLE", "WHITE", "off");
            }
        }
        if (args.length == 4 && sub.equals("action") && args[2].equalsIgnoreCase("add")) {
            return Arrays.asList("cmd", "console", "opcmd", "message", "sound", "server");
        }
        if (args.length == 4 && (sub.equals("skin") || sub.equals("setskin"))) {
            String skinMode = args[2].toLowerCase();
            if (skinMode.equals("preset")) {
                return new ArrayList<>(plugin.getSkinManager().getPresetSkins().keySet());
            } else if (skinMode.equals("file") || skinMode.equals("png")) {
                List<String> files = new ArrayList<>();
                plugin.getSkinManager().getLocalPngFiles().keySet().forEach(k -> {
                    files.add(k + ".png");
                    files.add(k);
                });
                return files.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
            } else if (skinMode.equals("username")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}
