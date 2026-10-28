package com.example.advancednpc.nms;

import com.example.advancednpc.api.INPC;
import com.example.advancednpc.api.NPCAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class NMSAdapter_v26_2_R1 extends NPCAdapter {

    @Override
    public INPC spawnNPC(String name, Location location) {

        throw new UnsupportedOperationException(
            "Adaptador NMS no implementado aun para v26_2_R1. Compila con BuildTools primero.");
    }

    @Override
    public boolean isSupported() {
        try {
            String ver = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            return ver.equals("v26_2_R1");
        } catch (Exception e) {

            String minecraftVersion = Bukkit.getServer().getBukkitVersion();
            return minecraftVersion.startsWith("26.2-R0.1-SNAPSHOT".replace("-R0.1-SNAPSHOT","").replace("-SNAPSHOT",""));
        }
    }
}

