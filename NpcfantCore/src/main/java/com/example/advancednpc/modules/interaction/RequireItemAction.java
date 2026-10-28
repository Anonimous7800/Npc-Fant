package com.example.advancednpc.modules.interaction;

import com.example.advancednpc.api.action.INPCAction;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public class RequireItemAction implements INPCAction {

    public static final String TYPE = "REQUIRE_ITEM";
    private final Material material;
    private final int amount;
    private final boolean consume;
    private final String denyMessage;

    public RequireItemAction(Material material, int amount, boolean consume, String denyMessage) {
        this.material    = material;
        this.amount      = amount;
        this.consume     = consume;
        this.denyMessage = denyMessage;
    }

    @Override
    public String getType() { return TYPE; }

    @Override
    public void execute(Player player, UUID npcId) {
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType() != material || hand.getAmount() < amount) {
            String msg = (denyMessage != null && !denyMessage.isEmpty())
                    ? denyMessage.replace("&", "§")
                    : "§cNecesitas §e" + amount + "x " + material.name() + " §cpara interactuar.";
            player.sendMessage(msg);
            return;
        }

        if (consume) {
            hand.setAmount(hand.getAmount() - amount);
            player.getInventory().setItemInMainHand(hand.getAmount() <= 0 ? null : hand);
        }

    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of(
            "type",         TYPE,
            "material",     material.name(),
            "amount",       amount,
            "consume",      consume,
            "deny-message", denyMessage != null ? denyMessage : ""
        );
    }

    public Material getMaterial() { return material; }
    public int getAmount()        { return amount; }
    public boolean isConsume()    { return consume; }
}
