package com.example.advancednpc.modules.interaction;

import com.example.advancednpc.api.action.INPCAction;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class PlaySoundAction implements INPCAction {

    public static final String TYPE = "PLAY_SOUND";

    private final String soundName;
    private final float  volume;
    private final float  pitch;


    public PlaySoundAction(String soundName, float volume, float pitch) {
        this.soundName = soundName.toUpperCase();
        this.volume    = volume;
        this.pitch     = pitch;
    }

    @Override
    public String getType() { return TYPE; }

    @Override
    public void execute(Player player, UUID npcId) {
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {

            player.getServer().getLogger().warning(
                    "[NpcFant] PlaySoundAction: sonido desconocido '" + soundName + "'");
        }
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of(
                "type",   TYPE,
                "sound",  soundName,
                "volume", volume,
                "pitch",  pitch
        );
    }

    public String getSoundName() { return soundName; }
    public float  getVolume()    { return volume; }
    public float  getPitch()     { return pitch; }
}
