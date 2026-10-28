package com.example.advancednpc.modules.gui;

import com.example.advancednpc.AdvancedNPCPlugin;
import com.example.advancednpc.api.action.INPCAction;
import com.example.advancednpc.modules.interaction.ConsoleCommandAction;
import com.example.advancednpc.modules.interaction.PlayerCommandAction;
import com.example.advancednpc.modules.interaction.SendMessageAction;
import com.example.advancednpc.npc.NPCEntity;
import com.example.advancednpc.npc.NPCManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NPCActionGUI implements Listener {

    private static final String GUI_TITLE_PREFIX = "§8§l✦ §6§lAcciones del NPC: §f";
    private static final int GUI_SIZE = 45;

    private final AdvancedNPCPlugin plugin;
    private final NamespacedKey keyAction;
    private final NamespacedKey keyIndex;
    private final Map<UUID, UUID> activeNpcMap = new ConcurrentHashMap<>();
    private final Map<UUID, ChatPrompt> chatPrompts = new ConcurrentHashMap<>();

    private record ChatPrompt(UUID npcId, String actionType) {}

    public NPCActionGUI(AdvancedNPCPlugin plugin) {
        this.plugin = plugin;
        this.keyAction = new NamespacedKey(plugin, "npcfant_action_btn");
        this.keyIndex  = new NamespacedKey(plugin, "npcfant_action_idx");
    }

    public void openGUI(Player player, NPCEntity npc) {
        if (player == null || npc == null) return;
        activeNpcMap.put(player.getUniqueId(), npc.getId());

        String title = GUI_TITLE_PREFIX + ChatColor.stripColor(npc.getName());
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, title);

        ItemStack border = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, null, -1);
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 36; i < 45; i++) inv.setItem(i, border);
        inv.setItem(9, border);
        inv.setItem(17, border);
        inv.setItem(18, border);
        inv.setItem(26, border);
        inv.setItem(27, border);
        inv.setItem(35, border);

        List<INPCAction> currentActions = npc.getActionChain().getActions();
        int[] actionSlots = {10, 11, 12, 13, 14, 15, 16};
        for (int i = 0; i < actionSlots.length; i++) {
            int slot = actionSlots[i];
            if (i < currentActions.size()) {
                INPCAction act = currentActions.get(i);
                String displayValue = getActionDisplayValue(act);
                inv.setItem(slot, makeItem(
                    Material.ENCHANTED_BOOK,
                    "§eAcción #" + i + ": §6" + act.getType(),
                    "delete_action",
                    Arrays.asList(
                        "§7Tipo: §f" + act.getType(),
                        "§7Ejecuta: §e" + displayValue,
                        "",
                        "§cClic para eliminar esta acción"
                    ),
                    i
                ));
            } else if (i == currentActions.size()) {
                inv.setItem(slot, makeItem(
                    Material.BOOK,
                    "§7(Espacio disponible para acción)",
                    null,
                    Collections.singletonList("§8Usa los botones inferiores para añadir"),
                    -1
                ));
            }
        }

        inv.setItem(28, makeItem(
            Material.COMPASS,
            "§b§l+ Teletransportar Jugador",
            "add_tp",
            Arrays.asList(
                "§7Teletransporta al jugador al interactuar.",
                "",
                "§f- Clic Izquierdo: §7A tu posición actual",
                "§f- Clic Derecho: §7Escribir destino en el chat"
            ),
            -1
        ));

        inv.setItem(30, makeItem(
            Material.DIAMOND,
            "§a§l+ Dar Ítem al Jugador",
            "add_item",
            Arrays.asList(
                "§7Entrega un ítem al jugador al interactuar.",
                "",
                "§f- Clic Izquierdo: §7Copiar ítem de tu mano principal",
                "§f- Clic Derecho: §7Escribir ítem en el chat"
            ),
            -1
        ));

        inv.setItem(32, makeItem(
            Material.COMMAND_BLOCK,
            "§6§l+ Comando de Consola",
            "add_console",
            Arrays.asList(
                "§7Ejecuta un comando desde la consola.",
                "§8Variables: {player}, {uuid}",
                "",
                "§eClic para escribir comando en el chat"
            ),
            -1
        ));

        inv.setItem(34, makeItem(
            Material.WRITABLE_BOOK,
            "§d§l+ Comando como Jugador",
            "add_player_cmd",
            Arrays.asList(
                "§7El jugador ejecutará el comando.",
                "§8Ejemplo: spawn, warp tienda, etc.",
                "",
                "§eClic para escribir comando en el chat"
            ),
            -1
        ));

        inv.setItem(36, makeItem(
            Material.PAPER,
            "§e§l+ Enviar Mensaje",
            "add_msg",
            Arrays.asList(
                "§7Envía un mensaje privado al jugador.",
                "",
                "§eClic para escribir mensaje en el chat"
            ),
            -1
        ));

        inv.setItem(38, makeItem(
            Material.BARRIER,
            "§c§lLimpiar Todo",
            "clear_all",
            Collections.singletonList("§cClic para borrar todas las acciones"),
            -1
        ));

        inv.setItem(42, makeItem(
            Material.ARROW,
            "§7← Volver al Editor",
            "back",
            Collections.singletonList("§7Regresar al menú principal del NPC"),
            -1
        ));

        player.openInventory(inv);
    }

    private String getActionDisplayValue(INPCAction act) {
        if (act instanceof ConsoleCommandAction c) return "/" + c.getCommand();
        if (act instanceof PlayerCommandAction p) return "/" + p.getCommand();
        if (act instanceof SendMessageAction m) return m.getMessage();
        Map<String, Object> map = act.serialize();
        if (map.containsKey("command")) return String.valueOf(map.get("command"));
        if (map.containsKey("message")) return String.valueOf(map.get("message"));
        return act.getType();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID npcId = activeNpcMap.get(player.getUniqueId());
        String title = event.getView().getTitle();
        boolean isOurTitle = title != null && title.startsWith(GUI_TITLE_PREFIX);
        if (npcId == null && !isOurTitle) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String action = meta.getPersistentDataContainer().get(keyAction, PersistentDataType.STRING);
        if (action == null && !isOurTitle) return;

        event.setCancelled(true);
        if (action == null) return;

        if (npcId == null) {
            player.closeInventory();
            return;
        }

        NPCManager nm = plugin.getNpcManager();
        NPCEntity npc = (NPCEntity) nm.getNPC(npcId);
        if (npc == null) {
            player.sendMessage("§c[NpcFant] El NPC ya no existe.");
            player.closeInventory();
            return;
        }

        switch (action) {
            case "back" -> {
                player.closeInventory();
                plugin.getNPCEditorGUI().openEditor(player, npc);
            }

            case "clear_all" -> {
                npc.getActionChain().clear();
                nm.saveNPC(npc);
                player.sendMessage("§a[NpcFant] Todas las acciones han sido eliminadas.");
                openGUI(player, npc);
            }

            case "delete_action" -> {
                Integer idx = meta.getPersistentDataContainer().get(keyIndex, PersistentDataType.INTEGER);
                if (idx != null && idx >= 0 && idx < npc.getActionChain().size()) {
                    npc.getActionChain().removeAction(idx);
                    nm.saveNPC(npc);
                    player.sendMessage("§a[NpcFant] Acción #" + idx + " eliminada.");
                    openGUI(player, npc);
                }
            }

            case "add_tp" -> {
                if (event.isLeftClick()) {
                    Location loc = player.getLocation();
                    String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "world";
                    String tpCmd = "execute in " + worldName + " run teleport {player} "
                        + String.format(Locale.US, "%.2f %.2f %.2f %.2f %.2f",
                            loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                    npc.getActionChain().addAction(new ConsoleCommandAction(tpCmd));
                    nm.saveNPC(npc);
                    player.sendMessage("§a[NpcFant] Acción de teletransporte a tu ubicación añadida con éxito.");
                    openGUI(player, npc);
                } else {
                    player.closeInventory();
                    chatPrompts.put(player.getUniqueId(), new ChatPrompt(npcId, "tp"));
                    player.sendMessage("§e[NpcFant] Escribe en el chat las coordenadas o lugar a donde teletransportar (ej: §fworld 100 65 200§e o §fspawn§e) [o 'cancelar']:");
                }
            }

            case "add_item" -> {
                if (event.isLeftClick()) {
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand.getType().isAir()) {
                        player.sendMessage("§c[NpcFant] Sostén el ítem que deseas que el NPC entregue en tu mano principal.");
                        return;
                    }
                    String itemCmd = "give {player} " + hand.getType().name().toLowerCase() + " " + hand.getAmount();
                    npc.getActionChain().addAction(new ConsoleCommandAction(itemCmd));
                    nm.saveNPC(npc);
                    player.sendMessage("§a[NpcFant] Acción de entrega de ítem añadida (" + hand.getAmount() + "x " + hand.getType().name() + ").");
                    openGUI(player, npc);
                } else {
                    player.closeInventory();
                    chatPrompts.put(player.getUniqueId(), new ChatPrompt(npcId, "item"));
                    player.sendMessage("§e[NpcFant] Escribe en el chat el ítem y cantidad (ej: §fdiamond 5§e o §fgolden_apple 1§e) [o 'cancelar']:");
                }
            }

            case "add_console" -> {
                player.closeInventory();
                chatPrompts.put(player.getUniqueId(), new ChatPrompt(npcId, "console"));
                player.sendMessage("§e[NpcFant] Escribe el comando que ejecutará la consola (ej: §fgive {player} emerald 10§e) [o 'cancelar']:");
            }

            case "add_player_cmd" -> {
                player.closeInventory();
                chatPrompts.put(player.getUniqueId(), new ChatPrompt(npcId, "player"));
                player.sendMessage("§e[NpcFant] Escribe el comando que ejecutará el propio jugador (ej: §fwarp tienda§e o §fspawn§e) [o 'cancelar']:");
            }

            case "add_msg" -> {
                player.closeInventory();
                chatPrompts.put(player.getUniqueId(), new ChatPrompt(npcId, "message"));
                player.sendMessage("§e[NpcFant] Escribe el mensaje que recibirá el jugador en el chat [o 'cancelar']:");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ChatPrompt prompt = chatPrompts.remove(player.getUniqueId());
        if (prompt == null) return;

        event.setCancelled(true);
        String msg = event.getMessage().trim();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (msg.equalsIgnoreCase("cancelar") || msg.equalsIgnoreCase("cancel")) {
                player.sendMessage("§c[NpcFant] Operación cancelada.");
                NPCManager nm = plugin.getNpcManager();
                NPCEntity npc = (NPCEntity) nm.getNPC(prompt.npcId());
                if (npc != null) openGUI(player, npc);
                return;
            }

            NPCManager nm = plugin.getNpcManager();
            NPCEntity npc = (NPCEntity) nm.getNPC(prompt.npcId());
            if (npc == null) {
                player.sendMessage("§c[NpcFant] El NPC ya no existe.");
                return;
            }

            String clean = msg.startsWith("/") ? msg.substring(1) : msg;
            switch (prompt.actionType()) {
                case "tp" -> {
                    String cmd = clean.contains(" ") ? "teleport {player} " + clean : clean;
                    npc.getActionChain().addAction(new ConsoleCommandAction(cmd));
                    player.sendMessage("§a[NpcFant] Acción de teletransporte añadida: §e/" + cmd);
                }
                case "item" -> {
                    String cmd = "give {player} " + clean;
                    npc.getActionChain().addAction(new ConsoleCommandAction(cmd));
                    player.sendMessage("§a[NpcFant] Acción de entrega de ítem añadida: §e/" + cmd);
                }
                case "console" -> {
                    npc.getActionChain().addAction(new ConsoleCommandAction(clean));
                    player.sendMessage("§a[NpcFant] Comando de consola añadido: §e/" + clean);
                }
                case "player" -> {
                    npc.getActionChain().addAction(new PlayerCommandAction(clean));
                    player.sendMessage("§a[NpcFant] Comando de jugador añadido: §e/" + clean);
                }
                case "message" -> {
                    String formatted = ChatColor.translateAlternateColorCodes('&', msg);
                    npc.getActionChain().addAction(new SendMessageAction(formatted));
                    player.sendMessage("§a[NpcFant] Mensaje añadido.");
                }
            }

            nm.saveNPC(npc);
            openGUI(player, npc);
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        activeNpcMap.remove(event.getPlayer().getUniqueId());
    }

    private ItemStack makeItem(Material material, String name, String action, List<String> lore, int index) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            if (action != null) {
                meta.getPersistentDataContainer().set(keyAction, PersistentDataType.STRING, action);
            }
            if (index >= 0) {
                meta.getPersistentDataContainer().set(keyIndex, PersistentDataType.INTEGER, index);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}

