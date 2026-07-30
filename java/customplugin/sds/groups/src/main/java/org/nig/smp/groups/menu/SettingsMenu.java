package org.nig.smp.groups.menu;

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
import org.nig.smp.groups.Groups;

import java.util.ArrayList;
import java.util.List;

public class SettingsMenu implements InventoryHolder, Listener {

    private final Groups plugin;
    private final Inventory inventory;

    private static final int TOGGLE_SLOT = 11;
    private static final int RELOAD_SLOT = 15;
    private static final int INFO_SLOT = 22;

    public SettingsMenu(Groups plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 27,
                Component.text("§8⚙ Настройки донатов"));
        Bukkit.getPluginManager().registerEvents(this, plugin);
        draw();
    }

    private void draw() {
        fillBorders();

        boolean enabled = plugin.getCfg().isEnabled();

        // Toggle on/off
        inventory.setItem(TOGGLE_SLOT, createItem(
                enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                (enabled ? "§a§l✅ СИСТЕМА ВКЛЮЧЕНА" : "§c§l❌ СИСТЕМА ВЫКЛЮЧЕНА"),
                "§7Нажмите, чтобы " + (enabled ? "выключить" : "включить"),
                ""
        ));

        // Reload config
        inventory.setItem(RELOAD_SLOT, createItem(
                Material.COMPARATOR,
                "§e§l🔄 ПЕРЕЗАГРУЗИТЬ КОНФИГ",
                "§7Перезагрузить config.yml",
                "§7с сохранением всех изменений"
        ));

        // Info
        inventory.setItem(INFO_SLOT, createItem(
                Material.BOOK,
                "§6§l📋 ИНФОРМАЦИЯ",
                "§7Статус: " + (enabled ? "§a✓ Включено" : "§c✗ Выключено"),
                "§7Текст без доната: §f" + plugin.getCfg().getNoDonationText()
        ));
    }

    private void fillBorders() {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, "§r", "");
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i >= 18 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, border);
            }
        }
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            if (lore.length > 0) {
                List<Component> loreComponents = new ArrayList<>();
                for (String line : lore) {
                    if (!line.isEmpty()) {
                        loreComponents.add(Component.text(line));
                    }
                }
                meta.lore(loreComponents);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof SettingsMenu)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.hasPermission("donate.admin")) {
            player.sendMessage(plugin.getCfg().msg("no-permission"));
            player.closeInventory();
            return;
        }

        int slot = event.getSlot();
        if (slot == TOGGLE_SLOT) {
            boolean current = plugin.getCfg().isEnabled();
            plugin.getCfg().setEnabled(!current);
            player.sendMessage("§a✅ Система донатов " + (!current ? "§aвключена" : "§cвыключена"));
            draw();
            player.updateInventory();
        } else if (slot == RELOAD_SLOT) {
            plugin.getCfg().reload();
            player.sendMessage(plugin.getCfg().msg("config-reloaded"));
            draw();
            player.updateInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder(false) instanceof SettingsMenu) {
            HandlerList.unregisterAll(this);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
