package org.nig.smp.impers.command;

import net.kyori.adventure.text.Component;
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
            case "invite" -> {
                return invite(player, args);
            }
            case "kick" -> {
                return kick(player, args);
            }
            case "list" -> {
                return listTerritories(player);
            }
            case "remove" -> {
                return removeTerritory(player, args);
            }
            default -> {
                player.sendMessage(Component.text(plugin.msg("usage")));
                return true;
            }
        }
    }

    private boolean giveStick(Player player) {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        meta.displayName(Component.text("Выделение империи", net.kyori.adventure.text.format.NamedTextColor.GOLD));
        meta.lore(java.util.List.of(
            Component.text("ЛКМ - первая точка", net.kyori.adventure.text.format.NamedTextColor.GRAY),
            Component.text("ПКМ - вторая точка", net.kyori.adventure.text.format.NamedTextColor.GRAY)
        ));
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "impers_stick"),
            PersistentDataType.BOOLEAN,
            true
        );
        stick.setItemMeta(meta);
        player.getInventory().addItem(stick);
        player.sendMessage(Component.text(plugin.msg("stick-received")));
        return true;
    }

    private boolean createTerritory(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text(plugin.msg("usage-create")));
            return true;
        }

        String name = args[1];
        String tag = args[2];

        if (territoryManager.exists(name)) {
            player.sendMessage(Component.text(plugin.msg("territory-exists")));
            return true;
        }

        ChunkSelection sel = selections.get(player.getUniqueId());
        if (sel == null || !sel.isComplete()) {
            player.sendMessage(Component.text(plugin.msg("no-selection")));
            return true;
        }

        Territory territory = new Territory(
            name, tag, player.getWorld().getName(),
            sel.getMinChunkX(), sel.getMinChunkZ(),
            sel.getMaxChunkX(), sel.getMaxChunkZ(),
            player.getUniqueId()
        );

        territoryManager.add(territory);
        selections.remove(player.getUniqueId());

        player.sendMessage(Component.text(plugin.msg("territory-created", "name", name, "x", sel.getSizeX(), "z", sel.getSizeZ())));
        return true;
    }

    private boolean invite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text(plugin.msg("usage-invite")));
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(Component.text(plugin.msg("player-not-found")));
            return true;
        }

        Territory territory = getTerritoryAtPlayer(player);
        if (territory == null) {
            player.sendMessage(Component.text(plugin.msg("not-in-territory")));
            return true;
        }

        if (!territory.isOwner(player.getUniqueId())) {
            player.sendMessage(Component.text(plugin.msg("not-owner")));
            return true;
        }

        territory.addMember(target.getUniqueId());
        territoryManager.save();

        player.sendMessage(Component.text(plugin.msg("invite-success", "player", target.getName(), "name", territory.getName())));
        target.sendMessage(Component.text(plugin.msg("invite-target", "tag", territory.getTag(), "name", territory.getName())));
        return true;
    }

    private boolean kick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text(plugin.msg("usage-kick")));
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(Component.text(plugin.msg("player-not-found")));
            return true;
        }

        Territory territory = getTerritoryAtPlayer(player);
        if (territory == null) {
            player.sendMessage(Component.text(plugin.msg("not-in-territory")));
            return true;
        }

        if (!territory.isOwner(player.getUniqueId())) {
            player.sendMessage(Component.text(plugin.msg("not-owner")));
            return true;
        }

        if (territory.isOwner(target.getUniqueId())) {
            player.sendMessage(Component.text(plugin.msg("cannot-kick-owner")));
            return true;
        }

        if (!territory.isMember(target.getUniqueId())) {
            player.sendMessage(Component.text(plugin.msg("player-not-member")));
            return true;
        }

        territory.removeMember(target.getUniqueId());
        territoryManager.save();

        player.sendMessage(Component.text(plugin.msg("kick-success", "player", target.getName(), "name", territory.getName())));
        target.sendMessage(Component.text(plugin.msg("kick-target", "name", territory.getName())));
        return true;
    }

    private Territory getTerritoryAtPlayer(Player player) {
        return territoryManager.getTerritoryAt(
            player.getWorld().getName(),
            player.getLocation().getChunk().getX(),
            player.getLocation().getChunk().getZ()
        );
    }

    private boolean listTerritories(Player player) {
        java.util.Collection<Territory> all = territoryManager.getAll();
        if (all.isEmpty()) {
            player.sendMessage(Component.text(plugin.msg("list-empty")));
            return true;
        }
        player.sendMessage(Component.text(plugin.msg("list-header")));
        for (Territory t : all) {
            player.sendMessage(Component.text(plugin.msg("list-format",
                "name", t.getName(),
                "tag", t.getTag(),
                "world", t.getWorld(),
                "minX", t.getMinChunkX(),
                "minZ", t.getMinChunkZ(),
                "maxX", t.getMaxChunkX(),
                "maxZ", t.getMaxChunkZ()
            )));
        }
        return true;
    }

    private boolean removeTerritory(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text(plugin.msg("usage-remove")));
            return true;
        }
        String name = args[1];
        if (!territoryManager.exists(name)) {
            player.sendMessage(Component.text(plugin.msg("no-territory")));
            return true;
        }
        territoryManager.remove(name);
        player.sendMessage(Component.text(plugin.msg("territory-removed", "name", name)));
        return true;
    }
}
