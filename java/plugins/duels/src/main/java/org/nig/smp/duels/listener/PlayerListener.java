package org.nig.smp.duels.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
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
        if (plugin.isCompassWorld(player.getWorld())) {
            giveCompass(player);
        }
        Bukkit.getScheduler().runTask(plugin, visibilityManager::refresh);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isCompassWorld(player.getWorld())) {
            return;
        }
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
        if (plugin.getArenaManager().getArenas().isEmpty()) {
            player.sendMessage(plugin.msg("no-arenas"));
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
            event.getDrops().clear();
            event.setDeathMessage(null);
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
        if (plugin.isCompassWorld(player.getWorld())) {
            player.setGameMode(plugin.getDuelsGameMode());
            giveCompass(player);
        } else {
            removeCompass(player);
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
            && event.getFrom().getBlockY() == event.getTo().getBlockY()
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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() != Material.COMPASS || !isCompass(event.getCurrentItem())) {
            return;
        }
        if (plugin.isCompassWorld(player.getWorld())) {
            event.setCancelled(true);
        }
    }

    private void giveCompass(Player player) {
        player.getInventory().setItem(0, createCompass());
    }

    private void removeCompass(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() == Material.COMPASS && isCompass(item)) {
                inventory.setItem(i, null);
            }
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
            if (meta instanceof CompassMeta compassMeta) {
                compassMeta.setLodestoneTracked(false);
            }
            compass.setItemMeta(meta);
        }
        return compass;
    }
}
