package com.example.advancednpc.modules.gui;

import com.example.advancednpc.AdvancedNPCPlugin;
import com.example.advancednpc.modules.animation.NPCPose;
import com.example.advancednpc.modules.equipment.NPCEquipment;
import com.example.advancednpc.npc.NPCEntity;
import com.example.advancednpc.npc.NPCManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class NPCEditorGUI implements Listener {

    private static final String GUI_TITLE_PREFIX = "§8§l✦ §6§lConfigurar NPC: §f";
    private static final int GUI_SIZE = 45;

    private final AdvancedNPCPlugin plugin;
    private final NamespacedKey keyAction;
    private final Map<UUID, UUID> editingNpcMap = new HashMap<>();

    public NPCEditorGUI(AdvancedNPCPlugin plugin) {
        this.plugin = plugin;
        this.keyAction = new NamespacedKey(plugin, "npcfant_editor_action");
    }

    public void openEditor(Player player, NPCEntity npc) {
        if (player == null || npc == null) return;
        editingNpcMap.put(player.getUniqueId(), npc.getId());

        String title = GUI_TITLE_PREFIX + ChatColor.stripColor(npc.getName());
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, title);

        ItemStack border = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, null);
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 36; i < 45; i++) inv.setItem(i, border);
        inv.setItem(9, border);
        inv.setItem(17, border);
        inv.setItem(18, border);
        inv.setItem(26, border);
        inv.setItem(27, border);
        inv.setItem(35, border);

        NPCPose currentPose = npc.getPose();
        inv.setItem(10, makeItem(
            Material.ARMOR_STAND,
            "§e§lPose del NPC",
            "pose",
            Arrays.asList(
                "§7Pose actual: §a" + currentPose.name(),
                "",
                "§fClic Izquierdo: §7Siguiente pose",
                "§fClic Derecho: §7Pose anterior",
                "§8Opciones: STANDING, SNEAKING, SITTING, SLEEPING, SWIMMING"
            )
        ));

        inv.setItem(12, makeItem(
            Material.PLAYER_HEAD,
            "§b§lCambiar Skin",
            "skin",
            Arrays.asList(
                "§7Skin personalizada o del catálogo.",
                "",
                "§eClic para abrir el catálogo de skins (.png)"
            )
        ));

        boolean lookAt = npc.isLookAtPlayer();
        inv.setItem(14, makeItem(
            Material.ENDER_EYE,
            "§d§lMirar al Jugador",
            "look",
            Arrays.asList(
                "§7Estado: " + (lookAt ? "§aActivado (Gira cuerpo y cabeza)" : "§cDesactivado (Estático)"),
                "",
                "§eClic para alternar"
            )
        ));

        String glow = npc.getGlowColor();
        boolean hasGlow = glow != null && !glow.isEmpty() && !glow.equalsIgnoreCase("off");
        inv.setItem(16, makeItem(
            Material.GLOWSTONE_DUST,
            "§6§lEfecto de Brillo (Glow)",
            "glow",
            Arrays.asList(
                "§7Estado: " + (hasGlow ? "§a" + glow : "§cDesactivado"),
                "",
                "§eClic para alternar colores",
                "§8(RED, GOLD, GREEN, AQUA, BLUE, LIGHT_PURPLE, WHITE, OFF)"
            )
        ));

        inv.setItem(20, makeItem(
            Material.DIAMOND_CHESTPLATE,
            "§a§lEquipamiento",
            "equip",
            Arrays.asList(
                "§7Copia ítems de tu inventario al NPC:",
                "§f- Clic Izquierdo: §7Copiar tu mano principal",
                "§f- Clic Derecho: §7Copiar tu armadura completa",
                "§c- Shift + Clic: §7Limpiar todo el equipamiento"
            )
        ));

        boolean showNametag = npc.isShowNametag();
        inv.setItem(22, makeItem(
            Material.NAME_TAG,
            "§f§lVisibilidad del Nametag",
            "nametag",
            Arrays.asList(
                "§7Nametag sobre la cabeza: " + (showNametag ? "§aVisible" : "§cOculto"),
                "",
                "§eClic para alternar visibilidad"
            )
        ));

        boolean invisible = npc.isInvisible();
        inv.setItem(24, makeItem(
            Material.GLASS_BOTTLE,
            "§7§lInvisibilidad",
            "invisible",
            Arrays.asList(
                "§7Estado: " + (invisible ? "§aInvisible" : "§cVisible"),
                "",
                "§eClic para alternar"
            )
        ));

        inv.setItem(30, makeItem(
            Material.COMPASS,
            "§e§lMover a mi posición",
            "move",
            Arrays.asList(
                "§7Teletransporta este NPC a tu",
                "§7ubicación y orientación actual.",
                "",
                "§eClic para teletransportar aquí"
            )
        ));

        inv.setItem(32, makeItem(
            Material.BARRIER,
            "§c§lEliminar NPC",
            "delete",
            Arrays.asList(
                "§4¡Atención! Esta acción no se puede deshacer.",
                "",
                "§cShift + Clic para confirmar eliminación"
            )
        ));

        inv.setItem(40, makeItem(
            Material.RED_STAINED_GLASS_PANE,
            "§c✕ Cerrar",
            "close",
            Collections.singletonList("§7Cierra este menú de configuración")
        ));

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID npcId = editingNpcMap.get(player.getUniqueId());
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
            case "close" -> player.closeInventory();

            case "pose" -> {
                NPCPose[] poses = NPCPose.values();
                int idx = 0;
                for (int i = 0; i < poses.length; i++) {
                    if (poses[i] == npc.getPose()) {
                        idx = i;
                        break;
                    }
                }
                if (event.isRightClick()) {
                    idx = (idx - 1 + poses.length) % poses.length;
                } else {
                    idx = (idx + 1) % poses.length;
                }
                NPCPose newPose = poses[idx];
                npc.setPose(newPose);
                nm.saveNPC(npc);
                player.sendMessage("§a[NpcFant] Pose cambiada a: §e" + newPose.name());
                openEditor(player, npc);
            }

            case "skin" -> {
                player.closeInventory();
                plugin.getSkinMenuGUI().openMenu(player, 0);
            }

            case "look" -> {
                boolean newLook = !npc.isLookAtPlayer();
                npc.setLookAtPlayer(newLook);
                nm.saveNPC(npc);
                player.sendMessage("§a[NpcFant] Mirar al jugador: " + (newLook ? "§aActivado" : "§cDesactivado"));
                openEditor(player, npc);
            }

            case "glow" -> {
                String[] colors = {"RED", "GOLD", "GREEN", "AQUA", "BLUE", "LIGHT_PURPLE", "WHITE", null};
                int idx = colors.length - 1;
                for (int i = 0; i < colors.length; i++) {
                    if (Objects.equals(colors[i], npc.getGlowColor())) {
                        idx = i;
                        break;
                    }
                }
                idx = (idx + 1) % colors.length;
                String newColor = colors[idx];
                npc.setGlowColor(newColor);
                nm.saveNPC(npc);
                player.sendMessage("§a[NpcFant] Glow cambiado a: §e" + (newColor == null ? "Desactivado" : newColor));
                openEditor(player, npc);
            }

            case "equip" -> {
                if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                    npc.setEquipment(new NPCEquipment());
                    nm.saveNPC(npc);
                    player.sendMessage("§c[NpcFant] Equipamiento del NPC limpiado.");
                } else if (event.isRightClick()) {
                    NPCEquipment eq = npc.getEquipment() != null ? npc.getEquipment() : new NPCEquipment();
                    eq.helmet(player.getInventory().getHelmet());
                    eq.chestplate(player.getInventory().getChestplate());
                    eq.leggings(player.getInventory().getLeggings());
                    eq.boots(player.getInventory().getBoots());
                    npc.setEquipment(eq);
                    nm.saveNPC(npc);
                    player.sendMessage("§a[NpcFant] Armadura de tu personaje copiada al NPC.");
                } else {
                    NPCEquipment eq = npc.getEquipment() != null ? npc.getEquipment() : new NPCEquipment();
                    eq.mainHand(player.getInventory().getItemInMainHand());
                    npc.setEquipment(eq);
                    nm.saveNPC(npc);
                    player.sendMessage("§a[NpcFant] Ítem en mano principal copiado al NPC.");
                }
                openEditor(player, npc);
            }

            case "nametag" -> {
                boolean newNametag = !npc.isShowNametag();
                npc.setShowNametag(newNametag);
                nm.syncHologram(npc);
                nm.saveNPC(npc);
                player.sendMessage("§a[NpcFant] Nametag cambiado a: " + (newNametag ? "§aVisible" : "§cOculto"));
                openEditor(player, npc);
            }

            case "invisible" -> {
                boolean newInvis = !npc.isInvisible();
                npc.setInvisible(newInvis);
                nm.saveNPC(npc);
                player.sendMessage("§a[NpcFant] Invisibilidad: " + (newInvis ? "§aActivada" : "§cDesactivada"));
                openEditor(player, npc);
            }

            case "move" -> {
                nm.moveNPC(npc.getId(), player.getLocation());
                player.sendMessage("§a[NpcFant] NPC movido a tu ubicación actual.");
                openEditor(player, npc);
            }

            case "delete" -> {
                if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                    nm.removeNPC(npc.getId());
                    player.sendMessage("§c[NpcFant] NPC '" + npc.getName() + "' eliminado correctamente.");
                    player.closeInventory();
                } else {
                    player.sendMessage("§e[NpcFant] Para eliminar el NPC, mantén presionado §cSHIFT + Clic§e.");
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        editingNpcMap.remove(event.getPlayer().getUniqueId());
    }

    private ItemStack makeItem(Material material, String name, String action, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            if (action != null) {
                meta.getPersistentDataContainer().set(keyAction, PersistentDataType.STRING, action);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}

