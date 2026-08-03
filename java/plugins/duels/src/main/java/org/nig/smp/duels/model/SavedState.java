package org.nig.smp.duels.model;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class SavedState {

    private final Location location;
    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final ItemStack offhand;
    private final double health;
    private final int food;
    private final float saturation;
    private final float exp;
    private final int level;
    private final GameMode gameMode;

    public SavedState(Player player) {
        this.location = player.getLocation().clone();
        this.contents = player.getInventory().getContents();
        this.armor = player.getInventory().getArmorContents();
        this.offhand = player.getInventory().getItemInOffHand();
        this.health = player.getHealth();
        this.food = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.exp = player.getExp();
        this.level = player.getLevel();
        this.gameMode = player.getGameMode();
    }

    public void apply(Player player) {
        player.getInventory().setContents(contents);
        player.getInventory().setArmorContents(armor);
        player.getInventory().setItemInOffHand(offhand);
        player.setHealth(Math.max(1.0, Math.min(health, player.getMaxHealth())));
        player.setFoodLevel(food);
        player.setSaturation(saturation);
        player.setExp(exp);
        player.setLevel(level);
        player.setGameMode(gameMode);
        player.setFallDistance(0);
        player.teleport(location);
    }
}
