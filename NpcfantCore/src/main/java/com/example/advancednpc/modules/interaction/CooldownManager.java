package com.example.advancednpc.modules.interaction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {


    private final Map<String, Long> cooldowns = new HashMap<>();
    private final long defaultCooldownMs;

    public CooldownManager(long defaultCooldownSeconds) {
        this.defaultCooldownMs = defaultCooldownSeconds * 1000L;
    }


    public boolean canInteract(UUID playerId, UUID npcId) {
        String key = playerId + "_" + npcId;
        Long last  = cooldowns.get(key);
        if (last == null) return true;
        return (System.currentTimeMillis() - last) >= defaultCooldownMs;
    }


    public void registerInteraction(UUID playerId, UUID npcId) {
        cooldowns.put(playerId + "_" + npcId, System.currentTimeMillis());
    }


    public long getRemainingSeconds(UUID playerId, UUID npcId) {
        String key = playerId + "_" + npcId;
        Long last  = cooldowns.get(key);
        if (last == null) return 0;
        long elapsed = System.currentTimeMillis() - last;
        long remaining = defaultCooldownMs - elapsed;
        return Math.max(0, remaining / 1000L);
    }


    public void resetCooldown(UUID playerId, UUID npcId) {
        cooldowns.remove(playerId + "_" + npcId);
    }


    public void clearAll() {
        cooldowns.clear();
    }
}
