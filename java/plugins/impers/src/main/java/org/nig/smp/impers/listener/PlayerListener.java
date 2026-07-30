package org.nig.smp.impers.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            player.sendMessage(Component.text("Первая точка: чанк [" + chunkX + ", " + chunkZ + "]", NamedTextColor.GREEN));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            sel.setPos2(chunkX, chunkZ);
            player.sendMessage(Component.text("Вторая точка: чанк [" + chunkX + ", " + chunkZ + "]", NamedTextColor.AQUA));
            if (sel.isComplete()) {
                player.sendMessage(Component.text("Выделено чаunkов: " + sel.getSizeX() + "x" + sel.getSizeZ(), NamedTextColor.YELLOW));
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
        if (territory != null) {
            String msgKey = territory.getName() + "|" + player.getUniqueId();
            String lastMsg = lastTerritoryMessage.get(player.getUniqueId());
            if (!territory.getName().equals(lastMsg)) {
                player.sendMessage(Component.text("Территория " + territory.getName() + " империи " + territory.getTag(), NamedTextColor.GOLD));
                lastTerritoryMessage.put(player.getUniqueId(), territory.getName());
            }
        } else {
            lastTerritoryMessage.remove(player.getUniqueId());
        }
    }

    public void showSelectionParticles() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (!isSelectionStick(item)) continue;

            ChunkSelection sel = selections.get(player.getUniqueId());
            if (sel == null || !sel.hasPos1()) continue;

            World world = player.getWorld();
            int minX = sel.getMinChunkX() * 16;
            int maxX = (sel.getMaxChunkX() + 1) * 16;
            int minZ = sel.getMinChunkZ() * 16;
            int maxZ = (sel.getMaxChunkZ() + 1) * 16;

            double y = player.getLocation().getY() + 0.5;

            Particle.DustOptions options;
            if (sel.isComplete()) {
                options = new Particle.DustOptions(Color.LIME, 1.5f);
            } else {
                options = new Particle.DustOptions(Color.YELLOW, 1.5f);
            }

            for (int x = minX; x <= maxX; x += 2) {
                spawnParticle(world, x + 0.5, y, minZ + 0.5, options);
                spawnParticle(world, x + 0.5, y, maxZ + 0.5, options);
            }
            for (int z = minZ; z <= maxZ; z += 2) {
                spawnParticle(world, minX + 0.5, y, z + 0.5, options);
                spawnParticle(world, maxX + 0.5, y, z + 0.5, options);
            }
        }
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
