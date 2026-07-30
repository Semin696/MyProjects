package org.nig.smp.team;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class Team extends JavaPlugin {

    private TeamManager teamManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        this.teamManager = new TeamManager(this);

        TeamCommand executor = new TeamCommand(teamManager);
        Objects.requireNonNull(getCommand("team")).setExecutor(executor);
        Objects.requireNonNull(getCommand("team")).setTabCompleter(executor);

        getServer().getPluginManager().registerEvents(new FriendlyFireListener(teamManager), this);

        getLogger().info("Team plugin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Team plugin disabled.");
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }
}
