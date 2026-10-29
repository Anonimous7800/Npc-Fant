package com.example.advancednpc.modules.gui;

import com.example.advancednpc.AdvancedNPCPlugin;
import com.example.advancednpc.modules.skin.SkinData;
import com.example.advancednpc.modules.skin.SkinManager;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.*;

public class SkinMenuGUI implements Listener {


    private static final String  GUI_TITLE        = "§8§l✦ §b§lEditor de Skins §8§l✦";
    private static final int     GUI_SIZE         = 54;
    private static final int     SKINS_PER_PAGE   = 28;
    private static final int     SKIN_START_SLOT  = 10;
    private static final double  INTERACTION_DIST = 15.0;

    private static final String  NBT_SKIN_KEY  = "npcfant_skin_key";
    private static final String  NBT_SKIN_TYPE = "npcfant_skin_type";
    private static final String  NBT_GUI_ITEM  = "npcfant_gui_ender_eye";
    private static final String  NBT_PAGE      = "npcfant_page";

    private final AdvancedNPCPlugin plugin;
    private final NamespacedKey keyGuiItem;
    private final NamespacedKey keySkinKey;
    private final NamespacedKey keySkinType;
    private final NamespacedKey keyPage;
    private final NamespacedKey keyTargetNpc;

    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<UUID, List<Map.Entry<String, SkinData>>> playerSkinList = new HashMap<>();
    private final Map<UUID, UUID> targetNpcMap = new java.util.concurrent.ConcurrentHashMap<>();

    public SkinMenuGUI(AdvancedNPCPlugin plugin) {
        this.plugin       = plugin;
        this.keyGuiItem   = new NamespacedKey(plugin, NBT_GUI_ITEM);
        this.keySkinKey   = new NamespacedKey(plugin, NBT_SKIN_KEY);
        this.keySkinType  = new NamespacedKey(plugin, NBT_SKIN_TYPE);
        this.keyPage      = new NamespacedKey(plugin, NBT_PAGE);
        this.keyTargetNpc = new NamespacedKey(plugin, "npcfant_skin_npcid");
    }




