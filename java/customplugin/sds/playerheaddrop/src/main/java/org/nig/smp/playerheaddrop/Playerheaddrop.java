package org.nig.smp.playerheaddrop;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class Playerheaddrop extends JavaPlugin implements Listener {

    private double dropChance;
    private boolean pvpEnabled;
    private boolean pveEnabled;
    private boolean otherEnabled;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfigValues();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
    }

    private void reloadConfigValues() {
        dropChance = getConfig().getDouble("drop-chance", 1.0);
        pvpEnabled = getConfig().getBoolean("pvp", true);
        pveEnabled = getConfig().getBoolean("pve", true);
        otherEnabled = getConfig().getBoolean("other", true);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (!canDrop(player)) return;
        if (Math.random() > dropChance) return;

        ItemStack head = createHead(player);
        player.getWorld().dropItemNaturally(player.getLocation(), head);
    }

    private boolean canDrop(Player player) {
        Player killer = player.getKiller();

        if (killer != null) return pvpEnabled;

        EntityDamageEvent damage = player.getLastDamageCause();
        if (damage != null) {
            return switch (damage.getCause()) {
                case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, PROJECTILE -> pveEnabled;
                default -> otherEnabled;
            };
        }

        return otherEnabled;
    }

    private ItemStack createHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            head.setItemMeta(meta);
        }
        return head;
    }
}
