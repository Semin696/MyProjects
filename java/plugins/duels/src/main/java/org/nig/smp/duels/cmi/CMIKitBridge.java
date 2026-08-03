package org.nig.smp.duels.cmi;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
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
        Object kitManager = getKitManager();
        if (kitManager == null) {
            return names;
        }
        try {
            Map<?, ?> kits = (Map<?, ?>) method(kitManager, "getKits").invoke(kitManager);
            if (kits != null) {
                for (Object key : kits.keySet()) {
                    names.add(String.valueOf(key));
                }
            }
        } catch (Exception ignored) {
        }
        return names;
    }

    public static List<ItemStack> getKitItems(String name) {
        Object kit = getKit(name);
        if (kit == null) {
            return new ArrayList<>();
        }
        try {
            List<ItemStack> items = (List<ItemStack>) method(kit, "getItems").invoke(kit);
            return items != null ? items : new ArrayList<>();
        } catch (Exception ignored) {
        }
        return new ArrayList<>();
    }

    public static boolean applyKit(Player player, String name) {
        Object kit = getKit(name);
        if (kit == null) {
            return false;
        }
        try {
            Method method = method(kit, "loadkit");
            if (method.getParameterCount() == 1) {
                method.invoke(kit, player);
                return true;
            }
        } catch (Exception ignored) {
        }
        try {
            Method method = method(kit, "loadKit");
            if (method.getParameterCount() == 1) {
                method.invoke(kit, player);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static Object getKit(String name) {
        Object kitManager = getKitManager();
        if (kitManager == null) {
            return null;
        }
        try {
            return method(kitManager, "getKit").invoke(kitManager, name);
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Object getKitManager() {
        try {
            Class<?> cmiClass = Class.forName("com.Zrips.CMI.CMI");
            Object instance = cmiClass.getMethod("getInstance").invoke(null);
            return cmiClass.getMethod("getKitManager").invoke(instance);
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Method method(Object target, String name) throws NoSuchMethodException {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals(name)) {
                    m.setAccessible(true);
                    return m;
                }
            }
            clazz = clazz.getSuperclass();
        }
        throw new NoSuchMethodException(name);
    }
}
