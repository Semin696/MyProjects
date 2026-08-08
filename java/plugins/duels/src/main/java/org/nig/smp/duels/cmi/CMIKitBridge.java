package org.nig.smp.duels.cmi;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Kit;
import com.Zrips.CMI.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CMIKitBridge {

    private CMIKitBridge() {
    }

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("CMI") != null;
    }

    public static Set<String> getKitNames() {
        Set<String> names = new LinkedHashSet<>();
        if (!isAvailable()) {
            return names;
        }
        try {
            Map<String, Kit> kits = manager().getKits();
            if (kits != null) {
                names.addAll(kits.keySet());
            }
        } catch (Exception | LinkageError ignored) {
        }
        return names;
    }

    public static List<ItemStack> getKitItems(String name) {
        List<ItemStack> items = new ArrayList<>();
        if (!isAvailable()) {
            return items;
        }
        try {
            Kit kit = kit(name);
            if (kit != null && kit.getItems() != null) {
                items.addAll(kit.getItems());
            }
        } catch (Exception | LinkageError ignored) {
        }
        return items;
    }

    public static boolean applyKit(Player player, String name) {
        if (!isAvailable()) {
            return false;
        }
        try {
            Kit kit = kit(name);
            if (kit == null) {
                return false;
            }
            kit.loadkit(player);
            return true;
        } catch (Exception | LinkageError ignored) {
        }
        return false;
    }

    private static KitManager manager() {
        CMI cmi = CMI.getInstance();
        return cmi == null ? null : cmi.getKitManager();
    }

    private static Kit kit(String name) {
        KitManager manager = manager();
        return manager == null ? null : manager.getKit(name);
    }
}
