package com.example.advancednpc.modules.interaction;

import com.example.advancednpc.api.action.INPCAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class RandomAction implements INPCAction {

    public static final String TYPE = "RANDOM";
    private static final Random RANDOM = new Random();

    private final List<INPCAction> options;

    public RandomAction(List<INPCAction> options) {
        this.options = new ArrayList<>(options);
    }

    @Override
    public String getType() { return TYPE; }

    @Override
    public void execute(Player player, UUID npcId) {
        if (options.isEmpty()) return;
        INPCAction chosen = options.get(RANDOM.nextInt(options.size()));
        chosen.execute(player, npcId);
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of("type", TYPE, "options", options.stream()
                .map(INPCAction::serialize)
                .toList());
    }

    public List<INPCAction> getOptions() { return options; }
}