    public ItemStack createOpenerItem() {
        ItemStack item = new ItemStack(Material.ENDER_EYE);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "✦ Editor de Skins");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Clic derecho para abrir el",
            ChatColor.GRAY + "menú de personalización de skins.",
            "",
            ChatColor.YELLOW + "⚡ El NPC seleccionado será",
            ChatColor.YELLOW + "el más cercano a ti (≤15 bloques)."
        ));
        meta.getPersistentDataContainer()
            .set(keyGuiItem, PersistentDataType.STRING, "true");
        item.setItemMeta(meta);
        return item;
    }


    public boolean isOpenerItem(ItemStack item) {
        if (item == null || item.getType() != Material.ENDER_EYE) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer()
            .has(keyGuiItem, PersistentDataType.STRING);
    }




    public void openMenu(Player player, int page) {
        openMenu(player, page, null);
    }

    public void openMenuForNPC(Player player, NPCEntity npc, int page) {
        if (player == null || !player.isOnline()) return;
        if (npc != null) {
            targetNpcMap.put(player.getUniqueId(), npc.getId());
        }
        openMenu(player, page);
    }

    private void openMenu(Player player, int page, Void unused) {
        SkinManager skinManager = plugin.getNpcManager().getSkinManager();
        if (skinManager == null) {
            player.sendMessage(ChatColor.RED + "[NpcFant] SkinManager no disponible.");
            return;
        }
        skinManager.reload();


        List<Map.Entry<String, SkinData>> allSkins = new ArrayList<>();
        skinManager.getPresetSkins().entrySet().stream()
            .map(e -> Map.entry("preset:" + e.getKey(), e.getValue()))
            .forEach(allSkins::add);

        Set<String> addedFiles = new HashSet<>();
        skinManager.getLocalSkins().forEach((k, v) -> {
            addedFiles.add(k);
            allSkins.add(Map.entry("file:" + k, v));
        });
        skinManager.getLocalPngFiles().keySet().forEach(k -> {
            if (addedFiles.add(k)) {
                allSkins.add(Map.entry("file:" + k, skinManager.getDefaultSkin()));
            }
        });

        playerSkinList.put(player.getUniqueId(), allSkins);
        playerPages.put(player.getUniqueId(), page);

        int totalPages = Math.max(1, (int) Math.ceil((double) allSkins.size() / SKINS_PER_PAGE));
        int safePage   = Math.max(0, Math.min(page, totalPages - 1));
        playerPages.put(player.getUniqueId(), safePage);

        Inventory inv = Bukkit.createInventory(null, GUI_SIZE,
            GUI_TITLE + " §7(" + (safePage + 1) + "/" + totalPages + ")");


        ItemStack border = makeBorderItem();
        for (int i = 0; i < 9; i++) inv.setItem(i, border);


        for (int i = 45; i < 54; i++) inv.setItem(i, border);


        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9, border);
            inv.setItem(row * 9 + 8, border);
        }


        if (safePage > 0) {
            inv.setItem(3, makeNavButton(Material.ARROW,
                ChatColor.GREEN + "◀ Anterior",
                Collections.singletonList(ChatColor.GRAY + "Página " + safePage)));
        }
        inv.setItem(4, makeInfoButton(allSkins.size(), safePage, totalPages));
        if (safePage < totalPages - 1) {
            inv.setItem(5, makeNavButton(Material.ARROW,
                ChatColor.GREEN + "Siguiente ▶",
                Collections.singletonList(ChatColor.GRAY + "Página " + (safePage + 2))));
        }


        inv.setItem(8, makeNavButton(Material.BARRIER,
            ChatColor.RED + "✕ Cerrar",
            Collections.singletonList(ChatColor.GRAY + "Cierra este menú")));


        ItemStack userBtn = new ItemStack(Material.NAME_TAG);
        ItemMeta  userMeta = userBtn.getItemMeta();
        if (userMeta != null) {
            userMeta.setDisplayName(ChatColor.YELLOW + "✎ Importar Skin .PNG");
            userMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Coloca tus archivos .png en:",
                ChatColor.AQUA + "plugins/NpcFant/skins/custom/",
                ChatColor.GRAY + "o usa: /npcfant skin <npc> <skin.png>"
            ));
            userBtn.setItemMeta(userMeta);
        }
        inv.setItem(46, userBtn);


        int startIdx = safePage * SKINS_PER_PAGE;
        int endIdx   = Math.min(startIdx + SKINS_PER_PAGE, allSkins.size());


        int[] skinSlots = buildSkinSlots();
        int slotIdx = 0;

        UUID targetNpcId = targetNpcMap.get(player.getUniqueId());

        for (int i = startIdx; i < endIdx && slotIdx < skinSlots.length; i++, slotIdx++) {
            Map.Entry<String, SkinData> entry = allSkins.get(i);
            String fullKey = entry.getKey();
            String[] parts = fullKey.split(":", 2);
            String type    = parts[0];
            String key     = parts.length > 1 ? parts[1] : fullKey;

            String displayName = type.equals("preset")
                ? skinManager.getPresetDisplayName(key)
                : skinManager.getLocalDisplayName(key);

            ItemStack skinItem = makeSkinItem(entry.getValue(), displayName, type, key, targetNpcId);
            inv.setItem(skinSlots[slotIdx], skinItem);
        }

        player.openInventory(inv);
    }



    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (!e.getAction().name().contains("RIGHT_CLICK")) return;
        ItemStack item = e.getItem();
        if (!isOpenerItem(item)) return;
        e.setCancelled(true);
        openMenu(e.getPlayer(), 0);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!isNpcfantGUI(e.getView().getTitle())) return;

        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;


        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }


        if (clicked.getType() == Material.ARROW) {
            int curPage = playerPages.getOrDefault(player.getUniqueId(), 0);
            String name = meta.getDisplayName();
            int nextPage = name.contains("Anterior") ? curPage - 1 : curPage + 1;
            UUID tId = targetNpcMap.get(player.getUniqueId());
            NPCEntity targetNpc = tId != null ? (NPCEntity) plugin.getNpcManager().getNPC(tId) : null;
            if (targetNpc != null) {
                openMenuForNPC(player, targetNpc, nextPage);
            } else {
                openMenu(player, nextPage);
            }
            return;
        }


        if (!meta.getPersistentDataContainer().has(keySkinKey, PersistentDataType.STRING)) return;

        String skinKey  = meta.getPersistentDataContainer().get(keySkinKey,  PersistentDataType.STRING);
        String skinType = meta.getPersistentDataContainer().get(keySkinType, PersistentDataType.STRING);
        if (skinKey == null || skinType == null) return;


        NPCManager nm = plugin.getNpcManager();
        NPCEntity npc = null;
        String stampedId = meta.getPersistentDataContainer().get(keyTargetNpc, PersistentDataType.STRING);
        UUID targetId = null;
        if (stampedId != null) {
            try { targetId = UUID.fromString(stampedId); } catch (Exception ignored) {}
        }
        if (targetId == null) {
            targetId = targetNpcMap.get(player.getUniqueId());
        }
        if (targetId != null) {
            npc = (NPCEntity) nm.getNPC(targetId);
        }
        if (npc == null) {
            Optional<NPCEntity> closest = nm.getNearestNPC(player.getLocation(), INTERACTION_DIST);
            if (closest.isPresent()) {
                npc = closest.get();
            } else if (nm.getAllNPCs().size() == 1) {
                npc = (NPCEntity) nm.getAllNPCs().iterator().next();
            }
        }
        if (npc == null) {
            if (!nm.getAllNPCs().isEmpty() && nm.getSelectorGUI() != null) {
                player.sendMessage(ChatColor.YELLOW + "[NpcFant] Selecciona qué NPC recibirá la skin:");
                nm.getSelectorGUI().openGUI(player);
                return;
            }
            player.sendMessage(ChatColor.RED + "[NpcFant] No hay ningún NPC disponible para aplicar skins.");
            player.closeInventory();
            return;
        }
        targetNpcMap.put(player.getUniqueId(), npc.getId());
        player.sendMessage(ChatColor.YELLOW + "[NpcFant] Aplicando skin '" +
            skinKey + "' al NPC '" + npc.getName() + "'...");
        player.closeInventory();

        nm.applySkinAsync(npc.getId(), skinType, skinKey, player);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!isNpcfantGUI(e.getView().getTitle())) return;
        playerPages.remove(e.getPlayer().getUniqueId());
        playerSkinList.remove(e.getPlayer().getUniqueId());
    }



    private boolean isNpcfantGUI(String title) {
        return title != null && title.startsWith("§8§l✦ §b§lEditor de Skins");
    }

    private int[] buildSkinSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                slots.add(row * 9 + col);
            }
        }
        int[] arr = new int[slots.size()];
        for (int i = 0; i < slots.size(); i++) arr[i] = slots.get(i);
        return arr;
    }

    private ItemStack makeBorderItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack makeNavButton(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeInfoButton(int total, int page, int totalPages) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "✦ Editor de Skins");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Total de skins: " + ChatColor.WHITE + total,
                ChatColor.GRAY + "Página: " + ChatColor.WHITE + (page + 1) + "/" + totalPages,
                "",
                ChatColor.YELLOW + "Haz clic en una skin para",
                ChatColor.YELLOW + "aplicarla al NPC más cercano."
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeSkinItem(SkinData skin, String displayName,
                                   String type, String key, UUID targetNpcId) {
        ItemStack item;

        try {
            item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            if (skullMeta != null && skin != null && skin.getValue() != null
                    && !skin.getValue().isEmpty()) {
                PlayerProfile profile = Bukkit.createPlayerProfile(
                    UUID.randomUUID(), displayName.replaceAll("§.", ""));
                PlayerTextures textures = profile.getTextures();

                try {
                    String decoded = new String(Base64.getDecoder().decode(skin.getValue()));

                    int urlStart = decoded.indexOf("\"url\"");
                    if (urlStart >= 0) {
                        int q1 = decoded.indexOf('"', urlStart + 5) + 1;
                        int q2 = decoded.indexOf('"', q1);
                        String url = decoded.substring(q1, q2);
                        textures.setSkin(new URL(url));
                        profile.setTextures(textures);
                        skullMeta.setOwnerProfile(profile);
                    }
                } catch (Exception ignored) {  }
                item.setItemMeta(skullMeta);
            }
        } catch (Exception e) {
            item = new ItemStack(Material.PLAYER_HEAD);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String badge = type.equals("preset") ? ChatColor.AQUA + "[Predefinida]"
                                             : ChatColor.LIGHT_PURPLE + "[PNG Local]";
        meta.setDisplayName(ChatColor.WHITE + displayName);
        meta.setLore(Arrays.asList(
            badge,
            ChatColor.GRAY + "Clic para aplicar al NPC seleccionado",
            "",
            ChatColor.DARK_GRAY + "Archivo: " + (type.equals("preset") ? key : key + ".png")
        ));

        meta.getPersistentDataContainer().set(keySkinKey,  PersistentDataType.STRING, key);
        meta.getPersistentDataContainer().set(keySkinType, PersistentDataType.STRING, type);
        if (targetNpcId != null) {
            meta.getPersistentDataContainer().set(keyTargetNpc, PersistentDataType.STRING, targetNpcId.toString());
        }

        item.setItemMeta(meta);
        return item;
    }
}
