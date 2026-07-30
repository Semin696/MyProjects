package org.nig.smp.impers.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.nig.smp.impers.ImpersPlugin;
import org.nig.smp.impers.manager.TerritoryManager;
import org.nig.smp.impers.model.ChunkSelection;
import org.nig.smp.impers.model.Territory;

import java.util.Map;
import java.util.UUID;

public class ImpersCommand implements CommandExecutor {

    private final ImpersPlugin plugin;
    private final TerritoryManager territoryManager;
    private final Map<UUID, ChunkSelection> selections;

    public ImpersCommand(ImpersPlugin plugin, TerritoryManager territoryManager, Map<UUID, ChunkSelection> selections) {
        this.plugin = plugin;
        this.territoryManager = territoryManager;
        this.selections = selections;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("stick")) {
            return giveStick(player);
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                return createTerritory(player, args);
            }
            case "list" -> {
                return listTerritories(player);
            }
            case "remove" -> {
                return removeTerritory(player, args);
            }
            default -> {
                player.sendMessage(Component.text("Usage: /imp [stick|create <name> <tag>|list|remove <name>]", NamedTextColor.RED));
                return true;
            }
        }
    }

    private boolean giveStick(Player player) {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        meta.displayName(Component.text("Выделение империи", NamedTextColor.GOLD));
        meta.lore(java.util.List.of(
            Component.text("ЛКМ - первая точка", NamedTextColor.GRAY),
            Component.text("ПКМ - вторая точка", NamedTextColor.GRAY)
        ));
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "impers_stick"),
            PersistentDataType.BOOLEAN,
            true
        );
        stick.setItemMeta(meta);
        player.getInventory().addItem(stick);
        player.sendMessage(Component.text("Вы получили палку выделения империи", NamedTextColor.GREEN));
        return true;
    }

    private boolean createTerritory(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /imp create <name> <tag>", NamedTextColor.RED));
            return true;
        }

        String name = args[1];
        String tag = args[2];

        if (territoryManager.exists(name)) {
            player.sendMessage(Component.text("Территория с таким именем уже существует", NamedTextColor.RED));
            return true;
        }

        ChunkSelection sel = selections.get(player.getUniqueId());
        if (sel == null || !sel.isComplete()) {
            player.sendMessage(Component.text("Сначала выделите территорию палкой", NamedTextColor.RED));
            return true;
        }

        Territory territory = new Territory(
            name, tag, player.getWorld().getName(),
            sel.getMinChunkX(), sel.getMinChunkZ(),
            sel.getMaxChunkX(), sel.getMaxChunkZ()
        );

        territoryManager.add(territory);
        selections.remove(player.getUniqueId());

        player.sendMessage(Component.text("Территория '" + name + "' создана! Чаunkов: " + sel.getSizeX() + "x" + sel.getSizeZ(), NamedTextColor.GREEN));
        return true;
    }

    private boolean listTerritories(Player player) {
        java.util.Collection<Territory> all = territoryManager.getAll();
        if (all.isEmpty()) {
            player.sendMessage(Component.text("Нет созданных территорий", NamedTextColor.YELLOW));
            return true;
        }
        player.sendMessage(Component.text("Территории:", NamedTextColor.GOLD));
        for (Territory t : all) {
            player.sendMessage(Component.text(" §7- §e" + t.getName() + " §7[" + t.getTag() + "] §8мир: " + t.getWorld() + " чанки: " + t.getMinChunkX() + "," + t.getMinChunkZ() + " -> " + t.getMaxChunkX() + "," + t.getMaxChunkZ()));
        }
        return true;
    }

    private boolean removeTerritory(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /imp remove <name>", NamedTextColor.RED));
            return true;
        }
        String name = args[1];
        if (!territoryManager.exists(name)) {
            player.sendMessage(Component.text("Территория не найдена", NamedTextColor.RED));
            return true;
        }
        territoryManager.remove(name);
        player.sendMessage(Component.text("Территория '" + name + "' удалена", NamedTextColor.GREEN));
        return true;
    }
}
