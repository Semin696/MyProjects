package org.nig.smp.settings;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

public class SettingsListener implements Listener {

    private final SettingsManager manager;
    @SuppressWarnings("unused")
    private final MessageConfig msg;

    public SettingsListener(SettingsManager manager, MessageConfig msg) {
        this.manager = manager;
        this.msg = msg;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        applyEffects(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        removeEffects(player);
    }

    public void applyEffects(Player player) {
        Map<SettingType, Boolean> settings = manager.getSettings(player);

        for (Map.Entry<SettingType, Boolean> entry : settings.entrySet()) {
            if (entry.getValue()) {
                applySetting(player, entry.getKey());
            } else {
                removeSetting(player, entry.getKey());
            }
        }
    }

    public void applySetting(Player player, SettingType type) {
        switch (type) {
            case NIGHT_VISION:
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.NIGHT_VISION,
                        -1,
                        0, false, false, true
                ));
                break;
            case FLY_SPEED:
                player.setFlySpeed(0.2f);
                break;
            case WALK_SPEED:
                player.setWalkSpeed(0.3f);
                break;
            case AUTO_FISH:
                break;
        }
    }

    public void removeSetting(Player player, SettingType type) {
        switch (type) {
            case NIGHT_VISION:
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                break;
            case FLY_SPEED:
                player.setFlySpeed(0.1f);
                break;
            case WALK_SPEED:
                player.setWalkSpeed(0.2f);
                break;
            case AUTO_FISH:
                break;
        }
    }

    private void removeEffects(Player player) {
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.setFlySpeed(0.1f);
        player.setWalkSpeed(0.2f);
    }
}
