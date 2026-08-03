package org.nig.smp.duels.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.nig.smp.duels.DuelsPlugin;
import org.nig.smp.duels.manager.DuelManager;
import org.nig.smp.duels.manager.VisibilityManager;
import org.nig.smp.duels.menu.KitSelectionMenu;

public final class PlayerListener implements Listener {

    private final DuelsPlugin plugin;
    private final DuelManager duelManager;
    private final VisibilityManager visibilityManager;

    public PlayerListener(DuelsPlugin plugin, DuelManager duelManager, VisibilityManager visibilityManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
        this.visibilityManager = visibilityManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.getInventory().setItem(0, createCompass());
        Bukkit.getScheduler().runTask(plugin, visibilityManager::refresh);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.COMPASS || !isCompass(item)) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        event.setCancelled(true);
        if (duelManager.isBusy(player.getUniqueId())) {
            player.sendMessage(plugin.msg("already-in-duel"));
            return;
        }
        new KitSelectionMenu(plugin, player).open();
    }

    @EventHandler
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target)) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        event.setCancelled(true);
        duelManager.createDirectChallenge(player, target);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (duelManager.isInActiveDuel(player.getUniqueId())) {
            event.setCancelled(true);
            duelManager.onPlayerDeath(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        duelManager.onPlayerQuit(event.getPlayer());
        Bukkit.getScheduler().runTask(plugin, visibilityManager::refresh);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (plugin.isDuelsWorld(player.getWorld())) {
            player.setGameMode(plugin.getDuelsGameMode());
        }
        Bukkit.getScheduler().runTask(plugin, visibilityManager::refresh);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!duelManager.isWaiting(player.getUniqueId())) {
            return;
        }
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getType() == Material.COMPASS && isCompass(item)) {
            event.setCancelled(true);
        }
    }

    private boolean isCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(
            new NamespacedKey(plugin, "duels_compass"),
            PersistentDataType.BOOLEAN
        );
    }

    private ItemStack createCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(plugin.raw("compass-name")));
            meta.lore(java.util.List.of(Component.text(plugin.raw("compass-lore"))));
            meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "duels_compass"),
                PersistentDataType.BOOLEAN,
                true
            );
            compass.setItemMeta(meta);
        }
        return compass;
    }
}
