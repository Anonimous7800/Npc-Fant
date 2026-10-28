package com.example.advancednpc.modules.animation;

public enum NPCPose {


    STANDING,


    SNEAKING,


    SITTING,


    SLEEPING,


    SWIMMING,


    SPIN_ATTACK,


    DYING;


    public boolean affectsHitbox() {
        return this == SNEAKING || this == SLEEPING || this == SWIMMING;
    }
}
