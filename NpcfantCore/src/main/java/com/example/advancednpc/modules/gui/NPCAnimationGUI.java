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

public class NPCAnimationGUI implements Listener {

    private static final String GUI_TITLE_PREFIX = "§8§l✦ §d§lAnimaciones: §f";
    private static final int GUI_SIZE = 36;

    private final AdvancedNPCPlugin plugin;
    private final NamespacedKey keyAnim;
    private final Map<UUID, UUID> activeNpcMap = new ConcurrentHashMap<>();

    public NPCAnimationGUI(AdvancedNPCPlugin plugin) {
        this.plugin = plugin;
        this.keyAnim = new NamespacedKey(plugin, "npcfant_anim_action");
    }

    public void openGUI(Player player, NPCEntity npc) {
        if (player == null || npc == null) return;
        activeNpcMap.put(player.getUniqueId(), npc.getId());

        String title = GUI_TITLE_PREFIX + ChatColor.stripColor(npc.getName());
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, title);

        ItemStack border = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, null);
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 27; i < 36; i++) inv.setItem(i, border);
        inv.setItem(9, border);
        inv.setItem(17, border);
        inv.setItem(18, border);
        inv.setItem(26, border);

        inv.setItem(10, makeItem(
            Material.PLAYER_HEAD,
            "§a§lSaludar ahora (Levantar mano y agitar)",
            "swing_main",
            Arrays.asList(
                "§7El NPC levanta su mano y la agita",
                "§7saludando a los jugadores.",
                "",
                "§eClic para ejecutar animación"
            )
        ));

        inv.setItem(11, makeItem(
            Material.SHIELD,
            "§b§lSaludar con mano secundaria (Offhand)",
            "swing_off",
            Arrays.asList(
                "§7El NPC levanta y mueve su mano izquierda.",
                "",
                "§eClic para ejecutar animación"
            )
        ));

        inv.setItem(12, makeItem(
            Material.LEATHER_BOOTS,
            "§e§lHacer Reverencia (Inclinación)",
            "bow",
            Arrays.asList(
                "§7El NPC realiza una reverencia breve",
                "§7inclinando la cabeza y agachándose.",
                "",
                "§eClic para ejecutar animación"
            )
        ));

        inv.setItem(13, makeItem(
            Material.CLOCK,
            "§6§lSaludo Consecutivo (Bucle Continuo)",
            "toggle_consecutive",
            Arrays.asList(
                "§7El NPC saluda y levanta la mano",
                "§7de forma continua y consecutiva",
                "§7cada 3 segundos a los jugadores cercanos.",
                "",
                "§7Estado: " + (npc.isConsecutiveWave() ? "§aActivado (Consecutivo)" : "§cDesactivado"),
                "",
                "§eClic para alternar saludo continuo"
            )
        ));

        inv.setItem(14, makeItem(
            Material.BELL,
            "§e§lSaludo al Acercarse (Auto-Wave)",
            "toggle_wave",
            Arrays.asList(
                "§7Saluda levantando la mano cuando",
                "§7un jugador entra en su rango.",
                "",
                "§7Estado: " + (npc.isAutoWave() ? "§aActivado" : "§cDesactivado"),
                "",
                "§eClic para alternar"
            )
        ));

        inv.setItem(15, makeItem(
            Material.REDSTONE,
            "§c§lAnimación de Daño / Sobresalto",
            "hurt",
            Arrays.asList(
                "§7El NPC realiza la animación de golpe",
                "§7o sobresalto momentáneo.",
                "",
                "§eClic para ejecutar animación"
            )
        ));

        inv.setItem(16, makeItem(
            Material.EMERALD,
            "§a§lPartículas de Festejo (Happy)",
            "particles",
            Arrays.asList(
                "§7Genera partículas brillantes sobre",
                "§7la cabeza del NPC como festejo.",
                "",
                "§eClic para lanzar partículas"
            )
        ));

        inv.setItem(31, makeItem(
            Material.ARROW,
            "§7← Volver al Editor",
            "back",
            Collections.singletonList("§7Regresar al menú principal de configuración")
        ));

        player.openInventory(inv);
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

        String action = meta.getPersistentDataContainer().get(keyAnim, PersistentDataType.STRING);
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
            case "swing_main" -> {
                npc.getFakePlayer().waveArm();
                player.sendMessage("§a[NpcFant] ¡" + npc.getName() + " §aestá saludando con la mano!");
            }
            case "swing_off" -> {
                npc.getFakePlayer().swingOffHand();
                player.sendMessage("§a[NpcFant] ¡" + npc.getName() + " §asaludó con la mano izquierda!");
            }
            case "bow" -> {
                npc.getFakePlayer().bow();
                player.sendMessage("§a[NpcFant] ¡" + npc.getName() + " §arealizó una reverencia!");
            }
            case "toggle_consecutive" -> {
                boolean newState = !npc.isConsecutiveWave();
                npc.setConsecutiveWave(newState);
                nm.saveNPC(npc);
                player.sendMessage("§a[NpcFant] Saludo consecutivo continuo: " + (newState ? "§aActivado (Cada 3s)" : "§cDesactivado"));
                openGUI(player, npc);
            }
            case "toggle_wave" -> {
                boolean newState = !npc.isAutoWave();
                npc.setAutoWave(newState);
                nm.saveNPC(npc);
                player.sendMessage("§a[NpcFant] Auto-saludo al acercarse: " + (newState ? "§aActivado" : "§cDesactivado"));
                openGUI(player, npc);
            }
            case "hurt" -> {
                npc.getFakePlayer().hurtAnimation();
                player.sendMessage("§e[NpcFant] Animación de impacto ejecutada.");
            }
            case "particles" -> {
                npc.getFakePlayer().playHappyParticles();
                player.sendMessage("§a[NpcFant] ¡Partículas de festejo lanzadas!");
            }
            case "back" -> {
                player.closeInventory();
                plugin.getNPCEditorGUI().openEditor(player, npc);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        activeNpcMap.remove(event.getPlayer().getUniqueId());
    }

    private ItemStack makeItem(Material material, String name, String action, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            if (action != null) {
                meta.getPersistentDataContainer().set(keyAnim, PersistentDataType.STRING, action);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
