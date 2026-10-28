package com.example.advancednpc.nms;

import com.example.advancednpc.api.INPC;
import com.example.advancednpc.api.NPCAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class NMSAdapter_v1_21_R5 extends NPCAdapter {

    @Override
    public INPC spawnNPC(String name, Location location) {

        throw new UnsupportedOperationException(
            "Adaptador NMS no implementado aun para v1_21_R5. Compila con BuildTools primero.");
    }

    @Override
    public boolean isSupported() {
        try {
            String ver = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            return ver.equals("v1_21_R5");
        } catch (Exception e) {

            String minecraftVersion = Bukkit.getServer().getBukkitVersion();
            return minecraftVersion.startsWith("1.21.11-R0.1-SNAPSHOT".replace("-R0.1-SNAPSHOT","").replace("-SNAPSHOT",""));
        }
    }
}

