package org.nig.smp.connectapi.core;

import org.nig.smp.connectapi.ConnectApiPlugin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AccountManager {

    private final ConnectApiPlugin plugin;
    private final Map<String, String> accounts = new LinkedHashMap<>();

    public AccountManager(ConnectApiPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        accounts.clear();
        if (plugin.getConfig().getConfigurationSection("accounts") != null) {
            for (String nick : plugin.getConfig().getConfigurationSection("accounts").getKeys(false)) {
                accounts.put(nick, plugin.getConfig().getString("accounts." + nick, ""));
            }
        }
    }

    public boolean hasAccount(String nick) {
        return nick != null && accounts.containsKey(nick);
    }

    public boolean authorize(String nick, String password) {
        if (nick == null || !accounts.containsKey(nick)) return false;
        String stored = accounts.get(nick);
        // пустой пароль в конфиге = вход без пароля
        return stored == null || stored.isEmpty() || stored.equals(password);
    }

    public boolean addUser(String nick, String password) {
        if (nick == null || nick.isEmpty()) return false;
        accounts.put(nick, password == null ? "" : password);
        plugin.getConfig().set("accounts." + nick, password == null ? "" : password);
        plugin.saveConfig();
        return true;
    }

    public boolean removeUser(String nick) {
        if (nick == null || !accounts.containsKey(nick)) return false;
        accounts.remove(nick);
        plugin.getConfig().set("accounts." + nick, null);
        plugin.saveConfig();
        return true;
    }

    public boolean setPassword(String nick, String password) {
        if (nick == null || !accounts.containsKey(nick)) return false;
        accounts.put(nick, password == null ? "" : password);
        plugin.getConfig().set("accounts." + nick, password == null ? "" : password);
        plugin.saveConfig();
        return true;
    }

    public Map<String, String> getAccounts() {
        return Collections.unmodifiableMap(accounts);
    }
}
