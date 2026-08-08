package org.nig.smp.duels.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.nig.smp.duels.DuelsPlugin;
import org.nig.smp.duels.model.Arena;
import org.nig.smp.duels.model.DuelMatch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WorldProtectionListener implements Listener {

    private final DuelsPlugin plugin;
    private final Map<DuelMatch, Map<Location, BlockData>> broken = new HashMap<>();
    private final Map<DuelMatch, Set<Location>> placed = new HashMap<>();

    public WorldProtectionListener(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.isDuelsWorld(event.getBlock().getWorld())) {
            return;
        }
        DuelMatch match = activeMatchFor(event.getPlayer().getUniqueId());
        if (match == null || !canBreak(match)) {
            event.setCancelled(true);
            return;
        }
        broken.computeIfAbsent(match, k -> new HashMap<>())
            .putIfAbsent(event.getBlock().getLocation(), event.getBlock().getBlockData());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.isDuelsWorld(event.getBlock().getWorld())) {
            return;
        }
        DuelMatch match = activeMatchFor(event.getPlayer().getUniqueId());
        if (match == null || !canBreak(match)) {
            event.setCancelled(true);
            return;
        }
        placed.computeIfAbsent(match, k -> new HashSet<>()).add(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.getEntity().getWorld(), event.blockList());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.getBlock().getWorld(), event.blockList());
    }

    private void handleExplosion(World world, List<Block> blocks) {
        if (!plugin.isDuelsWorld(world) || blocks.isEmpty()) {
            return;
        }
        DuelMatch match = breakableMatchNear(world, blocks);
        if (match == null) {
            blocks.clear();
            return;
        }
        Map<Location, BlockData> map = broken.computeIfAbsent(match, k -> new HashMap<>());
        for (Block block : blocks) {
            map.putIfAbsent(block.getLocation(), block.getBlockData());
        }
    }

    private DuelMatch breakableMatchNear(World world, List<Block> blocks) {
        Location origin = blocks.get(0).getLocation();
        for (DuelMatch match : plugin.getDuelManager().getActiveMatches()) {
            if (!canBreak(match)) {
                continue;
            }
            Arena arena = plugin.getArenaManager().getArena(match.getArena());
            if (arena == null) {
                continue;
            }
            if (!arena.getSpawn1().getWorld().getName().equals(world.getName())) {
                continue;
            }
            if (arena.getSpawn1().distance(origin) <= 64 || arena.getSpawn2().distance(origin) <= 64) {
                return match;
            }
        }
        return null;
    }

    private DuelMatch activeMatchFor(UUID playerId) {
        DuelMatch match = plugin.getDuelManager().getActiveMatch(playerId);
        return match != null && match.getPhase() == DuelMatch.Phase.ACTIVE ? match : null;
    }

    private boolean canBreak(DuelMatch match) {
        if (match.getPhase() != DuelMatch.Phase.ACTIVE) {
            return false;
        }
        Arena arena = plugin.getArenaManager().getArena(match.getArena());
        if (arena == null || !arena.isBlockBreakAllowed()) {
            return false;
        }
        for (UUID id : match.players()) {
            String kit = match.getKit(id);
            if (kit != null && plugin.getKitManager().isDestructive(kit)) {
                return true;
            }
        }
        return false;
    }

    public void restore(DuelMatch match) {
        Map<Location, BlockData> brokenMap = broken.remove(match);
        if (brokenMap != null) {
            for (Map.Entry<Location, BlockData> entry : brokenMap.entrySet()) {
                Block block = entry.getKey().getWorld().getBlockAt(entry.getKey());
                block.setBlockData(entry.getValue(), false);
            }
        }
        Set<Location> placedSet = placed.remove(match);
        if (placedSet != null) {
            for (Location loc : placedSet) {
                Block block = loc.getWorld().getBlockAt(loc);
                if (block.getType() != Material.AIR) {
                    block.setType(Material.AIR, false);
                }
            }
        }
    }
}