package org.nig.smp.impers.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.nig.smp.impers.ImpersPlugin;
import org.nig.smp.impers.manager.TerritoryManager;
import org.nig.smp.impers.model.ChunkSelection;
import org.nig.smp.impers.model.Territory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    private static final int GRID_RADIUS = 3;

    private final ImpersPlugin plugin;
    private final TerritoryManager territoryManager;
    private final Map<UUID, ChunkSelection> selections;
    private final Map<UUID, String> lastTerritoryMessage;

    public PlayerListener(ImpersPlugin plugin, TerritoryManager territoryManager, Map<UUID, ChunkSelection> selections) {
        this.plugin = plugin;
        this.territoryManager = territoryManager;
        this.selections = selections;
        this.lastTerritoryMessage = new HashMap<>();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!isSelectionStick(item)) return;

        event.setCancelled(true);

        if (event.getClickedBlock() == null) return;

        int chunkX = event.getClickedBlock().getChunk().getX();
        int chunkZ = event.getClickedBlock().getChunk().getZ();

        ChunkSelection sel = selections.computeIfAbsent(player.getUniqueId(), k -> new ChunkSelection());

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            sel.setPos1(chunkX, chunkZ);
            player.sendMessage(Component.text(plugin.msg("selection-pos1", "x", chunkX, "z", chunkZ)));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            int max = plugin.getMaxSelectionSize();
            boolean capped = sel.setPos2(chunkX, chunkZ, max);
            player.sendMessage(Component.text(plugin.msg("selection-pos2", "x", sel.getPos2X(), "z", sel.getPos2Z())));
            if (capped) {
                player.sendMessage(Component.text(plugin.msg("selection-limit", "max", max)));
            }
            if (sel.isComplete()) {
                player.sendMessage(Component.text(plugin.msg("selection-size", "x", sel.getSizeX(), "z", sel.getSizeZ())));
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        int fromX = from.getChunk().getX();
        int fromZ = from.getChunk().getZ();
        int toX = to.getChunk().getX();
        int toZ = to.getChunk().getZ();

        if (fromX == toX && fromZ == toZ) return;

        Territory territory = territoryManager.getTerritoryAt(to.getWorld().getName(), toX, toZ);
        String last = lastTerritoryMessage.get(player.getUniqueId());
        if (territory != null) {
            if (!territory.getName().equals(last)) {
                player.sendMessage(Component.text(plugin.msg("enter-territory", "name", territory.getName(), "tag", territory.getTag())));
                lastTerritoryMessage.put(player.getUniqueId(), territory.getName());
            }
        } else if (last != null) {
            player.sendMessage(Component.text(plugin.msg("leave-territory", "name", last)));
            lastTerritoryMessage.remove(player.getUniqueId());
        }
    }

    public void showSelectionParticles() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (!isSelectionStick(item)) {
                item = player.getInventory().getItemInOffHand();
                if (!isSelectionStick(item)) continue;
            }

            ChunkSelection sel = selections.get(player.getUniqueId());
            World world = player.getWorld();
            double baseY = player.getLocation().getY() + 0.5;

            drawChunkGrid(world, player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ(), baseY);

            if (sel != null && sel.hasPos1()) {
                drawSelection(world, player, sel, baseY);
            }
        }
    }

    private void drawChunkGrid(World world, int centerChunkX, int centerChunkZ, double y) {
        Particle.DustOptions white = new Particle.DustOptions(Color.WHITE, 0.8f);
        int minX = (centerChunkX - GRID_RADIUS) * 16;
        int maxX = (centerChunkX + GRID_RADIUS + 1) * 16;
        int minZ = (centerChunkZ - GRID_RADIUS) * 16;
        int maxZ = (centerChunkZ + GRID_RADIUS + 1) * 16;

        for (int x = minX; x <= maxX; x += 16) {
            for (int z = minZ; z <= maxZ; z += 4) {
                spawnParticle(world, x + 0.5, y, z + 0.5, white);
            }
        }
        for (int z = minZ; z <= maxZ; z += 16) {
            for (int x = minX; x <= maxX; x += 4) {
                spawnParticle(world, x + 0.5, y, z + 0.5, white);
            }
        }
    }

    private void drawSelection(World world, Player player, ChunkSelection sel, double y) {
        Particle.DustOptions color = getSelectionColor(player, sel);
        int minX = sel.getMinChunkX() * 16;
        int maxX = (sel.getMaxChunkX() + 1) * 16;
        int minZ = sel.getMinChunkZ() * 16;
        int maxZ = (sel.getMaxChunkZ() + 1) * 16;

        for (int x = minX; x <= maxX; x += 2) {
            spawnParticle(world, x + 0.5, y, minZ + 0.5, color);
            spawnParticle(world, x + 0.5, y, maxZ + 0.5, color);
        }
        for (int z = minZ; z <= maxZ; z += 2) {
            spawnParticle(world, minX + 0.5, y, z + 0.5, color);
            spawnParticle(world, maxX + 0.5, y, z + 0.5, color);
        }
    }

    private Particle.DustOptions getSelectionColor(Player player, ChunkSelection sel) {
        String worldName = player.getWorld().getName();
        boolean enemy = false;
        boolean friendly = false;
        for (int cx = sel.getMinChunkX(); cx <= sel.getMaxChunkX(); cx++) {
            for (int cz = sel.getMinChunkZ(); cz <= sel.getMaxChunkZ(); cz++) {
                Territory t = territoryManager.getTerritoryAt(worldName, cx, cz);
                if (t != null) {
                    if (t.isMember(player.getUniqueId())) {
                        friendly = true;
                    } else {
                        enemy = true;
                    }
                }
            }
        }
        if (enemy) {
            return new Particle.DustOptions(Color.RED, 1.5f);
        }
        if (friendly) {
            return new Particle.DustOptions(Color.GREEN, 1.5f);
        }
        return new Particle.DustOptions(Color.ORANGE, 1.5f);
    }

    private void spawnParticle(World world, double x, double y, double z, Particle.DustOptions options) {
        world.spawnParticle(Particle.DUST, x, y, z, 1, options);
    }

    private boolean isSelectionStick(ItemStack item) {
        if (item == null || item.getType() != Material.STICK) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(
            new NamespacedKey(plugin, "impers_stick"),
            PersistentDataType.BOOLEAN
        );
    }
}
