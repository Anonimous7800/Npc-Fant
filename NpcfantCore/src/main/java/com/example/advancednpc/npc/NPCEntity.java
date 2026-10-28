package com.example.advancednpc.npc;

import com.example.advancednpc.api.INPC;
import com.example.advancednpc.modules.animation.NPCPose;
import com.example.advancednpc.modules.equipment.NPCEquipment;
import com.example.advancednpc.modules.interaction.ActionChain;
import com.example.advancednpc.modules.npc.FakePlayerNPC;
import com.example.advancednpc.modules.npc.NPCType;
import com.example.advancednpc.modules.skin.SkinData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NPCEntity implements INPC {

    private final UUID          id;
    private       String        name;
    private       Location      location;
    private       NPCType       type        = NPCType.PLAYER;
    private       NPCPose       pose        = NPCPose.STANDING;
    private       NPCEquipment  equipment   = new NPCEquipment();
    private       ActionChain   actionChain = new ActionChain();
    private       UUID          hologramId;


    private final FakePlayerNPC fakePlayer;


    private String  skinValue;
    private String  skinSignature;
    private String  glowColor;
    private boolean invisible         = false;
    private boolean showNametag       = true;
    private boolean lookAtPlayer      = true;
    private boolean spawned           = false;


    private List<String> hologramLines = new ArrayList<>();


    private boolean visibleToAll = true;
    private final java.util.Set<UUID> visibleTo = new java.util.HashSet<>();

    public NPCEntity(UUID id, String name, Location location, Plugin plugin, SkinData skin) {
        this.id        = id;
        this.name      = name;
        this.location  = location.clone();
        this.fakePlayer = new FakePlayerNPC(id, name, location, plugin);
        if (skin != null) {
            this.skinValue     = skin.getValue();
            this.skinSignature = skin.getSignature();
            this.fakePlayer.updateSkin(skin);
        }
        this.spawned = true;
    }


    @Override public UUID     getId()       { return id; }
    @Override public String   getName()     { return name; }
    @Override public Location getLocation() { return location.clone(); }
    @Override public boolean  isSpawned()   { return spawned; }

    @Override
    public void setName(String name) {
        this.name = name;
        fakePlayer.setName(name);
    }

    @Override
    public void teleport(Location location) {
        this.location = location.clone();
        fakePlayer.teleport(location);
    }

    @Override
    public void lookAtPlayer(Player player) {
        fakePlayer.lookAt(player);
    }

    @Override
    public void remove() {
        this.spawned = false;
        fakePlayer.despawnForAll();
    }


    public FakePlayerNPC getFakePlayer()       { return fakePlayer; }

    public NPCType      getType()              { return type; }
    public void         setType(NPCType t)     { this.type = t; }

    public NPCPose      getPose()              { return pose; }
    public void         setPose(NPCPose p)     { this.pose = p; }

    public NPCEquipment getEquipment()         { return equipment; }
    public void         setEquipment(NPCEquipment e) { this.equipment = e; }

    public ActionChain  getActionChain()       { return actionChain; }

    public UUID         getHologramId()        { return hologramId; }
    public void         setHologramId(UUID h)  { this.hologramId = h; }

    public String  getSkinValue()              { return skinValue; }
    public String  getSkinSignature()          { return skinSignature; }
    public void    setSkin(String value, String signature) {
        this.skinValue     = value;
        this.skinSignature = signature;
    }

    public String  getGlowColor()              { return glowColor; }
    public void    setGlowColor(String c)      { this.glowColor = c; }

    public boolean isInvisible()               { return invisible; }
    public void    setInvisible(boolean v)     { this.invisible = v; }

    public boolean isShowNametag()             { return showNametag; }
    public void    setShowNametag(boolean v)   { this.showNametag = v; }

    public boolean isLookAtPlayer()            { return lookAtPlayer; }
    public void    setLookAtPlayer(boolean v)  { this.lookAtPlayer = v; }

    public boolean isVisibleToAll()            { return visibleToAll; }
    public void    setVisibleToAll(boolean v)  { this.visibleToAll = v; }

    public java.util.Set<UUID> getVisibleTo()  { return visibleTo; }

    public List<String> getHologramLines()                { return hologramLines; }
    public void         setHologramLines(List<String> l)  { this.hologramLines = l; }
}
