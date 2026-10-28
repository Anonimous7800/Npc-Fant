package com.example.advancednpc.modules.npc;

public enum NPCType {

    PLAYER,


    VILLAGER,
    WANDERING_TRADER,
    COW,
    PIG,
    SHEEP,
    CHICKEN,
    HORSE,
    DONKEY,
    MULE,
    LLAMA,
    CAT,
    DOG,
    RABBIT,
    TURTLE,
    AXOLOTL,
    FROG,
    SNIFFER,
    ALLAY,


    WOLF,
    POLAR_BEAR,
    BEE,
    GOAT,
    PANDA,
    FOX,
    IRON_GOLEM,
    SNOWMAN,


    ZOMBIE,
    SKELETON,
    CREEPER,
    SPIDER,
    ENDERMAN,
    WITCH,
    PILLAGER,
    VINDICATOR,
    EVOKER,
    PHANTOM,
    DROWNED,
    HUSK,
    STRAY,
    WARDEN,
    BREEZE,


    ENDER_DRAGON,
    WITHER,
    ELDER_GUARDIAN,


    BLAZE,
    GHAST,
    MAGMA_CUBE,
    PIGLIN,
    PIGLIN_BRUTE,
    ZOMBIFIED_PIGLIN,
    STRIDER,
    HOGLIN,
    ZOGLIN,


    SQUID,
    GLOW_SQUID,
    FISH_COD,
    FISH_SALMON,
    DOLPHIN,
    GUARDIAN;


    public boolean isPlayer() {
        return this == PLAYER;
    }
}
