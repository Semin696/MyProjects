package org.nig.smp.duels.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.nig.smp.duels.DuelsPlugin;
import org.nig.smp.duels.cmi.CMIKitBridge;
import org.nig.smp.duels.model.Arena;
import org.nig.smp.duels.model.DuelMatch;
import org.nig.smp.duels.model.SavedState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DuelManager {

    private static final PotionEffectType STUN_EFFECT = PotionEffectType.SLOWNESS;
    private static final PotionEffectType BLIND_EFFECT = PotionEffectType.BLINDNESS;

    private final DuelsPlugin plugin;
    private final ArenaManager arenaManager;
    private final VisibilityManager visibilityManager;
    private final Map<UUID, DuelMatch> matches = new HashMap<>();
    private final Map<String, List<UUID>> queue = new HashMap<>();
    private final Set<UUID> waiting = new HashSet<>();
    private final Map<UUID, String> waitingKit = new HashMap<>();

    public DuelManager(DuelsPlugin plugin, ArenaManager arenaManager, VisibilityManager visibilityManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.visibilityManager = visibilityManager;
    }

    public boolean isBusy(UUID playerId) {
        return matches.containsKey(playerId) || waiting.contains(playerId);
    }

    public boolean isWaiting(UUID playerId) {
        return waiting.contains(playerId);
    }

    public Set<UUID> getWaitingPlayers() {
        return waiting;
    }

    public boolean isInActiveDuel(UUID playerId) {
        DuelMatch match = matches.get(playerId);
        return match != null && match.getPhase() == DuelMatch.Phase.ACTIVE;
    }

    public boolean sameActiveDuel(Player a, Player b) {
        DuelMatch ma = matches.get(a.getUniqueId());
        DuelMatch mb = matches.get(b.getUniqueId());
        return ma != null && ma == mb && ma.getPhase() == DuelMatch.Phase.ACTIVE;
    }

    public void createDirectChallenge(Player challenger, Player target) {
        if (challenger.getUniqueId().equals(target.getUniqueId())) {
            challenger.sendMessage(plugin.msg("cannot-duel-self"));
            return;
        }
        if (isBusy(challenger.getUniqueId())) {
            challenger.sendMessage(plugin.msg("already-in-duel"));
            return;
        }
        if (isBusy(target.getUniqueId())) {
            challenger.sendMessage(plugin.msg("target-in-duel", "player", target.getName()));
            return;
        }

        DuelMatch match = new DuelMatch(challenger.getUniqueId(), target.getUniqueId());
        match.setPhase(DuelMatch.Phase.KIT_SELECTION);
        matches.put(challenger.getUniqueId(), match);
        matches.put(target.getUniqueId(), match);

        challenger.sendMessage(plugin.msg("challenge-sent", "player", target.getName()));
        target.sendMessage(plugin.msg("challenge-received", "player", challenger.getName()));

        new org.nig.smp.duels.menu.KitSelectionMenu(plugin, challenger, target).open();
        new org.nig.smp.duels.menu.KitSelectionMenu(plugin, target, challenger).open();
    }

    public void selectKitForChallenge(Player player, String kit, Player opponent) {
        DuelMatch match = matches.get(player.getUniqueId());
        if (match == null || match.getPhase() != DuelMatch.Phase.KIT_SELECTION) {
            return;
        }
        match.setKit(player.getUniqueId(), kit);
        player.sendMessage(plugin.msg("kit-selected", "kit", kit));
        applyWaiting(player);

        if (match.kitsReady()) {
            match.setPhase(DuelMatch.Phase.MAP_SELECTION);
            for (UUID id : match.players()) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    new org.nig.smp.duels.menu.MapSelectionMenu(plugin, p, match).open();
                }
            }
        } else if (opponent != null && opponent.isOnline()) {
            player.showTitle(Title.title(
                plugin.msg("waiting-title"),
                plugin.msg("waiting-subtitle", "player", opponent.getName())
            ));
        }
    }

    public void selectKitForMatchmaking(Player player, String kit) {
        if (isBusy(player.getUniqueId())) {
            player.sendMessage(plugin.msg("already-in-duel"));
            return;
        }
        waitingKit.put(player.getUniqueId(), kit);
        new org.nig.smp.duels.menu.MapSelectionMenu(plugin, player, kit).open();
    }

    public void selectMapForChallenge(Player player, String arenaName) {
        DuelMatch match = matches.get(player.getUniqueId());
        if (match == null || match.getPhase() != DuelMatch.Phase.MAP_SELECTION) {
            return;
        }
        if (arenaManager.getArena(arenaName) == null) {
            player.sendMessage(plugin.msg("arena-not-found"));
            return;
        }
        match.setArena(arenaName);
        startDuel(match);
    }

    public void selectMapForMatchmaking(Player player, String arenaName) {
        if (arenaManager.getArena(arenaName) == null) {
            player.sendMessage(plugin.msg("arena-not-found"));
            return;
        }
        String kit = waitingKit.remove(player.getUniqueId());
        if (kit == null) {
            return;
        }
        queue(player, kit, arenaName);
    }

    private void queue(Player player, String kit, String arenaName) {
        if (isBusy(player.getUniqueId())) {
            player.sendMessage(plugin.msg("already-in-duel"));
            return;
        }
        String key = kit + "||" + arenaName;
        List<UUID> list = queue.computeIfAbsent(key, k -> new ArrayList<>());
        list.add(player.getUniqueId());
        player.sendMessage(plugin.msg("queued", "kit", kit, "arena", arenaName));
        applyWaiting(player);

        if (list.size() >= 2) {
            queue.remove(key);
            UUID a = list.get(0);
            UUID b = list.get(1);
            Player pa = Bukkit.getPlayer(a);
            Player pb = Bukkit.getPlayer(b);
            if (pa == null || pb == null) {
                for (Player p : new Player[]{pa, pb}) {
                    if (p != null) {
                        clearWaiting(p);
                        p.sendMessage(plugin.msg("match-failed"));
                    }
                }
                return;
            }
            DuelMatch match = new DuelMatch(a, b);
            match.setKit(a, kit);
            match.setKit(b, kit);
            match.setArena(arenaName);
            matches.put(a, match);
            matches.put(b, match);
            startDuel(match);
        }
    }

    public void startDuel(DuelMatch match) {
        if (match.getPhase() == DuelMatch.Phase.ACTIVE || match.getPhase() == DuelMatch.Phase.ENDED) {
            return;
        }
        match.setPhase(DuelMatch.Phase.ACTIVE);

        Arena arena = arenaManager.getArena(match.getArena());
        if (arena == null) {
            cancelMatch(match, plugin.msg("no-arenas"));
            return;
        }

        Player p1 = Bukkit.getPlayer(match.getP1());
        Player p2 = Bukkit.getPlayer(match.getP2());
        if (p1 == null || p2 == null) {
            cancelMatch(match, plugin.msg("opponent-quit"));
            return;
        }

        for (Player p : new Player[]{p1, p2}) {
            p.closeInventory();
            clearWaiting(p);
            match.saveState(p.getUniqueId(), new SavedState(p));
            p.getInventory().clear();
            p.getActivePotionEffects().forEach(effect -> p.removePotionEffect(effect.getType()));
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20);
            p.setSaturation(10f);
            p.setFallDistance(0);
            p.setFireTicks(0);
            p.setGameMode(GameMode.SURVIVAL);

            String kit = match.getKit(p.getUniqueId());
            if (kit != null && !CMIKitBridge.applyKit(p, kit)) {
                p.sendMessage(plugin.msg("kit-not-found", "kit", kit));
            }
        }

        p1.teleport(arena.getSpawn1());
        p2.teleport(arena.getSpawn2());

        p1.sendMessage(plugin.msg("duel-start", "arena", arena.getName()));
        p2.sendMessage(plugin.msg("duel-start", "arena", arena.getName()));
        visibilityManager.refresh();
    }

    public void onPlayerDeath(Player victim) {
        DuelMatch match = matches.get(victim.getUniqueId());
        if (match == null || match.getPhase() != DuelMatch.Phase.ACTIVE) {
            return;
        }
        UUID winner = match.getP1().equals(victim.getUniqueId()) ? match.getP2() : match.getP1();
        endDuel(match, winner);
    }

    public void onPlayerQuit(Player player) {
        UUID id = player.getUniqueId();
        waiting.remove(id);
        waitingKit.remove(id);
        queue.values().forEach(list -> list.remove(id));

        DuelMatch match = matches.get(id);
        if (match == null) {
            return;
        }
        if (match.getPhase() == DuelMatch.Phase.ACTIVE) {
            UUID winner = match.getP1().equals(id) ? match.getP2() : match.getP1();
            endDuel(match, winner);
        } else {
            cancelMatch(match, plugin.msg("cancelled"));
            Player other = Bukkit.getPlayer(match.getP1().equals(id) ? match.getP2() : match.getP1());
            if (other != null) {
                other.sendMessage(plugin.msg("challenge-cancelled", "player", player.getName()));
            }
        }
    }

    public void cancel(Player player) {
        DuelMatch match = matches.get(player.getUniqueId());
        if (match == null || match.getPhase() == DuelMatch.Phase.ENDED) {
            player.sendMessage(plugin.msg("not-in-duel"));
            return;
        }
        if (match.getPhase() == DuelMatch.Phase.ACTIVE) {
            player.sendMessage(plugin.msg("cannot-cancel-active"));
            return;
        }
        Player other = Bukkit.getPlayer(match.getP1().equals(player.getUniqueId()) ? match.getP2() : match.getP1());
        cancelMatch(match, plugin.msg("cancelled"));
        if (other != null) {
            other.sendMessage(plugin.msg("challenge-cancelled", "player", player.getName()));
        }
    }

    private void endDuel(DuelMatch match, UUID winnerId) {
        if (match.getPhase() == DuelMatch.Phase.ENDED) {
            return;
        }
        match.setPhase(DuelMatch.Phase.ENDED);
        matches.remove(match.getP1());
        matches.remove(match.getP2());

        Player winner = Bukkit.getPlayer(winnerId);
        for (UUID id : match.players()) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) {
                continue;
            }
            p.closeInventory();
            clearWaiting(p);
            p.getActivePotionEffects().forEach(effect -> p.removePotionEffect(effect.getType()));
            p.setFireTicks(0);
            SavedState state = match.getState(id);
            if (state != null) {
                state.apply(p);
            }
            if (id.equals(winnerId)) {
                p.sendMessage(plugin.msg("you-win"));
            } else {
                p.sendMessage(plugin.msg("you-lose"));
            }
        }
        if (winner != null) {
            winner.sendMessage(plugin.msg("duel-end", "winner", winner.getName()));
        }
        visibilityManager.refresh();
    }

    private void cancelMatch(DuelMatch match, Component reason) {
        if (match.getPhase() == DuelMatch.Phase.ENDED) {
            return;
        }
        match.setPhase(DuelMatch.Phase.ENDED);
        matches.remove(match.getP1());
        matches.remove(match.getP2());
        for (UUID id : match.players()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.closeInventory();
                clearWaiting(p);
                p.sendMessage(reason);
            }
        }
        visibilityManager.refresh();
    }

    public void applyWaiting(Player player) {
        waiting.add(player.getUniqueId());
        player.addPotionEffect(new PotionEffect(STUN_EFFECT, Integer.MAX_VALUE, 6, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 250, false, false));
        player.addPotionEffect(new PotionEffect(BLIND_EFFECT, Integer.MAX_VALUE, 0, false, false));
    }

    public void clearWaiting(Player player) {
        waiting.remove(player.getUniqueId());
        player.removePotionEffect(STUN_EFFECT);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.removePotionEffect(BLIND_EFFECT);
    }

    public int countPlayersByKit(String kit) {
        Set<DuelMatch> seen = new HashSet<>();
        int count = 0;
        for (DuelMatch match : matches.values()) {
            if (seen.add(match) && match.getPhase() == DuelMatch.Phase.ACTIVE) {
                for (UUID id : match.players()) {
                    if (kit.equals(match.getKit(id))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public void shutdown() {
        List<DuelMatch> active = new ArrayList<>();
        for (DuelMatch match : matches.values()) {
            if (!active.contains(match) && match.getPhase() == DuelMatch.Phase.ACTIVE) {
                active.add(match);
            }
        }
        for (DuelMatch match : active) {
            cancelMatch(match, plugin.msg("cancelled"));
        }
        waiting.clear();
        waitingKit.clear();
        queue.clear();
    }
}
