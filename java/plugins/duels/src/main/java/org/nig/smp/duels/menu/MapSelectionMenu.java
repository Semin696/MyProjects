package org.nig.smp.duels.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nig.smp.duels.DuelsPlugin;
import org.nig.smp.duels.model.Arena;
import org.nig.smp.duels.model.DuelMatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MapSelectionMenu implements InventoryHolder, Listener {

    private final DuelsPlugin plugin;
    private final Player player;
    private final DuelMatch match;
    private final Inventory inventory;
    private final Map<Integer, String> slotArena = new HashMap<>();
    private boolean selected;

    public MapSelectionMenu(DuelsPlugin plugin, Player player, DuelMatch match) {
        this.plugin = plugin;
        this.player = player;
        this.match = match;

        List<Arena> arenas = plugin.getArenaManager().getArenas().stream()
            .filter(a -> isArenaCompatible(a))
            .toList();
        int rows = Math.max(1, (arenas.size() + 8) / 9);
        int size = Math.min(54, Math.max(9, rows * 9));
        this.inventory = Bukkit.createInventory(this, size, Component.text(plugin.raw("menu-title-map")));
        Bukkit.getPluginManager().registerEvents(this, plugin);
        draw(arenas);
    }

    private boolean isArenaCompatible(Arena arena) {
        boolean destructive = false;
        for (UUID id : match.players()) {
            String kit = match.getKit(id);
            if (kit != null && plugin.getKitManager().isDestructive(kit)) {
                destructive = true;
            }
        }
        return destructive == arena.isBlockBreakAllowed();
    }

    private void draw(List<Arena> arenas) {
        if (arenas.isEmpty()) {
            inventory.setItem(4, createItem(Material.BARRIER, plugin.msg("no-compatible-arenas")));
            return;
        }
        int slot = 0;
        for (Arena arena : arenas) {
            if (slot >= inventory.getSize()) {
                break;
            }
            ItemStack icon = createItem(Material.GRASS_BLOCK, plugin.msg("arena-name", "arena", arena.getDisplayName()));
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.lore(List.of(plugin.msg("arena-lore")));
                icon.setItemMeta(meta);
            }
            inventory.setItem(slot, icon);
            slotArena.put(slot, arena.getName());
            slot++;
        }
    }

    private ItemStack createItem(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof MapSelectionMenu menu)) {
            return;
        }
        if (menu != this) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player who)) {
            return;
        }
        if (!who.getUniqueId().equals(player.getUniqueId())) {
            return;
        }
        String arena = slotArena.get(event.getSlot());
        if (arena == null) {
            return;
        }
        if (plugin.getDuelManager().selectMapForChallenge(who, arena)) {
            selected = true;
            who.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder(false) instanceof MapSelectionMenu menu && menu == this) {
            if (!selected) {
                plugin.getDuelManager().cancelAfterMenuClose(player);
            }
            HandlerList.unregisterAll(this);
        }
    }
}
