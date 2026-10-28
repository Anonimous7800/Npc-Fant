package com.example.advancednpc.nms;

import com.example.advancednpc.api.INPC;
import com.example.advancednpc.api.NPCAdapter;
import org.bukkit.Location;

public class NMSAdapter_v1_19_R3 extends NPCAdapter {

    @Override
    public INPC spawnNPC(String name, Location location) {

        throw new UnsupportedOperationException("Adaptador NMS aÃƒÂºn no implementado para v1_19_R3");
    }

    @Override
    public boolean isSupported() {
        String serverVersion = org.bukkit.Bukkit.getServer().getClass()
                .getPackage().getName().split("\\.")[3];
        return serverVersion.equals("v1_19_R3");
    }
}

