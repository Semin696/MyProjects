package org.nig.smp.connectapi.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.nig.smp.connectapi.core.AccountManager;

import java.util.Map;

public class ConnectApiCommand implements CommandExecutor {

    private final AccountManager accounts;

    public ConnectApiCommand(AccountManager accounts) {
        this.accounts = accounts;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            send(sender, "&7Использование: &f/" + label + " &aadduser&f <ник> <пароль> &7| &apasswd&f <ник> <пароль> &7| &aremoveuser&f <ник> &7| &alist");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "adduser": {
                if (args.length < 3) {
                    send(sender, "&cИспользование: /" + label + " adduser <ник> <пароль>");
                    return true;
                }
                if (accounts.addUser(args[1], args[2])) {
                    send(sender, "&aАккаунт добавлен: &f" + args[1] + (args[2].isEmpty() ? " &7(без пароля)" : ""));
                } else {
                    send(sender, "&cНе удалось добавить аккаунт");
                }
                return true;
            }
            case "passwd": {
                if (args.length < 3) {
                    send(sender, "&cИспользование: /" + label + " passwd <ник> <пароль>");
                    return true;
                }
                if (accounts.setPassword(args[1], args[2])) {
                    send(sender, "&aПароль обновлён для &f" + args[1] + (args[2].isEmpty() ? " &7(теперь без пароля)" : ""));
                } else {
                    send(sender, "&cАккаунт не найден: " + args[1]);
                }
                return true;
            }
            case "removeuser": {
                if (args.length < 2) {
                    send(sender, "&cИспользование: /" + label + " removeuser <ник>");
                    return true;
                }
                if (accounts.removeUser(args[1])) {
                    send(sender, "&aАккаунт удалён: &f" + args[1]);
                } else {
                    send(sender, "&cАккаунт не найден: " + args[1]);
                }
                return true;
            }
            case "list": {
                send(sender, "&7Аккаунты ConnectApi (&f" + accounts.getAccounts().size() + "&7):");
                for (Map.Entry<String, String> e : accounts.getAccounts().entrySet()) {
                    String pass = (e.getValue() == null || e.getValue().isEmpty()) ? "&7(без пароля)" : "&a(пароль задан)";
                    send(sender, "  &f" + e.getKey() + " &r" + pass);
                }
                return true;
            }
            default:
                send(sender, "&cНеизвестная подкоманда: " + args[0]);
                return true;
        }
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&bConnectApi&8] " + message));
    }
}
