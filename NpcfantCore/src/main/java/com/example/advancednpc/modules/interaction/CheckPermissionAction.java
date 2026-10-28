package com.example.advancednpc.modules.interaction;

import com.example.advancednpc.api.action.INPCAction;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class CheckPermissionAction implements INPCAction {

    public static final String TYPE = "CHECK_PERMISSION";

    private final String      permission;
    private final INPCAction  ifTrue;
    private final INPCAction  ifFalse;


    public CheckPermissionAction(String permission, INPCAction ifTrue, INPCAction ifFalse) {
        this.permission = permission;
        this.ifTrue     = ifTrue;
        this.ifFalse    = ifFalse;
    }

    @Override
    public String getType() { return TYPE; }

    @Override
    public void execute(Player player, UUID npcId) {
        if (player.hasPermission(permission)) {
            if (ifTrue != null) ifTrue.execute(player, npcId);
        } else {
            if (ifFalse != null) ifFalse.execute(player, npcId);
        }
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of(
                "type",       TYPE,
                "permission", permission,
                "if-true",    ifTrue  != null ? ifTrue.serialize()  : Map.of(),
                "if-false",   ifFalse != null ? ifFalse.serialize() : Map.of()
        );
    }

    public String     getPermission() { return permission; }
    public INPCAction getIfTrue()     { return ifTrue; }
    public INPCAction getIfFalse()    { return ifFalse; }
}
