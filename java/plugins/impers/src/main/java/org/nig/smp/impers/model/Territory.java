package org.nig.smp.impers.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Territory {

    private final String name;
    private final String tag;
    private final String world;
    private final int minChunkX;
    private final int minChunkZ;
    private final int maxChunkX;
    private final int maxChunkZ;
    private final UUID owner;
    private final Set<UUID> members;

    public Territory(String name, String tag, String world, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ, UUID owner) {
        this.name = name;
        this.tag = tag;
        this.world = world;
        this.minChunkX = minChunkX;
        this.minChunkZ = minChunkZ;
        this.maxChunkX = maxChunkX;
        this.maxChunkZ = maxChunkZ;
        this.owner = owner;
        this.members = new HashSet<>();
        if (owner != null) {
            this.members.add(owner);
        }
    }

    public String getName() {
        return name;
    }

    public String getTag() {
        return tag;
    }

    public String getWorld() {
        return world;
    }

    public int getMinChunkX() {
        return minChunkX;
    }

    public int getMinChunkZ() {
        return minChunkZ;
    }

    public int getMaxChunkX() {
        return maxChunkX;
    }

    public int getMaxChunkZ() {
        return maxChunkZ;
    }

    public UUID getOwner() {
        return owner;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public boolean isOwner(UUID uuid) {
        return owner != null && owner.equals(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public boolean containsChunk(String worldName, int chunkX, int chunkZ) {
        return world.equals(worldName)
            && chunkX >= minChunkX && chunkX <= maxChunkX
            && chunkZ >= minChunkZ && chunkZ <= maxChunkZ;
    }
}
