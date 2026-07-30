package org.nig.smp.settings;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SettingsGUI implements Listener {

    private final SettingsManager manager;
    private final MessageConfig msg;

    public SettingsGUI(SettingsManager manager, MessageConfig msg) {
        this.manager = manager;
        this.msg = msg;
    }

    public void open(Player player) {
        SettingType[] types = SettingType.values();
        int size = ((types.length / 9) + 1) * 9;
        if (size < 9) size = 9;
        if (size > 54) size = 54;

        Inventory inv = Bukkit.createInventory(null, size, msg.menuTitle());

        for (int i = 0; i < types.length && i < size; i++) {
            inv.setItem(i, createToggleItem(player, types[i]));
        }

        player.openInventory(inv);
    }

    private ItemStack createToggleItem(Player player, SettingType type) {
        boolean enabled = manager.isEnabled(player, type);
        ItemStack item = new ItemStack(type.getIcon());
        ItemMeta meta = item.getItemMeta();

        Component name = msg.legacy(
                (enabled ? "&a" : "&c") + type.getDisplayName()
        ).decoration(TextDecoration.ITALIC, false);

        List<Component> lore = new ArrayList<>();
        lore.add(msg.legacy("&7" + msg.description(type))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(msg.legacy(enabled ? msg.enabled() : msg.disabled())
                .decoration(TextDecoration.ITALIC, false));
        lore.add(msg.legacy("&8" + msg.clickToToggle())
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        meta.itemName(name);
        item.setItemMeta(meta);

        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(msg.menuTitle())) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        int slot = event.getSlot();
        SettingType[] types = SettingType.values();
        if (slot < 0 || slot >= types.length) return;

        SettingType type = types[slot];
        manager.toggle(player, type);

        event.getInventory().setItem(slot, createToggleItem(player, type));

        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
    }
}
