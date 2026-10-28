package com.example.advancednpc.modules.hologram;

import com.example.advancednpc.api.IHologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class Hologram implements IHologram {

    private static final double LINE_HEIGHT = 0.3;

    private final UUID   id;
    private final Plugin plugin;
    private Location     baseLocation;
    private final List<HologramLine> lines  = new ArrayList<>();
    private final List<ArmorStand>   stands = new ArrayList<>();
    private final Set<UUID>          hiddenFrom = new HashSet<>();
    private boolean alive = true;

    public Hologram(UUID id, Plugin plugin, Location location) {
        this.id           = id;
        this.plugin       = plugin;
        this.baseLocation = location.clone();
    }

    @Override public UUID getId() { return id; }

    @Override
    public void addLine(String text) {
        lines.add(new HologramLine(text));
        scheduleRefresh();
    }

    @Override
    public void setLine(int index, String text) {
        if (index < 0 || index >= lines.size()) return;
        lines.get(index).setText(text);
        if (index < stands.size()) {
            ArmorStand stand = stands.get(index);
            if (stand != null && !stand.isDead()) {
                stand.setCustomName(lines.get(index).getDisplayText());
            }
        }
    }

    @Override
    public void removeLine(int index) {
        if (index < 0 || index >= lines.size()) return;
        lines.remove(index);
        scheduleRefresh();
    }

    @Override
    public List<String> getLines() {
        List<String> texts = new ArrayList<>();
        for (HologramLine l : lines) texts.add(l.getRawText());
        return texts;
    }

    @Override public Location getLocation() { return baseLocation.clone(); }

    @Override
    public void move(Location location) {
        this.baseLocation = location.clone();
        scheduleRefresh();
    }

    @Override
    public void showTo(Player player) {
        hiddenFrom.remove(player.getUniqueId());
        for (ArmorStand stand : stands) {
            if (stand != null && !stand.isDead()) {
                player.showEntity(plugin, stand);
            }
        }
    }

    @Override
    public void hideFrom(Player player) {
        hiddenFrom.add(player.getUniqueId());
        for (ArmorStand stand : stands) {
            if (stand != null && !stand.isDead()) {
                player.hideEntity(plugin, stand);
            }
        }
    }

    @Override
    public void delete() {
        alive = false;
        if (Bukkit.isPrimaryThread()) {
            despawnAll();
        } else {
            Bukkit.getScheduler().runTask(plugin, this::despawnAll);
        }
    }

    @Override public boolean isAlive() { return alive; }




    public void scheduleRefresh() {
        if (Bukkit.isPrimaryThread()) {
            refresh();
        } else {
            Bukkit.getScheduler().runTask(plugin, this::refresh);
        }
    }


    public void refresh() {
        despawnAll();
        if (!alive) return;


        if (baseLocation.getWorld() == null) {
            plugin.getLogger().warning("[NpcFant] Hologram.refresh(): world es null en "
                + baseLocation + ". Postponiendo spawn.");
            return;
        }

        int totalLines = lines.size();
        for (int i = 0; i < totalLines; i++) {
            double yOffset  = (totalLines - 1 - i) * LINE_HEIGHT;
            Location lineLoc = baseLocation.clone().add(0, yOffset, 0);
            final String lineText = lines.get(i).getDisplayText();

            ArmorStand stand = baseLocation.getWorld().spawn(lineLoc, ArmorStand.class, as -> {
                as.setVisible(false);
                as.setGravity(false);
                as.setSmall(true);
                as.setMarker(true);
                as.setCustomNameVisible(true);
                as.setCustomName(lineText);
                as.setCollidable(false);
                as.setInvulnerable(true);
            });
            stands.add(stand);


            for (Player p : baseLocation.getWorld().getPlayers()) {
                if (hiddenFrom.contains(p.getUniqueId())) {
                    p.hideEntity(plugin, stand);
                }
            }
        }
    }

    private void despawnAll() {
        for (ArmorStand stand : stands) {
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        stands.clear();
    }

    public Set<UUID> getHiddenFrom() { return Collections.unmodifiableSet(hiddenFrom); }
}
