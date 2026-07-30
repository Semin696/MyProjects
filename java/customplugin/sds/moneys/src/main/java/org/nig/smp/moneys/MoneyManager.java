package org.nig.smp.moneys;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MoneyManager {

    private final Moneys plugin;
    private final File dataFile;
    private FileConfiguration data;
    private final Map<UUID, Double> balances = new HashMap<>();

    private double defaultBalance;

    public MoneyManager(Moneys plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        loadConfig();
        loadData();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        defaultBalance = config.getDouble("default-balance", 100.0);
    }

    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create data.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : data.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                balances.put(uuid, data.getDouble(key));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void saveData() {
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            data.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save data.yml: " + e.getMessage());
        }
    }

    public double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, defaultBalance);
    }

    public void setBalance(UUID uuid, double amount) {
        balances.put(uuid, round(amount));
    }

    public boolean hasEnough(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }

    public void deposit(UUID uuid, double amount) {
        setBalance(uuid, getBalance(uuid) + amount);
    }

    public boolean withdraw(UUID uuid, double amount) {
        if (!hasEnough(uuid, amount)) return false;
        setBalance(uuid, getBalance(uuid) - amount);
        return true;
    }

    public String format(double amount) {
        return String.format("%.2f", amount);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
