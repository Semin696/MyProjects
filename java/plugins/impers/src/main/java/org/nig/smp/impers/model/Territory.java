package org.nig.smp.impers.model;

public class Territory {

    private final String name;
    private final String tag;
    private final String world;
    private final int minChunkX;
    private final int minChunkZ;
    private final int maxChunkX;
    private final int maxChunkZ;

    public Territory(String name, String tag, String world, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        this.name = name;
        this.tag = tag;
        this.world = world;
        this.minChunkX = minChunkX;
        this.minChunkZ = minChunkZ;
        this.maxChunkX = maxChunkX;
        this.maxChunkZ = maxChunkZ;
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

    public boolean containsChunk(String worldName, int chunkX, int chunkZ) {
        return world.equals(worldName)
            && chunkX >= minChunkX && chunkX <= maxChunkX
            && chunkZ >= minChunkZ && chunkZ <= maxChunkZ;
    }
}
