package org.nig.smp.team;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class TeamManager {
    private final Team plugin;
    private final Map<String, TeamData> teamsByName;
    private final Map<UUID, String> playerTeamMap;
    private final Map<UUID, String> pendingInvites;
    private final Gson gson;
    private final File dataFile;

    public TeamManager(Team plugin) {
        this.plugin = plugin;
        this.teamsByName = new HashMap<>();
        this.playerTeamMap = new HashMap<>();
        this.pendingInvites = new HashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataFile = new File(plugin.getDataFolder(), "teams.json");
        load();
    }

    public String createTeam(Player leader, String name) {
        if (playerTeamMap.containsKey(leader.getUniqueId()))
            return msg("already-in-team");
        if (teamsByName.containsKey(name.toLowerCase()))
            return msg("team-exists");

        TeamData team = new TeamData(name, leader.getUniqueId());
        teamsByName.put(name.toLowerCase(), team);
        playerTeamMap.put(leader.getUniqueId(), name.toLowerCase());
        save();
        return msg("team-created", "name", name);
    }

    public String disbandTeam(Player player) {
        String key = playerTeamMap.get(player.getUniqueId());
        if (key == null) return msg("not-in-team");
        TeamData team = teamsByName.get(key);
        if (team == null) return msg("team-not-found");
        if (!team.getLeader().equals(player.getUniqueId()))
            return msg("only-leader");

        for (UUID uid : team.getAllPlayers()) {
            playerTeamMap.remove(uid);
        }
        teamsByName.remove(key);
        save();
        return msg("team-disbanded", "name", team.getName());
    }

    public String invitePlayer(Player inviter, String targetName) {
        String key = playerTeamMap.get(inviter.getUniqueId());
        if (key == null) return msg("not-in-team");
        TeamData team = teamsByName.get(key);
        if (team == null) return msg("team-not-found");
        if (!team.getLeader().equals(inviter.getUniqueId()))
            return msg("only-leader");

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) return msg("player-not-found");
        if (playerTeamMap.containsKey(target.getUniqueId()))
            return msg("player-already-in-team");

        pendingInvites.put(target.getUniqueId(), key);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingInvites.containsKey(target.getUniqueId()) &&
                pendingInvites.get(target.getUniqueId()).equals(key)) {
                pendingInvites.remove(target.getUniqueId());
                target.sendMessage(msg("invite-expired"));
            }
        }, 1200L);

        target.sendMessage(pf() + "§a" + inviter.getName() + " приглашает тебя в команду \"" + team.getName() + "\".");
        target.sendMessage("§aИспользуй §f/team join " + team.getName() + " §aчтобы присоединиться.");
        return msg("invite-sent", "player", target.getName());
    }

    public String joinTeam(Player player, String teamName) {
        String key = teamName.toLowerCase();
        if (playerTeamMap.containsKey(player.getUniqueId()))
            return msg("already-in-team");
        if (!pendingInvites.containsKey(player.getUniqueId()) ||
            !pendingInvites.get(player.getUniqueId()).equals(key))
            return msg("no-invite");

        TeamData team = teamsByName.get(key);
        if (team == null) return msg("team-not-found");

        team.addMember(player.getUniqueId());
        playerTeamMap.put(player.getUniqueId(), key);
        pendingInvites.remove(player.getUniqueId());
        save();

        for (UUID uid : team.getAllPlayers()) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && !p.equals(player)) {
                p.sendMessage(pf() + msg("player-joined", "player", player.getName()));
            }
        }
        return msg("joined-team", "team", team.getName());
    }

    public String leaveTeam(Player player) {
        String key = playerTeamMap.get(player.getUniqueId());
        if (key == null) return msg("not-in-team");
        TeamData team = teamsByName.get(key);
        if (team == null) return msg("team-not-found");
        if (team.getLeader().equals(player.getUniqueId()))
            return msg("leader-cant-leave");

        team.removeMember(player.getUniqueId());
        playerTeamMap.remove(player.getUniqueId());
        save();

        for (UUID uid : team.getAllPlayers()) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) {
                p.sendMessage(pf() + msg("player-left", "player", player.getName()));
            }
        }
        return msg("left-team", "team", team.getName());
    }

    public String kickPlayer(Player leader, String targetName) {
        String key = playerTeamMap.get(leader.getUniqueId());
        if (key == null) return msg("not-in-team");
        TeamData team = teamsByName.get(key);
        if (team == null) return msg("team-not-found");
        if (!team.getLeader().equals(leader.getUniqueId()))
            return msg("only-leader");

        Player target = Bukkit.getPlayerExact(targetName);
        UUID targetId = target != null ? target.getUniqueId() : null;

        if (targetId == null) {
            for (Map.Entry<UUID, String> entry : playerTeamMap.entrySet()) {
                if (entry.getValue().equals(key)) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p != null && p.getName().equalsIgnoreCase(targetName)) {
                        targetId = p.getUniqueId();
                        target = p;
                        break;
                    }
                }
            }
            if (targetId == null)
                return msg("player-not-in-team");
        }

        if (team.getLeader().equals(targetId))
            return msg("cant-kick-leader");
        if (!team.isMember(targetId))
            return msg("player-not-in-team");

        team.removeMember(targetId);
        playerTeamMap.remove(targetId);
        save();

        if (target != null)
            target.sendMessage(pf() + msg("you-were-kicked", "team", team.getName()));
        return msg("player-kicked", "player", target.getName());
    }

    public String setHome(Player player) {
        String key = playerTeamMap.get(player.getUniqueId());
        if (key == null) return msg("not-in-team");
        TeamData team = teamsByName.get(key);
        if (team == null) return msg("team-not-found");
        if (!team.getLeader().equals(player.getUniqueId()))
            return msg("only-leader");

        team.setHome(player.getLocation());
        save();
        return msg("home-set");
    }

    public String home(Player player) {
        String key = playerTeamMap.get(player.getUniqueId());
        if (key == null) return msg("not-in-team");
        TeamData team = teamsByName.get(key);
        if (team == null) return msg("team-not-found");
        if (!team.hasHome()) return msg("home-not-set");

        Location loc = team.getHome();
        if (loc.getWorld() == null) return msg("world-unavailable");

        player.teleport(loc);
        return msg("home-teleport");
    }

    public String info(Player player, String teamName) {
        String key;
        if (teamName != null) {
            key = teamName.toLowerCase();
        } else {
            key = playerTeamMap.get(player.getUniqueId());
            if (key == null) return msg("specify-team");
        }

        TeamData team = teamsByName.get(key);
        if (team == null) return msg("team-not-found");

        String homeStatus = team.hasHome() ? "§aустановлен" : "§cне установлен";

        StringBuilder sb = new StringBuilder();
        sb.append("§6=== ").append(team.getName()).append(" ===\n");
        sb.append("§eЛидер: §f").append(Bukkit.getOfflinePlayer(team.getLeader()).getName()).append("\n");
        sb.append("§eУчастники: §f").append(team.getMembers().size()).append("\n");
        sb.append("§eДом: ").append(homeStatus);

        if (player.hasPermission("team.info.members")) {
            sb.append("\n§eСостав:\n");
            for (UUID uid : team.getAllPlayers()) {
                String name = Bukkit.getOfflinePlayer(uid).getName();
                String prefix = team.getLeader().equals(uid) ? "§6[L] " : "§7- ";
                sb.append(prefix).append(name != null ? name : uid).append("\n");
            }
        }
        return sb.toString();
    }

    public String list() {
        if (teamsByName.isEmpty()) return msg("no-teams");
        StringBuilder sb = new StringBuilder("§6=== Команды (§f" + teamsByName.size() + "§6) ===\n");
        int i = 1;
        for (TeamData team : teamsByName.values()) {
            sb.append("§e").append(i++).append(". §f").append(team.getName())
              .append(" §7(").append(team.getAllPlayers().size()).append(" игроков)\n");
        }
        return sb.toString();
    }

    public boolean isOnSameTeam(Player a, Player b) {
        String keyA = playerTeamMap.get(a.getUniqueId());
        String keyB = playerTeamMap.get(b.getUniqueId());
        return keyA != null && keyA.equals(keyB);
    }

    public String getMsg(String key) {
        return pf() + format(key);
    }

    public String getPf() {
        return pf();
    }

    private String msg(String key, String... replacements) {
        return pf() + format(key, replacements);
    }

    private String format(String key, String... replacements) {
        String template = plugin.getConfig().getString("messages." + key);
        if (template == null) return "§cMissing message: " + key;

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            template = template.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return template.replace("&", "§");
    }

    private String pf() {
        String prefix = plugin.getConfig().getString("prefix");
        return prefix != null ? prefix.replace("&", "§") : "§6[Team] §r";
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!dataFile.exists()) return;

        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, TeamData>>(){}.getType();
            Map<String, TeamData> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                teamsByName.putAll(loaded);
                for (TeamData team : teamsByName.values()) {
                    for (UUID uid : team.getAllPlayers()) {
                        playerTeamMap.put(uid, team.getName().toLowerCase());
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось загрузить teams.json: " + e.getMessage());
        }
    }

    private void save() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(teamsByName, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить teams.json: " + e.getMessage());
        }
    }
}
