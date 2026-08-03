package org.nig.smp.duels.manager;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.nig.smp.duels.DuelsPlugin;
import org.nig.smp.duels.cmi.CMIKitBridge;
import org.nig.smp.duels.menu.KitSelectionMenu;
import org.nig.smp.duels.menu.MapSelectionMenu;
import org.nig.smp.duels.model.Arena;
import org.nig.smp.duels.model.DuelMatch;
import org.nig.smp.duels.model.SavedState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private final Map<UUID, SavedState> returnStates = new HashMap<>();
    private final Random random = new Random();

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

    // ===== Duels world entry / exit =====

    public void enterDuels(Player player) {
        if (plugin.getDuelsWorld() == null) {
            player.sendMessage(plugin.msg("no-arenas"));
            return;
        }
        if (!plugin.isDuelsWorld(player.getWorld())) {
            if (!returnStates.containsKey(player.getUniqueId())) {
                returnStates.put(player.getUniqueId(), new SavedState(player));
            }
            player.teleport(plugin.getDuelsWorld().getSpawnLocation());
        }
        player.sendMessage(plugin.msg("entered-duels"));
    }

    public void leaveDuels(Player player) {
        if (isInActiveDuel(player.getUniqueId())) {
            player.sendMessage(plugin.msg("cannot-cancel-active"));
            return;
        }
        DuelMatch pending = matches.get(player.getUniqueId());
        if (pending != null && pending.getPhase() != DuelMatch.Phase.ENDED) {
            Player other = Bukkit.getPlayer(pending.getP1().equals(player.getUniqueId())
                ? pending.getP2() : pending.getP1());
            cancelMatch(pending, plugin.msg("cancelled"));
            if (other != null) {
                other.sendMessage(plugin.msg("challenge-cancelled", "player", player.getName()));
            }
        }
        removeFromQueue(player);
        clearWaiting(player);
        SavedState state = returnStates.remove(player.getUniqueId());
        if (state != null) {
            state.apply(player);
        } else if (plugin.isDuelsWorld(player.getWorld())) {
            player.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
        }
        player.sendMessage(plugin.msg("left-duels"));
    }

    private void removeFromQueue(Player player) {
        UUID id = player.getUniqueId();
        waiting.remove(id);
        queue.values().forEach(list -> list.remove(id));
    }

    // ===== Direct challenges =====

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
        if (CMIKitBridge.getKitNames().isEmpty()) {
            challenger.sendMessage(plugin.msg("no-kits"));
            return;
        }
        if (arenaManager.getArenas().isEmpty()) {
            challenger.sendMessage(plugin.msg("no-arenas"));
            return;
        }

        DuelMatch match = new DuelMatch(challenger.getUniqueId(), target.getUniqueId());
        match.setPhase(DuelMatch.Phase.KIT_SELECTION);
        matches.put(challenger.getUniqueId(), match);
        matches.put(target.getUniqueId(), match);

        challenger.sendMessage(plugin.msg("challenge-sent", "player", target.getName()));
        target.sendMessage(plugin.msg("challenge-received", "player", challenger.getName()));

        enterDuels(challenger);
        enterDuels(target);

        new KitSelectionMenu(plugin, challenger, target).open();
        new KitSelectionMenu(plugin, target, challenger).open();
    }

    public void selectKitForChallenge(Player player, String kit, Player opponent) {
        DuelMatch match = matches.get(player.getUniqueId());
        if (match == null || match.getPhase() != DuelMatch.Phase.KIT_SELECTION) {
            return;
        }
        match.setKit(player.getUniqueId(), kit);
        player.sendMessage(plugin.msg("kit-selected", "kit", kit));

        if (match.kitsReady()) {
            match.setPhase(DuelMatch.Phase.MAP_SELECTION);
            for (UUID id : match.players()) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    new MapSelectionMenu(plugin, p, match).open();
                }
            }
        } else if (opponent != null && opponent.isOnline()) {
            player.sendTitle(
                plugin.raw("waiting-title"),
                plugin.raw("waiting-subtitle", "player", opponent.getName()),
                10, 60, 20
            );
        }
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
        if (isArenaInUse(arenaName)) {
            player.sendMessage(plugin.msg("arena-in-use", "arena", arenaName));
            return;
        }
        match.setArena(arenaName);
        startDuel(match);
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

    // ===== Matchmaking queue =====

    public void selectKitForMatchmaking(Player player, String kit) {
        if (isBusy(player.getUniqueId())) {
            player.sendMessage(plugin.msg("already-in-duel"));
            return;
        }
        queue(player, kit);
    }

    private void queue(Player player, String kit) {
        if (arenaManager.getArenas().isEmpty()) {
            player.sendMessage(plugin.msg("no-arenas"));
            return;
        }
        if (!CMIKitBridge.getKitNames().contains(kit)) {
            player.sendMessage(plugin.msg("kit-not-found", "kit", kit));
            return;
        }
        List<UUID> list = queue.computeIfAbsent(kit, k -> new ArrayList<>());
        if (list.contains(player.getUniqueId()) || waiting.contains(player.getUniqueId())) {
            player.sendMessage(plugin.msg("already-waiting"));
            return;
        }
        list.add(player.getUniqueId());
        waiting.add(player.getUniqueId());

        player.sendMessage(plugin.msg("queued", "kit", kit));
        applyWaiting(player);
        player.sendTitle(
            plugin.raw("waiting-title"),
            "Кит: " + kit,
            10, 60, 20
        );

        tryMatchQueue(kit);
    }

    private void tryMatchQueue(String kit) {
        List<UUID> list = queue.get(kit);
        if (list == null || list.size() < 2) {
            return;
        }
        List<UUID> online = new ArrayList<>();
        for (UUID id : list) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && matches.get(id) == null) {
                online.add(id);
            } else {
                waiting.remove(id);
            }
        }
        if (online.size() < 2) {
            queue.put(kit, online);
            return;
        }
        Arena arena = getRandomFreeArena();
        if (arena == null) {
            queue.put(kit, online);
            return;
        }
        queue.remove(kit);

        UUID a = online.get(0);
        UUID b = online.get(1);
        waiting.remove(a);
        waiting.remove(b);

        Player pa = Bukkit.getPlayer(a);
        Player pb = Bukkit.getPlayer(b);
        if (pa == null || pb == null) {
            return;
        }

        DuelMatch match = new DuelMatch(a, b);
        match.setKit(a, kit);
        match.setKit(b, kit);
        match.setArena(arena.getName());
        matches.put(a, match);
        matches.put(b, match);
        startDuel(match);
    }

    private void tryMatchQueues() {
        for (String kit : new ArrayList<>(queue.keySet())) {
            tryMatchQueue(kit);
        }
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
        for (List<UUID> list : queue.values()) {
            for (UUID id : list) {
                count++;
            }
        }
        return count;
    }

    // ===== Duel lifecycle =====

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

            SavedState state = returnStates.remove(p.getUniqueId());
            if (state == null) {
                state = new SavedState(p);
            }
            match.saveState(p.getUniqueId(), state);

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
        queue.values().forEach(list -> list.remove(id));
        returnStates.remove(id);

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
        tryMatchQueues();
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

    // ===== Waiting (stun + blindness) =====

    public void applyWaiting(Player player) {
        waiting.add(player.getUniqueId());
        player.addPotionEffect(new PotionEffect(STUN_EFFECT, Integer.MAX_VALUE, 6, false, false));
        player.addPotionEffect(new PotionEffect(BLIND_EFFECT, Integer.MAX_VALUE, 0, false, false));
    }

    public void clearWaiting(Player player) {
        waiting.remove(player.getUniqueId());
        player.removePotionEffect(STUN_EFFECT);
        player.removePotionEffect(BLIND_EFFECT);
    }

    // ===== Arenas =====

    private boolean isArenaInUse(String name) {
        for (DuelMatch match : matches.values()) {
            if (match.getPhase() == DuelMatch.Phase.ACTIVE
                && match.getArena() != null
                && match.getArena().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private Arena getRandomFreeArena() {
        List<Arena> free = new ArrayList<>();
        for (Arena arena : arenaManager.getArenas()) {
            if (!isArenaInUse(arena.getName())) {
                free.add(arena);
            }
        }
        if (free.isEmpty()) {
            return null;
        }
        return free.get(random.nextInt(free.size()));
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
        queue.clear();
        returnStates.clear();
    }
}
