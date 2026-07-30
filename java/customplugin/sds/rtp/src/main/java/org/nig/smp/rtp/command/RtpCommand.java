package org.nig.smp.rtp.command;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.nig.smp.rtp.config.ConfigManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class RtpCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final Random random = new Random();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public RtpCommand(JavaPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }

        if (!player.hasPermission("rtp.use")) {
            player.sendMessage(config.getMessageNoPermission());
            return true;
        }

        World world = player.getWorld();
        var enabledWorlds = config.getEnabledWorlds();
        if (!enabledWorlds.isEmpty() && !enabledWorlds.contains(world.getName())) {
            player.sendMessage(config.getMessageWorldDisabled());
            return true;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(uuid)) {
            long remaining = (cooldowns.get(uuid) - now) / 1000;
            if (remaining > 0) {
                player.sendMessage(config.getMessageCooldown().replace("%time%", String.valueOf(remaining)));
                return true;
            }
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            player.sendMessage(config.getMessageTeleporting());

            Location target = findSafeLocation(world);

            if (target == null) {
                player.sendMessage(config.getMessageSafeLocationNotFound());
                return;
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.teleportAsync(target);
                cooldowns.put(uuid, now + config.getCooldownSeconds() * 1000L);
                player.sendMessage(config.getMessageSuccess()
                        .replace("%x%", String.valueOf(target.getBlockX()))
                        .replace("%z%", String.valueOf(target.getBlockZ())));
            });
        });

        return true;
    }

    private Location findSafeLocation(World world) {
        int attempts = 20;
        for (int i = 0; i < attempts; i++) {
            int x = randomInRange(config.getMinX(), config.getMaxX());
            int z = randomInRange(config.getMinZ(), config.getMaxZ());

            if (config.isUseWorldBorder()) {
                var border = world.getWorldBorder();
                x = clampToBorder(x, border);
                z = clampToBorder(z, border);
            }

            int y = getHighestSafeY(world, x, z);
            if (y != -1) {
                return new Location(world, x + 0.5, y + 1, z + 0.5);
            }
        }
        return null;
    }

    private static final java.util.Set<org.bukkit.Material> SAFE_BLOCKS = java.util.Set.of(
            org.bukkit.Material.GRASS_BLOCK,
            org.bukkit.Material.SNOW_BLOCK
    );

    private int getHighestSafeY(World world, int x, int z) {
        int maxY = Math.min(config.getMaxY(), world.getMaxHeight() - 1);
        int minY = Math.max(config.getMinY(), world.getMinHeight());

        for (int y = maxY; y >= minY; y--) {
            Block block = world.getBlockAt(x, y, z);
            Block above = world.getBlockAt(x, y + 1, z);
            Block head = world.getBlockAt(x, y + 2, z);

            if (SAFE_BLOCKS.contains(block.getType())
                    && above.getType().isAir()
                    && head.getType().isAir()) {
                return y;
            }
        }
        return -1;
    }

    private int randomInRange(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    private int clampToBorder(int value, org.bukkit.WorldBorder border) {
        int centerX = (int) border.getCenter().getBlockX();
        int centerZ = (int) border.getCenter().getBlockZ();
        int radius = (int) border.getSize() / 2;
        return Math.max(centerX - radius, Math.min(centerX + radius, value));
    }
}
