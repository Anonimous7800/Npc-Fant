package com.example.advancednpc.modules.gui;

import com.example.advancednpc.AdvancedNPCPlugin;
import com.example.advancednpc.npc.NPCEntity;
import com.example.advancednpc.npc.NPCManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NPCGlowGUI implements Listener {

    private static final String GUI_TITLE_PREFIX = "§8§l✦ §6§lColor de Glow: §f";
    private static final int GUI_SIZE = 36;

    private final AdvancedNPCPlugin plugin;
    private final NamespacedKey keyColor;
    private final NamespacedKey keyNpcId;
    private final Map<UUID, UUID> activeNpcMap = new ConcurrentHashMap<>();

    public NPCGlowGUI(AdvancedNPCPlugin plugin) {
        this.plugin = plugin;
        this.keyColor = new NamespacedKey(plugin, "npcfant_glow_color");
        this.keyNpcId = new NamespacedKey(plugin, "npcfant_glow_npcid");
    }

    public void openGUI(Player player, NPCEntity npc) {
        if (player == null || npc == null) return;
        UUID id = npc.getId();
        activeNpcMap.put(player.getUniqueId(), id);

        String title = GUI_TITLE_PREFIX + ChatColor.stripColor(npc.getName());
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, title);

        ItemStack border = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, id, null);
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 27; i < 36; i++) inv.setItem(i, border);
        inv.setItem(9, border);
        inv.setItem(17, border);
        inv.setItem(18, border);
        inv.setItem(26, border);

        String current = npc.getGlowColor();

        inv.setItem(10, makeItem(
            Material.RED_CONCRETE,
            "§c§lRojo (RED)",
            "RED",
            id,
            Arrays.asList("§7Aura luminosa de color rojo.", "", "RED".equalsIgnoreCase(current) ? "§a✔ Color actual" : "§eClic para seleccionar")
        ));

        inv.setItem(11, makeItem(
            Material.ORANGE_CONCRETE,
            "§6§lDorado (GOLD)",
            "GOLD",
            id,
            Arrays.asList("§7Aura luminosa de color dorado/naranja.", "", "GOLD".equalsIgnoreCase(current) ? "§a✔ Color actual" : "§eClic para seleccionar")
        ));

        inv.setItem(12, makeItem(
            Material.YELLOW_CONCRETE,
            "§e§lAmarillo (YELLOW)",
            "YELLOW",
            id,
            Arrays.asList("§7Aura luminosa de color amarillo.", "", "YELLOW".equalsIgnoreCase(current) ? "§a✔ Color actual" : "§eClic para seleccionar")
        ));

        inv.setItem(13, makeItem(
            Material.LIME_CONCRETE,
            "§a§lVerde (GREEN)",
            "GREEN",
            id,
            Arrays.asList("§7Aura luminosa de color verde brillante.", "", "GREEN".equalsIgnoreCase(current) ? "§a✔ Color actual" : "§eClic para seleccionar")
        ));

        inv.setItem(14, makeItem(
            Material.LIGHT_BLUE_CONCRETE,
            "§b§lAqua (AQUA)",
            "AQUA",
            id,
            Arrays.asList("§7Aura luminosa de color celeste / cyan.", "", "AQUA".equalsIgnoreCase(current) ? "§a✔ Color actual" : "§eClic para seleccionar")
        ));

        inv.setItem(15, makeItem(
            Material.BLUE_CONCRETE,
            "§9§lAzul (BLUE)",
            "BLUE",
            id,
            Arrays.asList("§7Aura luminosa de color azul oscuro.", "", "BLUE".equalsIgnoreCase(current) ? "§a✔ Color actual" : "§eClic para seleccionar")
        ));

        inv.setItem(16, makeItem(
            Material.MAGENTA_CONCRETE,
            "§d§lMorado / Rosa (LIGHT_PURPLE)",
            "LIGHT_PURPLE",
            id,
            Arrays.asList("§7Aura luminosa de color magenta.", "", "LIGHT_PURPLE".equalsIgnoreCase(current) ? "§a✔ Color actual" : "§eClic para seleccionar")
        ));

        inv.setItem(21, makeItem(
            Material.WHITE_CONCRETE,
            "§f§lBlanco (WHITE)",
            "WHITE",
            id,
            Arrays.asList("§7Aura luminosa blanca clásica.", "", "WHITE".equalsIgnoreCase(current) ? "§a✔ Color actual" : "§eClic para seleccionar")
        ));

        inv.setItem(23, makeItem(
            Material.BARRIER,
            "§c§lDesactivar Glow",
            "OFF",
            id,
            Arrays.asList("§7Quita el efecto de aura luminosa.", "", (current == null || current.isEmpty() || current.equalsIgnoreCase("OFF")) ? "§a✔ Desactivado" : "§cClic para desactivar")
        ));

        inv.setItem(31, makeItem(
            Material.ARROW,
            "§7← Volver al Editor",
            "back",
            id,
            Collections.singletonList("§7Regresar al menú principal de configuración")
        ));

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        boolean isOurTitle = title != null && title.startsWith(GUI_TITLE_PREFIX);

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String color = meta.getPersistentDataContainer().get(keyColor, PersistentDataType.STRING);
        if (color == null && !isOurTitle) return;

        event.setCancelled(true);
        if (color == null) return;

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

        if (color.equalsIgnoreCase("back")) {
            player.closeInventory();
            plugin.getNPCEditorGUI().openEditor(player, npc);
            return;
        }

        if (color.equalsIgnoreCase("OFF")) {
            npc.setGlowColor(null);
            nm.saveNPC(npc);
            player.sendMessage("§a[NpcFant] Glow desactivado para '§e" + npc.getName() + "§a'.");
        } else {
            npc.setGlowColor(color.toUpperCase());
            nm.saveNPC(npc);
            player.sendMessage("§a[NpcFant] Glow de '§e" + npc.getName() + "§a' cambiado a: §e" + color.toUpperCase());
        }
        openGUI(player, npc);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
    }

    private ItemStack makeItem(Material material, String name, String color, UUID npcId, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            if (color != null) {
                meta.getPersistentDataContainer().set(keyColor, PersistentDataType.STRING, color);
            }
            if (npcId != null) {
                meta.getPersistentDataContainer().set(keyNpcId, PersistentDataType.STRING, npcId.toString());
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}

