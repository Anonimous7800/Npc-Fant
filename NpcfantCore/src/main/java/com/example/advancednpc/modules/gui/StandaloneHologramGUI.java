package com.example.advancednpc.modules.gui;

import com.example.advancednpc.AdvancedNPCPlugin;
import com.example.advancednpc.modules.hologram.Hologram;
import com.example.advancednpc.modules.hologram.HologramLine;
import com.example.advancednpc.modules.hologram.HologramManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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

public class StandaloneHologramGUI implements Listener {

    private static final String LIST_TITLE = "§8§l✦ §6§lHologramas del Mapa";
    private static final String EDIT_PREFIX = "§8§l✦ §6§lHolo: §f";
    private static final String COLOR_PREFIX = "§8§l✦ §d§lColor Línea: §f";
    private static final int GUI_SIZE = 54;
    private static final int COLOR_SIZE = 36;

    private final AdvancedNPCPlugin plugin;
    private final HologramManager hologramManager;
    private final NamespacedKey keyAction;
    private final NamespacedKey keyHoloName;
    private final NamespacedKey keyLineIndex;
    private final NamespacedKey keyColorCode;

    private final Map<UUID, String> activeEditingHolo = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> activeEditingLine = new ConcurrentHashMap<>();
    private final Map<UUID, ChatPrompt> pendingPrompts = new ConcurrentHashMap<>();

    private record ChatPrompt(PromptType type, String holoName, int lineIndex) {}
    private enum PromptType { CREATE_HOLO, ADD_LINE, EDIT_LINE }

    public StandaloneHologramGUI(AdvancedNPCPlugin plugin, HologramManager hologramManager) {
        this.plugin = plugin;
        this.hologramManager = hologramManager;
        this.keyAction = new NamespacedKey(plugin, "standalone_holo_action");
        this.keyHoloName = new NamespacedKey(plugin, "standalone_holo_name");
        this.keyLineIndex = new NamespacedKey(plugin, "standalone_holo_idx");
        this.keyColorCode = new NamespacedKey(plugin, "standalone_holo_color");
    }

    public void openHologramList(Player player) {
        if (player == null || !player.isOnline()) return;

        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, LIST_TITLE);

