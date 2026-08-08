package org.nig.smp.duels.model;

import org.bukkit.Location;

public final class Arena {

    private final String name;
    private final String displayName;
    private final boolean allowBlockBreak;
    private final Location spawn1;
    private final Location spawn2;

    public Arena(String name, String displayName, boolean allowBlockBreak, Location spawn1, Location spawn2) {
        this.name = name;
        this.displayName = displayName;
        this.allowBlockBreak = allowBlockBreak;
        this.spawn1 = spawn1;
        this.spawn2 = spawn2;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isBlockBreakAllowed() {
        return allowBlockBreak;
    }

    public Location getSpawn1() {
        return spawn1;
    }

    public Location getSpawn2() {
        return spawn2;
    }
}
