package com.example.advancednpc.modules.equipment;

import org.bukkit.inventory.ItemStack;

public class NPCEquipment {

    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;
    private ItemStack mainHand;
    private ItemStack offHand;

    public NPCEquipment() {}


    public ItemStack getHelmet()     { return helmet; }
    public ItemStack getChestplate() { return chestplate; }
    public ItemStack getLeggings()   { return leggings; }
    public ItemStack getBoots()      { return boots; }
    public ItemStack getMainHand()   { return mainHand; }
    public ItemStack getOffHand()    { return offHand; }


    public NPCEquipment helmet(ItemStack item)     { this.helmet = item;     return this; }
    public NPCEquipment chestplate(ItemStack item) { this.chestplate = item; return this; }
    public NPCEquipment leggings(ItemStack item)   { this.leggings = item;   return this; }
    public NPCEquipment boots(ItemStack item)      { this.boots = item;      return this; }
    public NPCEquipment mainHand(ItemStack item)   { this.mainHand = item;   return this; }
    public NPCEquipment offHand(ItemStack item)    { this.offHand = item;    return this; }


    public enum Slot {
        HELMET, CHESTPLATE, LEGGINGS, BOOTS, MAIN_HAND, OFF_HAND;

        public static Slot fromString(String s) {
            return valueOf(s.toUpperCase().replace("-", "_").replace(" ", "_"));
        }
    }


    public ItemStack get(Slot slot) {
        return switch (slot) {
            case HELMET     -> helmet;
            case CHESTPLATE -> chestplate;
            case LEGGINGS   -> leggings;
            case BOOTS      -> boots;
            case MAIN_HAND  -> mainHand;
            case OFF_HAND   -> offHand;
        };
    }


    public void set(Slot slot, ItemStack item) {
        switch (slot) {
            case HELMET     -> this.helmet = item;
            case CHESTPLATE -> this.chestplate = item;
            case LEGGINGS   -> this.leggings = item;
            case BOOTS      -> this.boots = item;
            case MAIN_HAND  -> this.mainHand = item;
            case OFF_HAND   -> this.offHand = item;
        }
    }


    public boolean hasAnyEquipment() {
        return helmet != null || chestplate != null || leggings != null
                || boots != null || mainHand != null || offHand != null;
    }
}
