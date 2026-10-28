package com.example.advancednpc.api;

import com.example.advancednpc.api.action.INPCAction;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class NPCBuilder {

    private String name = "NPC";
    private EntityType entityType = EntityType.PLAYER;
    private Location location;
    private String skinUsername;
    private String skinUUID;
    private String skinURL;
    private String skinFile;
    private Pose pose = Pose.STANDING;
    private String glowColor;
    private boolean invisible = false;
    private boolean lookAtPlayer = true;
    private final List<String> hologramLines = new ArrayList<>();
    private final List<INPCAction> actions = new ArrayList<>();

    public enum Pose {
        STANDING, SITTING, SNEAKING, SLEEPING
    }

    public NPCBuilder name(String name) {
        this.name = name;
        return this;
    }

    public NPCBuilder type(EntityType type) {
        this.entityType = type;
        return this;
    }

    public NPCBuilder location(Location location) {
        this.location = location;
        return this;
    }

    public NPCBuilder skinByUsername(String username) {
        this.skinUsername = username;
        return this;
    }

    public NPCBuilder skinByUUID(String uuid) {
        this.skinUUID = uuid;
        return this;
    }

    public NPCBuilder skinByURL(String url) {
        this.skinURL = url;
        return this;
    }

    public NPCBuilder skinByFile(String filePath) {
        this.skinFile = filePath;
        return this;
    }

    public NPCBuilder pose(Pose pose) {
        this.pose = pose;
        return this;
    }

    public NPCBuilder glowColor(String color) {
        this.glowColor = color;
        return this;
    }

    public NPCBuilder invisible(boolean invisible) {
        this.invisible = invisible;
        return this;
    }

    public NPCBuilder lookAtPlayer(boolean look) {
        this.lookAtPlayer = look;
        return this;
    }

    public NPCBuilder hologramLine(String line) {
        this.hologramLines.add(line);
        return this;
    }

    public NPCBuilder action(INPCAction action) {
        this.actions.add(action);
        return this;
    }

    public String getName() { return name; }
    public EntityType getEntityType() { return entityType; }
    public Location getLocation() { return location; }
    public String getSkinUsername() { return skinUsername; }
    public String getSkinUUID() { return skinUUID; }
    public String getSkinURL() { return skinURL; }
    public String getSkinFile() { return skinFile; }
    public Pose getPose() { return pose; }
    public String getGlowColor() { return glowColor; }
    public boolean isInvisible() { return invisible; }
    public boolean isLookAtPlayer() { return lookAtPlayer; }
    public List<String> getHologramLines() { return hologramLines; }
    public List<INPCAction> getActions() { return actions; }

    public INPC build() {
        if (location == null) {
            throw new IllegalStateException("Location no puede ser null");
        }
        INPCManager manager = NPCAdapter.getActiveManager();
        if (manager == null) {
            throw new IllegalStateException("NpcFant no esta inicializado aun");
        }
        return manager.createNPC(name, location);
    }
}
