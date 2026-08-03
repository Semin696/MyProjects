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
import org.nig.smp.duels.cmi.CMIKitBridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class KitSelectionMenu implements InventoryHolder, Listener {

    private enum Context {
        CHALLENGE, MATCHMAKING
    }

    private final DuelsPlugin plugin;
    private final Player player;
    private final Context context;
    private final Player opponent;
    private final Inventory inventory;
    private final Map<Integer, String> slotKit = new HashMap<>();

    public KitSelectionMenu(DuelsPlugin plugin, Player player, Player opponent) {
        this(plugin, player, Context.CHALLENGE, opponent);
    }

    public KitSelectionMenu(DuelsPlugin plugin, Player player) {
        this(plugin, player, Context.MATCHMAKING, null);
    }

    private KitSelectionMenu(DuelsPlugin plugin, Player player, Context context, Player opponent) {
        this.plugin = plugin;
        this.player = player;
        this.context = context;
        this.opponent = opponent;

        List<String> kits = new ArrayList<>(CMIKitBridge.getKitNames());
        int rows = Math.max(1, (kits.size() + 8) / 9);
        int size = Math.min(54, Math.max(9, rows * 9));
        this.inventory = Bukkit.createInventory(this, size, Component.text(plugin.raw("menu-title-kit")));
        Bukkit.getPluginManager().registerEvents(this, plugin);
        draw(kits);
    }

    private void draw(List<String> kits) {
        if (kits.isEmpty()) {
            inventory.setItem(4, createItem(Material.BARRIER, plugin.msg("no-kits")));
            return;
        }
        int slot = 0;
        for (String kit : kits) {
            if (slot >= inventory.getSize()) {
                break;
            }
            ItemStack icon = icon(kit);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(plugin.msg("kit-name", "kit", kit));
                List<Component> lore = new ArrayList<>();
                lore.add(plugin.msg("kit-players", "count", plugin.getDuelManager().countPlayersByKit(kit)));
                if (context == Context.CHALLENGE && opponent != null) {
                    lore.add(plugin.msg("kit-opponent", "player", opponent.getName()));
                }
                meta.lore(lore);
                icon.setItemMeta(meta);
            }
            inventory.setItem(slot, icon);
            slotKit.put(slot, kit);
            slot++;
        }
    }

    private ItemStack icon(String kit) {
        List<ItemStack> items = CMIKitBridge.getKitItems(kit);
        if (items != null && !items.isEmpty() && items.get(0) != null) {
            return items.get(0).clone();
        }
        return new ItemStack(Material.CHEST);
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
        if (!(event.getInventory().getHolder(false) instanceof KitSelectionMenu menu)) {
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
        String kit = slotKit.get(event.getSlot());
        if (kit == null) {
            return;
        }
        who.closeInventory();
        if (context == Context.CHALLENGE) {
            plugin.getDuelManager().selectKitForChallenge(who, kit, opponent);
        } else {
            plugin.getDuelManager().selectKitForMatchmaking(who, kit);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder(false) instanceof KitSelectionMenu menu && menu == this) {
            HandlerList.unregisterAll(this);
        }
    }
}
