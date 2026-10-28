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

public class NPCEditorGUI implements Listener {

    private static final String GUI_TITLE_PREFIX = "§8§l✦ §6§lConfigurar NPC: §f";
    private static final int GUI_SIZE = 45;

    private final AdvancedNPCPlugin plugin;
    private final NamespacedKey keyAction;
    private final Map<UUID, UUID> editingNpcMap = new HashMap<>();
    private final Map<UUID, UUID> pendingNameChat = new ConcurrentHashMap<>();

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

        inv.setItem(4, makeItem(
            Material.PLAYER_HEAD,
            "§b§lCambiar de NPC §7(Seleccionar otro)",
            "select_npc",
            Arrays.asList(
                "§7Editando actualmente: §f" + npc.getName(),
                "§7Total de NPCs creados: §e" + plugin.getNpcManager().getAllNPCs().size(),
                "",
                "§a▶ Clic para abrir el selector de NPCs"
            )
        ));

        NPCPose currentPose = npc.getPose();
        inv.setItem(10, makeItem(
            Material.ARMOR_STAND,
            "§e§lPose del NPC",
            "pose",
            Arrays.asList(
                "§7Pose actual: §a" + currentPose.name(),
                "",
                "§eClic para abrir el menú de poses",
                "§8(De pie, Agachado, Sentado, Durmiendo, etc.)"
            )
        ));

        inv.setItem(11, makeItem(
            Material.NAME_TAG,
            "§b§lCambiar Nombre y Color",
            "name",
            Arrays.asList(
                "§7Nombre actual: §f" + npc.getName(),
                "",
                "§eClic Izquierdo: §7Escribir nuevo texto en el chat",
                "§eClic Derecho: §7Paleta de colores del nombre",
                "",
                "§8Soporta códigos '&' (ej: &6&lRey &bNPC)"
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

        inv.setItem(13, makeItem(
            Material.COMMAND_BLOCK,
            "§6§lComandos y Acciones al Clic",
            "actions",
            Arrays.asList(
                "§7Configura qué comandos o acciones",
                "§7ejecutará el NPC al recibir un clic.",
                "",
                "§fAcciones activas: §e" + (npc.getActionChain() != null ? npc.getActionChain().size() : 0),
                "",
                "§eClic para abrir el editor de comandos"
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
        inv.setItem(15, makeItem(
            Material.GLOWSTONE_DUST,
            "§6§lEfecto de Brillo (Glow)",
            "glow",
            Arrays.asList(
                "§7Estado: " + (hasGlow ? "§a" + glow : "§cDesactivado"),
                "",
                "§eClic para abrir la paleta de colores de Glow"
            )
        ));

        inv.setItem(16, makeItem(
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
        inv.setItem(20, makeItem(
            Material.NAME_TAG,
            "§f§lVisibilidad del Nametag",
            "nametag",
            Arrays.asList(
                "§7Nametag sobre la cabeza: " + (showNametag ? "§aVisible" : "§cOculto"),
                "",
                "§eClic para alternar visibilidad"
            )
        ));

        inv.setItem(21, makeItem(
            Material.OAK_SIGN,
            "§d§l✦ Menú de Hologramas",
            "holograms",
            Arrays.asList(
                "§7Configura líneas flotantes, colores",
                "§7y textos informativos sobre el NPC.",
                "",
                "§fLíneas activas: §e" + (npc.getHologramLines() != null ? npc.getHologramLines().size() : 0),
                "",
                "§eClic para abrir el editor de hologramas"
            )
        ));

        boolean invisible = npc.isInvisible();
        inv.setItem(22, makeItem(
            Material.GLASS_BOTTLE,
            "§7§lInvisibilidad",
            "invisible",
            Arrays.asList(
                "§7Estado: " + (invisible ? "§aInvisible" : "§cVisible"),
                "",
                "§eClic para alternar"
            )
        ));

        inv.setItem(24, makeItem(
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

        inv.setItem(31, makeItem(
            Material.BARRIER,
            "§c§lEliminar NPC",
            "delete",
            Arrays.asList(
                "§4¡Atención! Esta acción no se puede deshacer.",
                "",
                "§eClic para eliminar este NPC definitivamente."
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

            case "select_npc" -> {
                player.closeInventory();
                if (plugin.getNpcManager().getSelectorGUI() != null) {
                    plugin.getNpcManager().getSelectorGUI().openGUI(player);
                }
            }

            case "holograms" -> {
                player.closeInventory();
                if (plugin.getNpcManager().getHologramGUI() != null) {
                    plugin.getNpcManager().getHologramGUI().openGUI(player, npc);
                }
            }

            case "pose" -> {
                player.closeInventory();
                plugin.getNPCPoseGUI().openGUI(player, npc);
            }

            case "name" -> {
                if (event.isRightClick()) {
                    player.closeInventory();
                    plugin.getNPCNameColorGUI().openGUI(player, npc);
                } else {
                    pendingNameChat.put(player.getUniqueId(), npc.getId());
                    player.closeInventory();
                    player.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    player.sendMessage("§6§l✦ NpcFant — Cambiar Nombre del NPC");
                    player.sendMessage("§7Escribe el nuevo nombre para el NPC en el chat.");
                    player.sendMessage("§7Puedes usar códigos de color como §a&a§7, §b&b§7, §e&e§7, §l&l (negrita)§7, etc.");
                    player.sendMessage("§7Escribe §ccancel §7para cancelar.");
                    player.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                }
            }

            case "skin" -> {
                player.closeInventory();
                plugin.getSkinMenuGUI().openMenuForNPC(player, npc, 0);
            }

            case "actions" -> {
                player.closeInventory();
                plugin.getNPCActionGUI().openGUI(player, npc);
            }

            case "look" -> {
                boolean newLook = !npc.isLookAtPlayer();
                npc.setLookAtPlayer(newLook);
                nm.saveNPC(npc);
                player.sendMessage("§a[NpcFant] Mirar al jugador: " + (newLook ? "§aActivado" : "§cDesactivado"));
                openEditor(player, npc);
            }

            case "glow" -> {
                player.closeInventory();
                plugin.getNPCGlowGUI().openGUI(player, npc);
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
                editingNpcMap.remove(player.getUniqueId());
                player.closeInventory();
                nm.removeNPC(npc.getId());
                player.sendMessage("§c[NpcFant] NPC '" + npc.getName() + "' eliminado correctamente.");
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        editingNpcMap.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID npcId = pendingNameChat.remove(player.getUniqueId());
        if (npcId == null) return;

        event.setCancelled(true);
        String msg = event.getMessage().trim();
        if (msg.equalsIgnoreCase("cancel")) {
            player.sendMessage("§c[NpcFant] Cambio de nombre cancelado.");
            Bukkit.getScheduler().runTask(plugin, () -> {
                NPCEntity npc = (NPCEntity) plugin.getNpcManager().getNPC(npcId);
                if (npc != null) openEditor(player, npc);
            });
            return;
        }

        String colored = ChatColor.translateAlternateColorCodes('&', msg);
        Bukkit.getScheduler().runTask(plugin, () -> {
            NPCManager nm = plugin.getNpcManager();
            NPCEntity npc = (NPCEntity) nm.getNPC(npcId);
            if (npc == null) {
                player.sendMessage("§c[NpcFant] El NPC ya no existe.");
                return;
            }
            npc.setName(colored);
            nm.syncHologram(npc);
            npc.getFakePlayer().setName(colored);
            nm.saveNPC(npc);
            player.sendMessage("§a[NpcFant] ¡Nombre actualizado a: " + colored + "§a!");
            openEditor(player, npc);
        });
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