        ItemStack border = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, null, -1, null);
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

        Map<String, Hologram> named = hologramManager.getNamedHolograms();
        List<Map.Entry<String, Hologram>> list = new ArrayList<>(named.entrySet());
        Location pLoc = player.getLocation();

        list.sort(Comparator.comparingDouble(entry -> {
            Location loc = entry.getValue().getLocation();
            if (loc == null || loc.getWorld() == null || !loc.getWorld().equals(pLoc.getWorld())) {
                return 999999.0;
            }
            return loc.distanceSquared(pLoc);
        }));

        int slot = 10;
        for (Map.Entry<String, Hologram> entry : list) {
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;

            String name = entry.getKey();
            Hologram h = entry.getValue();
            Location loc = h.getLocation();

            String distStr = "Mundo diferente";
            if (loc != null && loc.getWorld() != null && loc.getWorld().equals(pLoc.getWorld())) {
                distStr = String.format("%.1f bloques", loc.distance(pLoc));
            }

            List<String> lore = new ArrayList<>();
            lore.add("§7Líneas actuales:");
            List<String> lines = h.getLines();
            if (lines.isEmpty()) {
                lore.add("  §8(Sin líneas de texto)");
            } else {
                for (int i = 0; i < Math.min(lines.size(), 4); i++) {
                    lore.add("  §f" + (i + 1) + ". §r" + HologramLine.colorize(lines.get(i)));
                }
                if (lines.size() > 4) {
                    lore.add("  §8... y " + (lines.size() - 4) + " línea(s) más");
                }
            }
            lore.add("");
            lore.add("§7Ubicación: §b" + (loc.getWorld() != null ? loc.getWorld().getName() : "world")
                + String.format(" (%.0f, %.0f, %.0f)", loc.getX(), loc.getY(), loc.getZ()));
            lore.add("§7Distancia a ti: §a" + distStr);
            lore.add("");
            lore.add("§a▶ Clic Izquierdo: §7Editar líneas y colores");
            lore.add("§b▶ Clic Derecho: §7Teletransportarme aquí");
            lore.add("§e▶ Shift + Clic Izq: §7Mover a mi posición");
            lore.add("§c▶ Shift + Clic Der: §7Eliminar holograma");

            inv.setItem(slot, makeItem(
                Material.OAK_SIGN,
                "§e§l✦ Holograma: §f" + name.toUpperCase(),
                "select_holo",
                name,
                -1,
                lore
            ));
            slot++;
        }

        if (named.isEmpty()) {
            inv.setItem(22, makeItem(
                Material.BARRIER,
                "§c§lNo hay hologramas en el mapa",
                null,
                null,
                -1,
                Arrays.asList(
                    "§7Aún no has creado hologramas independientes.",
                    "",
                    "§aHaz clic en '+ Crear Nuevo Holograma' abajo",
                    "§7o usa: §f/holo create <nombre> [texto]"
                )
            ));
        }

        inv.setItem(48, makeItem(
            Material.ANVIL,
            "§a§l＋ Crear Nuevo Holograma",
            "create_holo",
            null,
            -1,
            Arrays.asList(
                "§7Crea un holograma en tu ubicación actual.",
                "",
                "§eClic para escribir el nombre por chat"
            )
        ));

        inv.setItem(50, makeItem(
            Material.RED_STAINED_GLASS_PANE,
            "§c✕ Cerrar",
            "close",
            null,
            -1,
            Collections.singletonList("§7Cerrar este menú")
        ));

        player.openInventory(inv);
    }

    public void openEditor(Player player, String holoName) {
        if (player == null || !player.isOnline() || holoName == null) return;
        Hologram h = hologramManager.getNamedHologram(holoName).orElse(null);
        if (h == null) {
            player.sendMessage("§c[NpcFant] El holograma '" + holoName + "' ya no existe.");
            openHologramList(player);
            return;
        }

        activeEditingHolo.put(player.getUniqueId(), holoName);

        String title = EDIT_PREFIX + holoName.toUpperCase();
        if (title.length() > 32) title = title.substring(0, 32);
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, title);

        ItemStack border = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, null, -1, null);
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

        List<String> lines = h.getLines();
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
                holoName,
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
                "§7§o(Holograma vacío)",
                null,
                null,
                -1,
                Arrays.asList(
                    "§7Este holograma no tiene líneas de texto.",
                    "",
                    "§aHaz clic en '+ Añadir Línea' abajo",
                    "§apara escribir un texto flotante."
                )
            ));
        }

        inv.setItem(47, makeItem(
            Material.ANVIL,
            "§a§l＋ Añadir Línea",
            "add_line",
            holoName,
            -1,
            Arrays.asList(
                "§7Añade un nuevo renglón de texto.",
                "",
                "§eClic para escribir por chat",
                "§8Soporta códigos de color '&'"
            )
        ));

        inv.setItem(49, makeItem(
            Material.COMPASS,
            "§e§lMover a mi posición",
            "move_here",
            holoName,
            -1,
            Arrays.asList(
                "§7Mueve este holograma a donde estás parado.",
                "",
                "§eClic para teletransportarlo aquí"
            )
        ));

        inv.setItem(51, makeItem(
            Material.ENDER_PEARL,
            "§b§lTeletransportarme al Holograma",
            "tp_to",
            holoName,
            -1,
            Arrays.asList(
                "§7Te teletransporta a la posición del holograma.",
                "",
                "§eClic para ir allá"
            )
        ));

        inv.setItem(45, makeItem(
            Material.ARROW,
            "§c← Volver a la Lista",
            "back_to_list",
            null,
            -1,
            Collections.singletonList("§7Regresar a la lista de hologramas del mapa")
        ));

        inv.setItem(53, makeItem(
            Material.BARRIER,
            "§4§lEliminar Holograma",
            "delete_holo",
            holoName,
            -1,
            Arrays.asList(
                "§4¡Atención! Se eliminará este holograma.",
                "",
                "§eClic para eliminarlo"
            )
        ));

        player.openInventory(inv);
    }

    public void openColorPicker(Player player, String holoName, int lineIndex) {
        if (player == null || !player.isOnline()) return;
        activeEditingHolo.put(player.getUniqueId(), holoName);
        activeEditingLine.put(player.getUniqueId(), lineIndex);

        String title = COLOR_PREFIX + "#" + (lineIndex + 1);
        if (title.length() > 32) title = title.substring(0, 32);
        Inventory inv = Bukkit.createInventory(null, COLOR_SIZE, title);

        ItemStack border = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, null, -1, null);
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 27; i < 36; i++) inv.setItem(i, border);
        inv.setItem(9, border);
        inv.setItem(17, border);
        inv.setItem(18, border);
        inv.setItem(26, border);

        inv.setItem(10, makeColorItem(Material.RED_CONCRETE, "§c§lRojo Claro", "&c"));
        inv.setItem(11, makeColorItem(Material.RED_TERRACOTTA, "§4§lRojo Oscuro", "&4"));
        inv.setItem(12, makeColorItem(Material.ORANGE_CONCRETE, "§6§lDorado", "&6"));
        inv.setItem(13, makeColorItem(Material.YELLOW_CONCRETE, "§e§lAmarillo", "&e"));
        inv.setItem(14, makeColorItem(Material.LIME_CONCRETE, "§a§lVerde Lima", "&a"));
        inv.setItem(15, makeColorItem(Material.GREEN_CONCRETE, "§2§lVerde Oscuro", "&2"));
        inv.setItem(16, makeColorItem(Material.LIGHT_BLUE_CONCRETE, "§b§lAqua", "&b"));

        inv.setItem(19, makeColorItem(Material.CYAN_CONCRETE, "§3§lCyan", "&3"));
        inv.setItem(20, makeColorItem(Material.BLUE_CONCRETE, "§9§lAzul Claro", "&9"));
        inv.setItem(21, makeColorItem(Material.BLUE_TERRACOTTA, "§1§lAzul Oscuro", "&1"));
        inv.setItem(22, makeColorItem(Material.PINK_CONCRETE, "§d§lRosa / Magenta", "&d"));
        inv.setItem(23, makeColorItem(Material.PURPLE_CONCRETE, "§5§lMorado", "&5"));
        inv.setItem(24, makeColorItem(Material.WHITE_CONCRETE, "§f§lBlanco", "&f"));
        inv.setItem(25, makeColorItem(Material.LIGHT_GRAY_CONCRETE, "§7§lGris Claro", "&7"));

        inv.setItem(31, makeItem(
            Material.ARROW,
            "§7← Volver al Holograma",
            "back_to_editor",
            holoName,
            -1,
            Collections.singletonList("§7Regresar al editor del holograma")
        ));

        player.openInventory(inv);
    }

    private ItemStack makeColorItem(Material mat, String name, String code) {
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
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeItem(Material mat, String name, String action, String holoName, int lineIndex, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            if (action != null) {
                meta.getPersistentDataContainer().set(keyAction, PersistentDataType.STRING, action);
            }
            if (holoName != null) {
                meta.getPersistentDataContainer().set(keyHoloName, PersistentDataType.STRING, holoName);
            }
            if (lineIndex >= 0) {
                meta.getPersistentDataContainer().set(keyLineIndex, PersistentDataType.INTEGER, lineIndex);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        boolean isList = LIST_TITLE.equals(title);
        boolean isEdit = title != null && title.startsWith(EDIT_PREFIX);
        boolean isColor = title != null && title.startsWith(COLOR_PREFIX);
        if (!isList && !isEdit && !isColor) return;

        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (isColor) {
            String holoName = activeEditingHolo.get(player.getUniqueId());
            Integer lineIdx = activeEditingLine.get(player.getUniqueId());
            String action = meta.getPersistentDataContainer().get(keyAction, PersistentDataType.STRING);
            if ("back_to_editor".equals(action)) {
                openEditor(player, holoName);
                return;
            }

            String code = meta.getPersistentDataContainer().get(keyColorCode, PersistentDataType.STRING);
            if (code != null && holoName != null && lineIdx != null) {
                Hologram h = hologramManager.getNamedHologram(holoName).orElse(null);
                if (h != null && lineIdx >= 0 && lineIdx < h.getLines().size()) {
                    List<String> lines = new ArrayList<>(h.getLines());
                    String raw = lines.get(lineIdx);
                    String stripped = ChatColor.stripColor(HologramLine.colorize(raw));
                    lines.set(lineIdx, code + stripped);
                    h.setAllLines(lines);
                    hologramManager.saveHolograms();
                    player.sendMessage("§a[NpcFant] Color de la línea #" + (lineIdx + 1) + " cambiado a " + code + "este color§a.");
                }
                openEditor(player, holoName);
            }
            return;
        }

        String action = meta.getPersistentDataContainer().get(keyAction, PersistentDataType.STRING);
        if (action == null) return;

        String holoName = meta.getPersistentDataContainer().get(keyHoloName, PersistentDataType.STRING);

        switch (action) {
            case "close" -> player.closeInventory();

            case "create_holo" -> {
                pendingPrompts.put(player.getUniqueId(), new ChatPrompt(PromptType.CREATE_HOLO, null, -1));
                player.closeInventory();
                player.sendMessage("");
                player.sendMessage("§6§l✦ NpcFant §8» §aEscribe en el chat el nombre para el nuevo holograma:");
                player.sendMessage("§7(Sin espacios, ej: §espawn§7, §evip§7, §eregla§7. Escribe §ccancelar §7para abortar)");
                player.sendMessage("");
            }

            case "back_to_list" -> openHologramList(player);

            case "select_holo" -> {
                if (holoName == null) return;
                Hologram h = hologramManager.getNamedHologram(holoName).orElse(null);
                if (h == null) {
                    player.sendMessage("§c[NpcFant] El holograma ya no existe.");
                    openHologramList(player);
                    return;
                }

                if (event.isShiftClick()) {
                    if (event.isRightClick()) {
                        hologramManager.removeNamedHologram(holoName);
                        player.sendMessage("§c[NpcFant] Holograma '" + holoName + "' eliminado.");
                        openHologramList(player);
                    } else {
                        Location newLoc = player.getLocation().clone().add(0, 1.2, 0);
                        h.move(newLoc);
                        hologramManager.saveHolograms();
                        player.sendMessage("§a[NpcFant] Holograma '" + holoName + "' movido a tu posición actual.");
                        openHologramList(player);
                    }
                } else if (event.isRightClick()) {
                    player.teleport(h.getLocation());
                    player.sendMessage("§a[NpcFant] Teletransportado al holograma '" + holoName + "'.");
                } else {
                    openEditor(player, holoName);
                }
            }

            case "add_line" -> {
                if (holoName == null) return;
                pendingPrompts.put(player.getUniqueId(), new ChatPrompt(PromptType.ADD_LINE, holoName, -1));
                player.closeInventory();
                player.sendMessage("");
                player.sendMessage("§6§l✦ NpcFant §8» §aEscribe la nueva línea de texto para el holograma '" + holoName + "':");
                player.sendMessage("§7(Soporta códigos '&', ej: §e&6&lServidor &bSurvival§7. Escribe §ccancelar §7para abortar)");
                player.sendMessage("");
            }

            case "move_here" -> {
                if (holoName == null) return;
                Hologram h = hologramManager.getNamedHologram(holoName).orElse(null);
                if (h != null) {
                    Location newLoc = player.getLocation().clone().add(0, 1.2, 0);
                    h.move(newLoc);
                    hologramManager.saveHolograms();
                    player.sendMessage("§a[NpcFant] Holograma '" + holoName + "' movido a tu posición.");
                    openEditor(player, holoName);
                }
            }

            case "tp_to" -> {
                if (holoName == null) return;
                Hologram h = hologramManager.getNamedHologram(holoName).orElse(null);
                if (h != null) {
                    player.teleport(h.getLocation());
                    player.sendMessage("§a[NpcFant] Teletransportado al holograma '" + holoName + "'.");
                }
            }

            case "delete_holo" -> {
                if (holoName == null) return;
                hologramManager.removeNamedHologram(holoName);
                player.sendMessage("§c[NpcFant] Holograma '" + holoName + "' eliminado correctamente.");
                openHologramList(player);
            }

            case "line_entry" -> {
                if (holoName == null) return;
                Integer idx = meta.getPersistentDataContainer().get(keyLineIndex, PersistentDataType.INTEGER);
                if (idx == null) return;

                Hologram h = hologramManager.getNamedHologram(holoName).orElse(null);
                if (h == null || idx < 0 || idx >= h.getLines().size()) return;

                if (event.isShiftClick()) {
                    h.removeLine(idx);
                    hologramManager.saveHolograms();
                    player.sendMessage("§e[NpcFant] Línea #" + (idx + 1) + " eliminada.");
                    openEditor(player, holoName);
                } else if (event.isRightClick()) {
                    pendingPrompts.put(player.getUniqueId(), new ChatPrompt(PromptType.EDIT_LINE, holoName, idx));
                    player.closeInventory();
                    player.sendMessage("");
                    player.sendMessage("§6§l✦ NpcFant §8» §bEscribe el nuevo texto para la línea #" + (idx + 1) + ":");
                    player.sendMessage("§7(Texto actual: §f" + h.getLines().get(idx) + "§7. Escribe §ccancelar §7para abortar)");
                    player.sendMessage("");
                } else {
                    openColorPicker(player, holoName, idx);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ChatPrompt prompt = pendingPrompts.remove(player.getUniqueId());
        if (prompt == null) return;

        event.setCancelled(true);
        String msg = event.getMessage().trim();

        if (msg.equalsIgnoreCase("cancelar") || msg.equalsIgnoreCase("cancel")) {
            player.sendMessage("§c[NpcFant] Acción cancelada.");
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (prompt.holoName() != null) {
                    openEditor(player, prompt.holoName());
                } else {
                    openHologramList(player);
                }
            });
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (prompt.type() == PromptType.CREATE_HOLO) {
                String cleanName = msg.split("\\s+")[0].toLowerCase();
                if (hologramManager.getNamedHologram(cleanName).isPresent()) {
                    player.sendMessage("§c[NpcFant] Ya existe un holograma con el nombre '" + cleanName + "'.");
                    openHologramList(player);
                    return;
                }
                Location loc = player.getLocation().clone().add(0, 1.2, 0);
                List<String> initLines = new ArrayList<>();
                initLines.add("§e§l" + cleanName.toUpperCase());
                hologramManager.createNamedHologram(cleanName, loc, initLines);
                player.sendMessage("§a[NpcFant] Holograma '" + cleanName + "' creado.");
                openEditor(player, cleanName);
                return;
            }

            Hologram h = hologramManager.getNamedHologram(prompt.holoName()).orElse(null);
            if (h == null) {
                player.sendMessage("§c[NpcFant] El holograma ya no existe.");
                openHologramList(player);
                return;
            }

            if (prompt.type() == PromptType.ADD_LINE) {
                h.addLine(msg);
                hologramManager.saveHolograms();
                player.sendMessage("§a[NpcFant] Nueva línea añadida: §r" + HologramLine.colorize(msg));
                openEditor(player, prompt.holoName());
            } else if (prompt.type() == PromptType.EDIT_LINE) {
                int idx = prompt.lineIndex();
                if (idx >= 0 && idx < h.getLines().size()) {
                    h.setLine(idx, msg);
                    hologramManager.saveHolograms();
                    player.sendMessage("§a[NpcFant] Línea #" + (idx + 1) + " actualizada a: §r" + HologramLine.colorize(msg));
                }
                openEditor(player, prompt.holoName());
            }
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (title != null && (title.startsWith(LIST_TITLE) || title.startsWith(EDIT_PREFIX) || title.startsWith(COLOR_PREFIX))) {
            if (!pendingPrompts.containsKey(player.getUniqueId())) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    String currentTitle = player.getOpenInventory().getTitle();
                    if (currentTitle == null || (!currentTitle.startsWith(LIST_TITLE) && !currentTitle.startsWith(EDIT_PREFIX) && !currentTitle.startsWith(COLOR_PREFIX))) {
                        activeEditingHolo.remove(player.getUniqueId());
                        activeEditingLine.remove(player.getUniqueId());
                    }
                }, 20L);
            }
        }
    }
}
