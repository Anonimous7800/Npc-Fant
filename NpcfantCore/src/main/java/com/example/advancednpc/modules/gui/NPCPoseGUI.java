package com.example.advancednpc.modules.gui;

import com.example.advancednpc.AdvancedNPCPlugin;
import com.example.advancednpc.modules.animation.NPCPose;
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

public class NPCPoseGUI implements Listener {

    private static final String GUI_TITLE_PREFIX = "§8§l✦ §6§lPose del NPC: §f";
    private static final int GUI_SIZE = 36;

    private final AdvancedNPCPlugin plugin;
    private final NamespacedKey keyPose;
    private final NamespacedKey keyNpcId;
    private final Map<UUID, UUID> activeNpcMap = new ConcurrentHashMap<>();

    public NPCPoseGUI(AdvancedNPCPlugin plugin) {
        this.plugin = plugin;
        this.keyPose = new NamespacedKey(plugin, "npcfant_pose_type");
        this.keyNpcId = new NamespacedKey(plugin, "npcfant_pose_npcid");
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

        NPCPose current = npc.getPose();

        inv.setItem(11, makeItem(
            Material.PLAYER_HEAD,
            "§a§lDe Pie (STANDING)",
            "STANDING",
            id,
            Arrays.asList(
                "§7Postura erguida estándar.",
                "",
                (current == NPCPose.STANDING ? "§a✔ Pose actual" : "§eClic para seleccionar")
            )
        ));

        inv.setItem(12, makeItem(
            Material.LEATHER_BOOTS,
            "§e§lAgachado (SNEAKING)",
            "SNEAKING",
            id,
            Arrays.asList(
                "§7Postura agachada (Shift / Crouch).",
                "",
                (current == NPCPose.SNEAKING ? "§a✔ Pose actual" : "§eClic para seleccionar")
            )
        ));

        inv.setItem(13, makeItem(
            Material.OAK_STAIRS,
            "§6§lSentado (SITTING)",
            "SITTING",
            id,
            Arrays.asList(
                "§7Postura de descanso sentado.",
                "",
                (current == NPCPose.SITTING ? "§a✔ Pose actual" : "§eClic para seleccionar")
            )
        ));

        inv.setItem(14, makeItem(
            Material.RED_BED,
            "§b§lDurmiendo (SLEEPING)",
            "SLEEPING",
            id,
            Arrays.asList(
                "§7Postura horizontal acostado.",
                "",
                (current == NPCPose.SLEEPING ? "§a✔ Pose actual" : "§eClic para seleccionar")
            )
        ));

        inv.setItem(15, makeItem(
            Material.WATER_BUCKET,
            "§9§lNadando (SWIMMING)",
            "SWIMMING",
            id,
            Arrays.asList(
                "§7Postura de nado / arrastre.",
                "",
                (current == NPCPose.SWIMMING ? "§a✔ Pose actual" : "§eClic para seleccionar")
            )
        ));

        inv.setItem(21, makeItem(
            Material.TRIDENT,
            "§d§lGiro con Tridente (SPIN_ATTACK)",
            "SPIN_ATTACK",
            id,
            Arrays.asList(
                "§7Ataque giratorio Riptide con tridente.",
                "",
                (current == NPCPose.SPIN_ATTACK ? "§a✔ Pose actual" : "§eClic para seleccionar")
            )
        ));

        inv.setItem(23, makeItem(
            Material.TOTEM_OF_UNDYING,
            "§c§lDerribado (DYING)",
            "DYING",
            id,
            Arrays.asList(
                "§7Postura de jugador caído en el suelo.",
                "",
                (current == NPCPose.DYING ? "§a✔ Pose actual" : "§eClic para seleccionar")
            )
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

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String poseStr = meta.getPersistentDataContainer().get(keyPose, PersistentDataType.STRING);
        String idStr = meta.getPersistentDataContainer().get(keyNpcId, PersistentDataType.STRING);
        UUID npcId = null;
        if (idStr != null) {
            try {
                npcId = UUID.fromString(idStr);
                activeNpcMap.put(player.getUniqueId(), npcId);
            } catch (IllegalArgumentException ignored) {}
        }
        if (npcId == null) {
            npcId = activeNpcMap.get(player.getUniqueId());
        }

        String title = event.getView().getTitle();
        boolean isOurTitle = title != null && title.startsWith(GUI_TITLE_PREFIX);
        if (poseStr == null && !isOurTitle) return;

        event.setCancelled(true);
        if (poseStr == null) return;

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

        if (poseStr.equalsIgnoreCase("back")) {
            player.closeInventory();
            plugin.getNPCEditorGUI().openEditor(player, npc);
            return;
        }

        try {
            NPCPose newPose = NPCPose.valueOf(poseStr.toUpperCase());
            npc.setPose(newPose);
            nm.saveNPC(npc);
            nm.syncHologram(npc);
            player.sendMessage("§a[NpcFant] Pose de '§e" + npc.getName() + "§a' cambiada a: §e" + newPose.name());
            openGUI(player, npc);
        } catch (IllegalArgumentException ignored) {}
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
    }

    private ItemStack makeItem(Material material, String name, String pose, UUID npcId, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            if (pose != null) {
                meta.getPersistentDataContainer().set(keyPose, PersistentDataType.STRING, pose);
            }
            if (npcId != null) {
                meta.getPersistentDataContainer().set(keyNpcId, PersistentDataType.STRING, npcId.toString());
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}

