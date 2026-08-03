package org.nig.smp.duels.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DuelMatch {

    public enum Phase {
        KIT_SELECTION, MAP_SELECTION, ACTIVE, ENDED
    }

    private final UUID p1;
    private final UUID p2;
    private final Map<UUID, String> kits = new HashMap<>();
    private final Map<UUID, SavedState> states = new HashMap<>();
    private Phase phase = Phase.KIT_SELECTION;
    private String arena;

    public DuelMatch(UUID p1, UUID p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    public UUID getP1() {
        return p1;
    }

    public UUID getP2() {
        return p2;
    }

    public List<UUID> players() {
        return List.of(p1, p2);
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public void setKit(UUID player, String kit) {
        kits.put(player, kit);
    }

    public String getKit(UUID player) {
        return kits.get(player);
    }

    public boolean kitsReady() {
        return kits.containsKey(p1) && kits.containsKey(p2);
    }

    public String getArena() {
        return arena;
    }

    public void setArena(String arena) {
        this.arena = arena;
    }

    public void saveState(UUID player, SavedState state) {
        states.put(player, state);
    }

    public SavedState getState(UUID player) {
        return states.get(player);
    }
}
