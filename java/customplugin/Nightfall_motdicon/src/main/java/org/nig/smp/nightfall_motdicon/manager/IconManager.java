package org.nig.smp.nightfall_motdicon.manager;

import org.bukkit.Bukkit;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;
import org.nig.smp.nightfall_motdicon.Nightfall_motdicon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;

public class IconManager {

    private final Nightfall_motdicon plugin;
    private CachedServerIcon cachedIcon;

    public IconManager(Nightfall_motdicon plugin) {
        this.plugin = plugin;
    }

    public void loadIcon() {
        File iconFile = new File(plugin.getDataFolder(), plugin.getConfig().getString("icon.file", "server-icon.png"));

        if (!iconFile.exists()) {
            loadFallback(iconFile);
        }

        if (!iconFile.exists()) {
            plugin.getLogger().warning("Server icon file not found: " + iconFile.getAbsolutePath());
            cachedIcon = null;
            return;
        }

        try {
            BufferedImage image = ImageIO.read(iconFile);
            if (image == null) {
                plugin.getLogger().warning("Failed to read icon file (unsupported format): " + iconFile.getName());
                cachedIcon = null;
                return;
            }
            cachedIcon = Bukkit.loadServerIcon(iconFile);
            plugin.getLogger().info("Server icon loaded: " + iconFile.getName());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load server icon", e);
            cachedIcon = null;
        }
    }

    private void loadFallback(File iconFile) {
        String fallbackUrl = plugin.getConfig().getString("icon.fallback-url", "");
        if (fallbackUrl == null || fallbackUrl.isBlank()) return;

        plugin.getLogger().info("Attempting to download fallback icon from: " + fallbackUrl);
        try {
            BufferedImage image = ImageIO.read(new URL(fallbackUrl));
            if (image == null) {
                plugin.getLogger().warning("Fallback URL returned no image data");
                return;
            }
            BufferedImage resized = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = resized.createGraphics();
            g.drawImage(image, 0, 0, 64, 64, null);
            g.dispose();
            ImageIO.write(resized, "png", iconFile);
            plugin.getLogger().info("Fallback icon saved to: " + iconFile.getName());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to download fallback icon", e);
        }
    }

    public void applyIcon(ServerListPingEvent event) {
        if (cachedIcon != null) {
            event.setServerIcon(cachedIcon);
        }
    }
}
