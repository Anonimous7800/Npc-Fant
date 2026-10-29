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

public class NPCNameColorGUI implements Listener {

    private static final String GUI_TITLE_PREFIX = "§8§l✦ §6§lColor del Nombre: §f";
    private static final int GUI_SIZE = 36;

    private final AdvancedNPCPlugin plugin;
    private final NamespacedKey keyColorCode;
    private final NamespacedKey keyNpcId;
    private final Map<UUID, UUID> activeNpcMap = new ConcurrentHashMap<>();

    public NPCNameColorGUI(AdvancedNPCPlugin plugin) {
        this.plugin = plugin;
        this.keyColorCode = new NamespacedKey(plugin, "npcfant_name_color_code");
        this.keyNpcId = new NamespacedKey(plugin, "npcfant_namecolor_npcid");
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

        inv.setItem(10, makeItem(Material.RED_CONCRETE, "§c§lRojo Claro (&c)", "&c", id, Arrays.asList("§7Aplica este color al nombre", "", "§eClic para seleccionar")));
        inv.setItem(11, makeItem(Material.RED_TERRACOTTA, "§4§lRojo Oscuro (&4)", "&4", id, Arrays.asList("§7Aplica este color al nombre", "", "§eClic para seleccionar")));
        inv.setItem(12, makeItem(Material.ORANGE_CONCRETE, "§6§lDorado (&6)", "&6", id, Arrays.asList("§7Aplica este color al nombre", "", "§eClic para seleccionar")));
        inv.setItem(13, makeItem(Material.YELLOW_CONCRETE, "§e§lAmarillo (&e)", "&e", id, Arrays.asList("§7Aplica este color al nombre", "", "§eClic para seleccionar")));
        inv.setItem(14, makeItem(Material.LIME_CONCRETE, "§a§lVerde Lima (&a)", "&a", id, Arrays.asList("§7Aplica este color al nombre", "", "§eClic para seleccionar")));
        inv.setItem(15, makeItem(Material.GREEN_CONCRETE, "§2§lVerde Oscuro (&2)", "&2", id, Arrays.asList("§7Aplica este color al nombre", "", "§eClic para seleccionar")));
        inv.setItem(16, makeItem(Material.LIGHT_BLUE_CONCRETE, "§b§lAqua (&b)", "&b", id, Arrays.asList("§7Aplica este color al nombre", "", "§eClic para seleccionar")));

        inv.setItem(19, makeItem(Material.CYAN_CONCRETE, "§3§lCyan (&3)", "&3", id, Arrays.asList("§7Aplica este color al nombre", "", "§eClic para seleccionar")));
        inv.setItem(20, makeItem(Material.BLUE_CONCRETE, "§9§lAzul Claro (&9)", "&9", id, Arrays.asList("§7Aplica este color al nombre", "", "§eClic para seleccionar")));
        inv.setItem(21, makeItem(Material.BLUE_TERRACOTTA, "§1§lAzul Oscuro (&1)", "&1", id, Arrays.asList("§7Aplica este color al nombre", "", "§eClic para seleccionar")));
        inv.setItem(22, makeItem(Material.PINK_CONCRETE, "§d§lRosa (&d)", "&d", id, Arrays.asList("§7Aplica este color al nombre", "", "§eClic para seleccionar")));
        inv.setItem(23, makeItem(Material.PURPLE_CONCRETE, "§5§lMorado (&5)", "&5", id, Arrays.asList("§7Aplica este color al nombre", "", "§eClic para seleccionar")));
        inv.setItem(24, makeItem(Material.WHITE_CONCRETE, "§f§lBlanco (&f)", "&f", id, Arrays.asList("§7Aplica este color al nombre", "", "§eClic para seleccionar")));
        inv.setItem(25, makeItem(Material.LIGHT_GRAY_CONCRETE, "§7§lGris (&7)", "&7", id, Arrays.asList("§7Aplica este color al nombre", "", "§eClic para seleccionar")));

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

        String colorCode = meta.getPersistentDataContainer().get(keyColorCode, PersistentDataType.STRING);
        if (colorCode == null && !isOurTitle) return;

        event.setCancelled(true);
        if (colorCode == null) return;

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

        if ("back".equals(colorCode)) {
            player.closeInventory();
            plugin.getNPCEditorGUI().openEditor(player, npc);
            return;
        }

        String plainName = ChatColor.stripColor(npc.getName());
        String newColoredName = ChatColor.translateAlternateColorCodes('&', colorCode + plainName);
        npc.setName(newColoredName);
        nm.syncHologram(npc);
        npc.getFakePlayer().setName(newColoredName);
        nm.saveNPC(npc);

        player.sendMessage("§a[NpcFant] Color del nombre actualizado a: " + newColoredName);
        player.closeInventory();
        plugin.getNPCEditorGUI().openEditor(player, npc);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
    }

    private ItemStack makeItem(Material material, String name, String code, UUID npcId, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            if (code != null) {
                meta.getPersistentDataContainer().set(keyColorCode, PersistentDataType.STRING, code);
            }
            if (npcId != null) {
                meta.getPersistentDataContainer().set(keyNpcId, PersistentDataType.STRING, npcId.toString());
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
