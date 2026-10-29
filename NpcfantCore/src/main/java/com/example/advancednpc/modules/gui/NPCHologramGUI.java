package com.example.advancednpc.modules.gui;

import com.example.advancednpc.AdvancedNPCPlugin;
import com.example.advancednpc.modules.hologram.HologramLine;
import com.example.advancednpc.npc.NPCEntity;
import com.example.advancednpc.npc.NPCManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NPCHologramGUI implements Listener {

    private static final String GUI_TITLE_PREFIX = "§8§l✦ §6§lHologramas: §f";
    private static final String COLOR_GUI_PREFIX = "§8§l✦ §d§lColor Línea: §f";
    private static final int GUI_SIZE = 54;
    private static final int COLOR_GUI_SIZE = 36;

    private final AdvancedNPCPlugin plugin;
    private final NamespacedKey keyAction;
    private final NamespacedKey keyIndex;
    private final NamespacedKey keyColorCode;
    private final NamespacedKey keyNpcId;

    private final Map<UUID, UUID> activeNpcMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> activeLineIdxMap = new ConcurrentHashMap<>();
    private final Map<UUID, ChatPrompt> pendingChatPrompts = new ConcurrentHashMap<>();

    private record ChatPrompt(UUID npcId, PromptType type, int lineIndex) {}
    private enum PromptType { ADD_LINE, EDIT_LINE }

    public NPCHologramGUI(AdvancedNPCPlugin plugin) {
        this.plugin = plugin;
        this.keyAction = new NamespacedKey(plugin, "npcfant_holo_action");
        this.keyIndex = new NamespacedKey(plugin, "npcfant_holo_idx");
        this.keyColorCode = new NamespacedKey(plugin, "npcfant_holo_color");
        this.keyNpcId = new NamespacedKey(plugin, "npcfant_holo_npcid");
    }

    public void openGUI(Player player, NPCEntity npc) {
        if (player == null || npc == null) return;
        UUID id = npc.getId();
        activeNpcMap.put(player.getUniqueId(), id);

        String title = GUI_TITLE_PREFIX + ChatColor.stripColor(npc.getName());
        if (title.length() > 32) title = title.substring(0, 32);
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, title);

        ItemStack border = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, id, -1, null);
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        inv.setItem(9, border);
        inv.setItem(17, border);
        inv.setItem(18, border);
        inv.setItem(26, border);
        inv.setItem(27, border);
        inv.setItem(35, border);
        inv.setItem(36, border);
        inv.setItem(44, border);

        List<String> lines = npc.getHologramLines();
        int slot = 10;
        for (int i = 0; i < lines.size() && slot <= 43; i++) {
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;

            String raw = lines.get(i);
            String colored = HologramLine.colorize(raw);

            inv.setItem(slot, makeItem(
                Material.NAME_TAG,
                colored,
                "line_entry",
                id,
                i,
                Arrays.asList(
                    "§7Línea #" + (i + 1),
                    "§8Texto bruto: §f" + raw,
                    "",
                    "§eClic Izquierdo: §7Cambiar Color (Paleta)",
                    "§bClic Derecho: §7Editar texto por Chat",
                    "§cShift + Clic: §7Eliminar línea"
                )
            ));
            slot++;
        }

        if (lines.isEmpty()) {
            inv.setItem(22, makeItem(
                Material.PAPER,
                "§7§o(Sin líneas adicionales)",
                null,
                id,
                -1,
                Arrays.asList(
                    "§7Este NPC actualmente solo muestra",
                    "§7su nametag principal.",
                    "",
                    "§aHaz clic en '+ Añadir Línea' abajo",
                    "§apara agregar textos flotantes."
                )
            ));
        }

        inv.setItem(47, makeItem(
            Material.ANVIL,
            "§a§l＋ Añadir Nueva Línea",
            "add_line",
            id,
            -1,
            Arrays.asList(
                "§7Añade un nuevo texto flotante",
                "§7encima del NPC.",
                "",
                "§eClic para escribir por chat",
                "§8Soporta códigos de color '&'"
            )
        ));

        boolean showNametag = npc.isShowNametag();
        inv.setItem(49, makeItem(
            showNametag ? Material.ENDER_EYE : Material.ENDER_PEARL,
            "§e§lNametag Principal",
            "toggle_nametag",
            id,
            -1,
            Arrays.asList(
                "§7Nombre base del NPC: " + (showNametag ? "§aVisible" : "§cOculto"),
                "",
                "§eClic para alternar visibilidad"
            )
        ));

        if (!lines.isEmpty()) {
            inv.setItem(51, makeItem(
                Material.LAVA_BUCKET,
                "§c§lLimpiar Líneas",
                "clear_lines",
                id,
                -1,
                Arrays.asList(
                    "§4Borra todas las líneas flotantes del NPC.",
                    "",
                    "§eClic para limpiar"
                )
            ));
        }

        inv.setItem(45, makeItem(
            Material.ARROW,
            "§c← Volver al Editor",
            "back",
            id,
            -1,
            Collections.singletonList("§7Regresar al menú principal de configuración")
        ));

        player.openInventory(inv);
    }

    public void openColorPicker(Player player, NPCEntity npc, int lineIndex) {
        if (player == null || npc == null) return;
        UUID id = npc.getId();
        activeNpcMap.put(player.getUniqueId(), id);
        activeLineIdxMap.put(player.getUniqueId(), lineIndex);

        String title = COLOR_GUI_PREFIX + "#" + (lineIndex + 1);
        if (title.length() > 32) title = title.substring(0, 32);
        Inventory inv = Bukkit.createInventory(null, COLOR_GUI_SIZE, title);

        ItemStack border = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, id, -1, null);
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 27; i < 36; i++) inv.setItem(i, border);
        inv.setItem(9, border);
        inv.setItem(17, border);
        inv.setItem(18, border);
        inv.setItem(26, border);

        inv.setItem(10, makeColorItem(Material.RED_CONCRETE, "§c§lRojo Claro", "&c", id));
        inv.setItem(11, makeColorItem(Material.RED_TERRACOTTA, "§4§lRojo Oscuro", "&4", id));
        inv.setItem(12, makeColorItem(Material.ORANGE_CONCRETE, "§6§lDorado", "&6", id));
        inv.setItem(13, makeColorItem(Material.YELLOW_CONCRETE, "§e§lAmarillo", "&e", id));
        inv.setItem(14, makeColorItem(Material.LIME_CONCRETE, "§a§lVerde Lima", "&a", id));
        inv.setItem(15, makeColorItem(Material.GREEN_CONCRETE, "§2§lVerde Oscuro", "&2", id));
        inv.setItem(16, makeColorItem(Material.LIGHT_BLUE_CONCRETE, "§b§lAqua", "&b", id));

        inv.setItem(19, makeColorItem(Material.CYAN_CONCRETE, "§3§lCyan", "&3", id));
        inv.setItem(20, makeColorItem(Material.BLUE_CONCRETE, "§9§lAzul Claro", "&9", id));
        inv.setItem(21, makeColorItem(Material.BLUE_TERRACOTTA, "§1§lAzul Oscuro", "&1", id));
        inv.setItem(22, makeColorItem(Material.PINK_CONCRETE, "§d§lRosa / Magenta", "&d", id));
        inv.setItem(23, makeColorItem(Material.PURPLE_CONCRETE, "§5§lMorado", "&5", id));
        inv.setItem(24, makeColorItem(Material.WHITE_CONCRETE, "§f§lBlanco", "&f", id));
        inv.setItem(25, makeColorItem(Material.LIGHT_GRAY_CONCRETE, "§7§lGris Claro", "&7", id));

        inv.setItem(31, makeItem(
            Material.ARROW,
            "§7← Volver a Hologramas",
            "back_to_holo",
            id,
            -1,
            Collections.singletonList("§7Regresar a la lista de líneas")
        ));

        player.openInventory(inv);
    }

    private ItemStack makeColorItem(Material mat, String name, String code, UUID npcId) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name + " (" + code + ")");
            meta.setLore(Arrays.asList(
                "§7Aplica este color a la línea.",
                "",
                "§eClic para seleccionar"
            ));
            meta.getPersistentDataContainer().set(keyColorCode, PersistentDataType.STRING, code);
            if (npcId != null) {
                meta.getPersistentDataContainer().set(keyNpcId, PersistentDataType.STRING, npcId.toString());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeItem(Material mat, String name, String action, UUID npcId, int index, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            if (action != null) {
                meta.getPersistentDataContainer().set(keyAction, PersistentDataType.STRING, action);
            }
            if (npcId != null) {
                meta.getPersistentDataContainer().set(keyNpcId, PersistentDataType.STRING, npcId.toString());
            }
            if (index >= 0) {
                meta.getPersistentDataContainer().set(keyIndex, PersistentDataType.INTEGER, index);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        boolean isHoloGUI = title != null && title.startsWith(GUI_TITLE_PREFIX);
        boolean isColorGUI = title != null && title.startsWith(COLOR_GUI_PREFIX);
        if (!isHoloGUI && !isColorGUI) return;

        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        UUID npcId = activeNpcMap.get(player.getUniqueId());
        String stampedId = meta.getPersistentDataContainer().get(keyNpcId, PersistentDataType.STRING);
        if (stampedId != null) {
            try {
                npcId = UUID.fromString(stampedId);
                activeNpcMap.put(player.getUniqueId(), npcId);
            } catch (Exception ignored) {}
        }

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

        if (isColorGUI) {
            String colorCode = meta.getPersistentDataContainer().get(keyColorCode, PersistentDataType.STRING);
            String action = meta.getPersistentDataContainer().get(keyAction, PersistentDataType.STRING);

            if ("back_to_holo".equals(action)) {
                openGUI(player, npc);
                return;
            }

            if (colorCode != null) {
                Integer lineIdx = activeLineIdxMap.get(player.getUniqueId());
                if (lineIdx != null && lineIdx >= 0 && lineIdx < npc.getHologramLines().size()) {
                    List<String> lines = new ArrayList<>(npc.getHologramLines());
                    String current = lines.get(lineIdx);
                    String stripped = ChatColor.stripColor(HologramLine.colorize(current));
                    lines.set(lineIdx, colorCode + stripped);
                    npc.setHologramLines(lines);
                    nm.syncHologram(npc);
                    nm.saveNPC(npc);
                    player.sendMessage("§a[NpcFant] Color de la línea #" + (lineIdx + 1) + " actualizado a " + colorCode + "este color§a.");
                }
                openGUI(player, npc);
            }
            return;
        }

        String action = meta.getPersistentDataContainer().get(keyAction, PersistentDataType.STRING);
        if (action == null) return;

        switch (action) {
            case "back" -> {
                if (plugin.getNpcManager().getEditorGUI() != null) {
                    plugin.getNpcManager().getEditorGUI().openEditor(player, npc);
                } else {
                    player.closeInventory();
                }
            }
            case "toggle_nametag" -> {
                npc.setShowNametag(!npc.isShowNametag());
                nm.syncHologram(npc);
                nm.saveNPC(npc);
                openGUI(player, npc);
            }
            case "clear_lines" -> {
                npc.setHologramLines(new ArrayList<>());
                nm.syncHologram(npc);
                nm.saveNPC(npc);
                player.sendMessage("§e[NpcFant] Todas las líneas de holograma fueron eliminadas.");
                openGUI(player, npc);
            }
            case "add_line" -> {
                pendingChatPrompts.put(player.getUniqueId(), new ChatPrompt(npc.getId(), PromptType.ADD_LINE, -1));
                player.closeInventory();
                player.sendMessage("");
                player.sendMessage("§6§l✦ NpcFant §8» §aEscribe en el chat la nueva línea para el holograma:");
                player.sendMessage("§7(Soporta códigos '&', ej: §e&6&lServidor &bSurvival§7. Escribe §ccancelar §7para abortar)");
                player.sendMessage("");
            }
            case "line_entry" -> {
                Integer idx = meta.getPersistentDataContainer().get(keyIndex, PersistentDataType.INTEGER);
                if (idx == null || idx < 0 || idx >= npc.getHologramLines().size()) return;

                if (event.isShiftClick()) {
                    List<String> lines = new ArrayList<>(npc.getHologramLines());
                    lines.remove((int) idx);
                    npc.setHologramLines(lines);
                    nm.syncHologram(npc);
                    nm.saveNPC(npc);
                    player.sendMessage("§e[NpcFant] Línea #" + (idx + 1) + " eliminada.");
                    openGUI(player, npc);
                } else if (event.getClick() == ClickType.RIGHT) {
                    pendingChatPrompts.put(player.getUniqueId(), new ChatPrompt(npc.getId(), PromptType.EDIT_LINE, idx));
                    player.closeInventory();
                    player.sendMessage("");
                    player.sendMessage("§6§l✦ NpcFant §8» §bEscribe el nuevo texto para la línea #" + (idx + 1) + ":");
                    player.sendMessage("§7(Texto actual: §f" + npc.getHologramLines().get(idx) + "§7. Escribe §ccancelar §7para abortar)");
                    player.sendMessage("");
                } else {
                    openColorPicker(player, npc, idx);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ChatPrompt prompt = pendingChatPrompts.remove(player.getUniqueId());
        if (prompt == null) return;

        event.setCancelled(true);
        String msg = event.getMessage().trim();

        if (msg.equalsIgnoreCase("cancelar") || msg.equalsIgnoreCase("cancel")) {
            player.sendMessage("§c[NpcFant] Edición de holograma cancelada.");
            Bukkit.getScheduler().runTask(plugin, () -> {
                NPCEntity npc = (NPCEntity) plugin.getNpcManager().getNPC(prompt.npcId());
                if (npc != null) openGUI(player, npc);
            });
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            NPCEntity npc = (NPCEntity) plugin.getNpcManager().getNPC(prompt.npcId());
            if (npc == null) {
                player.sendMessage("§c[NpcFant] El NPC ya no existe.");
                return;
            }

            List<String> lines = new ArrayList<>(npc.getHologramLines());
            if (prompt.type() == PromptType.ADD_LINE) {
                lines.add(msg);
                player.sendMessage("§a[NpcFant] Nueva línea añadida al holograma: §r" + HologramLine.colorize(msg));
            } else if (prompt.type() == PromptType.EDIT_LINE) {
                int idx = prompt.lineIndex();
                if (idx >= 0 && idx < lines.size()) {
                    lines.set(idx, msg);
                    player.sendMessage("§a[NpcFant] Línea #" + (idx + 1) + " actualizada a: §r" + HologramLine.colorize(msg));
                }
            }

            npc.setHologramLines(lines);
            plugin.getNpcManager().syncHologram(npc);
            plugin.getNpcManager().saveNPC(npc);
            openGUI(player, npc);
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (title != null && (title.startsWith(GUI_TITLE_PREFIX) || title.startsWith(COLOR_GUI_PREFIX))) {
            if (!pendingChatPrompts.containsKey(player.getUniqueId())) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    String currentTitle = player.getOpenInventory().getTitle();
                    if (currentTitle == null || (!currentTitle.startsWith(GUI_TITLE_PREFIX) && !currentTitle.startsWith(COLOR_GUI_PREFIX))) {
                        activeLineIdxMap.remove(player.getUniqueId());
                    }
                }, 20L);
            }
        }
    }
}
