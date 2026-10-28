package com.example.advancednpc.modules.gui;

import com.example.advancednpc.AdvancedNPCPlugin;
import com.example.advancednpc.modules.hologram.HologramLine;
import com.example.advancednpc.npc.NPCEntity;
import com.example.advancednpc.npc.NPCManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class NPCSelectorGUI implements Listener {

    private static final String GUI_TITLE = "§8§l✦ §6§lSeleccionar NPC a Configurar";
    private static final int GUI_SIZE = 54;

    private final AdvancedNPCPlugin plugin;
    private final NamespacedKey keyNpcId;
    private final NamespacedKey keyAction;

    public NPCSelectorGUI(AdvancedNPCPlugin plugin) {
        this.plugin = plugin;
        this.keyNpcId = new NamespacedKey(plugin, "npcfant_select_uuid");
        this.keyAction = new NamespacedKey(plugin, "npcfant_select_action");
    }

    public void openGUI(Player player) {
        if (player == null || !player.isOnline()) return;

        NPCManager nm = plugin.getNpcManager();
        Collection<?> allRaw = nm.getAllNPCs();
        List<NPCEntity> npcs = new ArrayList<>();
        for (Object o : allRaw) {
            if (o instanceof NPCEntity e) npcs.add(e);
        }

        Location pLoc = player.getLocation();
        npcs.sort(Comparator.comparingDouble(n -> {
            Location loc = n.getLocation();
            if (loc == null || loc.getWorld() == null || !loc.getWorld().equals(pLoc.getWorld())) {
                return 999999.0;
            }
            return loc.distanceSquared(pLoc);
        }));

        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);

        ItemStack border = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, null, null);
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

        int slot = 10;
        for (NPCEntity npc : npcs) {
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;

            Location loc = npc.getLocation();
            String distStr = "Mundo diferente";
            if (loc != null && loc.getWorld() != null && loc.getWorld().equals(pLoc.getWorld())) {
                double dist = loc.distance(pLoc);
                distStr = String.format("%.1f bloques", dist);
            }

            String worldName = loc != null && loc.getWorld() != null ? loc.getWorld().getName() : "N/A";
            String coords = loc != null ? String.format("X: %.0f, Y: %.0f, Z: %.0f", loc.getX(), loc.getY(), loc.getZ()) : "N/A";

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = head.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e§l" + HologramLine.colorize(npc.getName()));
                meta.setLore(Arrays.asList(
                    "§7UUID: §8" + npc.getId().toString().substring(0, 8) + "...",
                    "§7Pose: §f" + npc.getPose().name(),
                    "§7Distancia a ti: §a" + distStr,
                    "§7Posición: §b" + worldName + " §7(" + coords + ")",
                    "§7Acciones configuradas: §6" + (npc.getActionChain() != null ? npc.getActionChain().size() : 0),
                    "§7Líneas de holograma: §d" + npc.getHologramLines().size(),
                    "",
                    "§a▶ Haz clic para configurar este NPC"
                ));
                meta.getPersistentDataContainer().set(keyNpcId, PersistentDataType.STRING, npc.getId().toString());
                head.setItemMeta(meta);
            }

            inv.setItem(slot, head);
            slot++;
        }

        if (npcs.isEmpty()) {
            inv.setItem(22, makeItem(
                Material.BARRIER,
                "§c§lNo hay NPCs creados",
                null,
                null,
                Arrays.asList(
                    "§7Aún no se ha creado ningún NPC en el servidor.",
                    "",
                    "§eUsa: §f/nfant create <nombre>",
                    "§7para crear tu primer NPC."
                )
            ));
        }

        inv.setItem(48, makeItem(
            Material.COMPASS,
            "§b§lActualizar Lista",
            "refresh",
            null,
            Collections.singletonList("§7Recargar los NPCs cercanos")
        ));

        inv.setItem(50, makeItem(
            Material.RED_STAINED_GLASS_PANE,
            "§c✕ Cerrar",
            "close",
            null,
            Collections.singletonList("§7Cerrar este menú de selección")
        ));

        player.openInventory(inv);
    }

    private ItemStack makeItem(Material mat, String name, String action, String npcId, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            if (action != null) {
                meta.getPersistentDataContainer().set(keyAction, PersistentDataType.STRING, action);
            }
            if (npcId != null) {
                meta.getPersistentDataContainer().set(keyNpcId, PersistentDataType.STRING, npcId);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!GUI_TITLE.equals(title)) return;

        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String action = meta.getPersistentDataContainer().get(keyAction, PersistentDataType.STRING);
        if ("close".equals(action)) {
            player.closeInventory();
            return;
        }
        if ("refresh".equals(action)) {
            openGUI(player);
            return;
        }

        String idStr = meta.getPersistentDataContainer().get(keyNpcId, PersistentDataType.STRING);
        if (idStr == null) return;

        try {
            UUID id = UUID.fromString(idStr);
            NPCEntity npc = (NPCEntity) plugin.getNpcManager().getNPC(id);
            if (npc != null) {
                if (plugin.getNpcManager().getEditorGUI() != null) {
                    plugin.getNpcManager().getEditorGUI().openEditor(player, npc);
                }
            } else {
                player.sendMessage("§c[NpcFant] Este NPC ya no existe.");
                openGUI(player);
            }
        } catch (Exception ignored) {}
    }
}
